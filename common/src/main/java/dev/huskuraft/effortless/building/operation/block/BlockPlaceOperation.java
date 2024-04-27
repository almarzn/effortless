package dev.huskuraft.effortless.building.operation.block;

import java.util.Collections;

import dev.huskuraft.effortless.api.core.BlockInteraction;
import dev.huskuraft.effortless.api.core.BlockState;
import dev.huskuraft.effortless.api.core.InteractionHand;
import dev.huskuraft.effortless.api.core.ItemStack;
import dev.huskuraft.effortless.api.core.Player;
import dev.huskuraft.effortless.api.core.World;
import dev.huskuraft.effortless.api.sound.SoundInstance;
import dev.huskuraft.effortless.building.Context;
import dev.huskuraft.effortless.building.Storage;
import dev.huskuraft.effortless.building.pattern.MirrorContext;
import dev.huskuraft.effortless.building.pattern.MoveContext;
import dev.huskuraft.effortless.building.pattern.RefactorContext;
import dev.huskuraft.effortless.building.pattern.RevolveContext;
import dev.huskuraft.effortless.building.replace.ReplaceMode;

public class BlockPlaceOperation extends BlockOperation {

    public BlockPlaceOperation(
            World world,
            Player player,
            Context context,
            Storage storage,
            BlockInteraction interaction,
            BlockState blockState
    ) {
        super(world, player, context, storage, interaction, blockState);
    }

    private BlockOperationResult.Type placeBlock() {

        if (blockState == null) {
            return BlockOperationResult.Type.FAIL_BLOCK_STATE_NULL;
        }

        // spectator
        if (player.getGameType().isSpectator()) {
            return BlockOperationResult.Type.FAIL_PLAYER_IS_SPECTATOR;
        }

        // whitelist/blacklist
        if (!context.customParams().generalConfig().whitelistedItems().isEmpty() && !context.customParams().generalConfig().whitelistedItems().contains(blockState.getItem().getId())) {
            return BlockOperationResult.Type.FAIL_WHITELISTED;
        }

        if (!context.customParams().generalConfig().blacklistedItems().isEmpty() && context.customParams().generalConfig().blacklistedItems().contains(blockState.getItem().getId())) {
            return BlockOperationResult.Type.FAIL_BLACKLISTED;
        }

        // world permission
        if (!isInBorderBound()) {
            return BlockOperationResult.Type.FAIL_WORLD_BORDER;
        }

        if (!isInHeightBound()) {
            return BlockOperationResult.Type.FAIL_WORLD_HEIGHT;
        }

        // action permission
        var itemStack = storage.search(blockState.getItem()).orElse(null);

        if (itemStack == null || itemStack.isEmpty()) {
            return BlockOperationResult.Type.FAIL_ITEM_INSUFFICIENT;
        }

        if (itemStack.isAir()) {
            return BlockOperationResult.Type.FAIL_BLOCK_STATE_AIR;
        }

        if (!itemStack.getItem().isBlockItem()) {
            return BlockOperationResult.Type.FAIL_ITEM_NOT_BLOCK;
        }

        // action permission
        if (!player.canInteractBlock(getInteraction().getBlockPosition())) {
            return BlockOperationResult.Type.FAIL_PLAYER_CANNOT_INTERACT;
        }

        switch (context.replaceMode()) {
            case DISABLED -> {
                if (!player.getWorld().getBlockState(getInteraction().getBlockPosition()).isReplaceable(player, getInteraction())) {
                    return BlockOperationResult.Type.FAIL_PLAYER_CANNOT_BREAK;
                }
            }
            case NORMAL, QUICK -> {
                if (!player.getGameType().isCreative() && !player.getWorld().getBlockState(getInteraction().getBlockPosition()).isDestroyable()) {
                    return BlockOperationResult.Type.FAIL_PLAYER_CANNOT_BREAK;
                }
                if (!player.canAttackBlock(getInteraction().getBlockPosition())) {
                    return BlockOperationResult.Type.FAIL_PLAYER_CANNOT_BREAK;
                }
            }
        }

        if (context.isPreview() && player.getWorld().isClient()) {
            itemStack.decrease(1);
            return BlockOperationResult.Type.CONSUME;
        }

        if (context.replaceMode() == ReplaceMode.QUICK && !player.destroyBlock(getInteraction())) {
            return BlockOperationResult.Type.FAIL_UNKNOWN;
        }

//        if (context.type() == BuildType.COMMAND) {
//            CommandManager.dispatch(new SetBlockCommand(getBlockState(), getInteraction().getBlockPosition(), SetBlockCommand.Mode.REPLACE));
//            return BlockOperationResult.Type.SUCCESS;
//        }

        // compatible layer
        var originalItemStack = player.getItemStack(InteractionHand.MAIN);
        player.setItemStack(InteractionHand.MAIN, itemStack);
        var placed = player.useItem(interaction);
        player.setItemStack(InteractionHand.MAIN, originalItemStack);

        if (!placed) {
            return BlockOperationResult.Type.FAIL_UNKNOWN;
        }

        if (!world.getBlockState(getInteraction().getBlockPosition()).equals(blockState) && !world.setBlockState(getInteraction().getBlockPosition(), blockState)) {
            return BlockOperationResult.Type.FAIL_UNKNOWN;
        }

        return BlockOperationResult.Type.SUCCESS;
    }

    @Override
    public BlockPlaceOperationResult commit() {
        var inputs = blockState != null ? Collections.singletonList(blockState.getItem().getDefaultStack()) : Collections.<ItemStack>emptyList();
        var outputs = Collections.<ItemStack>emptyList();
        var result = placeBlock();

        if (getWorld().isClient() && getContext().isPreviewOnce() && result.success()) {
            var sound = SoundInstance.createBlock(getBlockState().getSoundSet().placeSound(), (getBlockState().getSoundSet().volume() + 1.0F) / 2.0F, getBlockState().getSoundSet().pitch() * 0.8F, getInteraction().getBlockPosition().getCenter());
            getPlayer().getClient().getSoundManager().play(sound);
        }

        return new BlockPlaceOperationResult(this, result, inputs, outputs);
    }

    @Override
    public BlockPlaceOperation move(MoveContext moveContext) {
        return new BlockPlaceOperation(world, player, context, storage, moveContext.move(interaction), blockState);
    }

    @Override
    public BlockPlaceOperation mirror(MirrorContext mirrorContext) {
        return new BlockPlaceOperation(world, player, context, storage, mirrorContext.mirror(interaction), mirrorContext.mirror(blockState));
    }

    @Override
    public BlockPlaceOperation revolve(RevolveContext revolveContext) {
        return null;
    }

    @Override
    public BlockPlaceOperation refactor(RefactorContext refactorContext) {
        return new BlockPlaceOperation(world, player, context, storage, interaction, refactorContext.refactor(player, getInteraction()));
    }

    private ItemStack getItemStack() {
        return storage.search(blockState.getItem()).orElse(null);
    }

}
