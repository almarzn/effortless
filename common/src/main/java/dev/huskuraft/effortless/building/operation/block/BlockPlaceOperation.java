package dev.huskuraft.effortless.building.operation.block;

import java.util.Collections;

import dev.huskuraft.effortless.api.core.BlockInteraction;
import dev.huskuraft.effortless.api.core.BlockItem;
import dev.huskuraft.effortless.api.core.BlockState;
import dev.huskuraft.effortless.api.core.ItemStack;
import dev.huskuraft.effortless.api.core.Player;
import dev.huskuraft.effortless.api.core.StatTypes;
import dev.huskuraft.effortless.api.core.World;
import dev.huskuraft.effortless.api.sound.SoundInstance;
import dev.huskuraft.effortless.building.Context;
import dev.huskuraft.effortless.building.Storage;
import dev.huskuraft.effortless.building.pattern.MirrorContext;
import dev.huskuraft.effortless.building.pattern.MoveContext;
import dev.huskuraft.effortless.building.pattern.RefactorContext;
import dev.huskuraft.effortless.building.pattern.RotateContext;

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
        if (player.getGameMode().isSpectator()) {
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

        if (!(itemStack.getItem() instanceof BlockItem blockItem)) {
            return BlockOperationResult.Type.FAIL_ITEM_NOT_BLOCK;
        }

        switch (context.replaceMode()) {
            case DISABLED -> {
                if (!player.getWorld().getBlockState(getBlockPosition()).isReplaceable(player, getInteraction())) {
                    return BlockOperationResult.Type.FAIL_PLAYER_CANNOT_BREAK;
                }
            }
            case NORMAL, QUICK -> {
                if (!player.getGameMode().isCreative() && !player.getWorld().getBlockState(getBlockPosition()).isDestroyable()) {
                    return BlockOperationResult.Type.FAIL_PLAYER_CANNOT_BREAK;
                }
                if (!player.canAttackBlock(getBlockPosition())) {
                    return BlockOperationResult.Type.FAIL_PLAYER_CANNOT_BREAK;
                }
            }
        }

        if (context.isPreview() && player.getWorld().isClient()) {
            itemStack.decrease(1);
            return BlockOperationResult.Type.CONSUME;
        }

        if (context.replaceMode().isReplace()) {
            if (!player.getWorld().getBlockState(getBlockPosition()).isReplaceable(player, getInteraction()) && !player.destroyBlock(getInteraction())) {
                return BlockOperationResult.Type.FAIL_UNKNOWN;
            }
        }

//        if (context.buildType() == BuildType.COMMAND) {
//            CommandManager.dispatch(new SetBlockCommand(getBlockState(), getBlockPosition(), SetBlockCommand.Mode.REPLACE));
//            return BlockOperationResult.Type.SUCCESS;
//        }

        // compatible layer

        if (world.isClient()) {
            return BlockOperationResult.Type.CONSUME;
        }

        var originalItemStack = player.getItemStack(getHand());
        player.setItemStack(getHand(), itemStack);
        var placed = blockItem.placeOnBlock(player, getInteraction()).consumesAction();
        player.setItemStack(getHand(), originalItemStack);

        if (!placed) {
            return BlockOperationResult.Type.FAIL_UNKNOWN;
        }
        if (!world.isClient()) {
            player.awardStat(StatTypes.ITEM_USED.get(itemStack.getItem()));
        }

        // FIXME: 29/4/24
        if (!world.getBlockState(getBlockPosition()).equals(blockState) && !world.setBlockState(getBlockPosition(), blockState)) {
            return BlockOperationResult.Type.FAIL_UNKNOWN;
        }

        return BlockOperationResult.Type.SUCCESS;
    }

    @Override
    public BlockPlaceOperationResult commit() {
        var inputs = blockState != null ? Collections.singletonList(blockState.getItem().getDefaultStack()) : Collections.<ItemStack>emptyList();
        var outputs = Collections.<ItemStack>emptyList();
        var result = placeBlock();

        if (getWorld().isClient() && getContext().isPreviewSound() && result.success()) {
            var sound = SoundInstance.createBlock(getBlockState().getSoundSet().placeSound(), (getBlockState().getSoundSet().volume() + 1.0F) / 2.0F * 0.5F, getBlockState().getSoundSet().pitch() * 0.8F, getBlockPosition().getCenter());
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
    public BlockPlaceOperation rotate(RotateContext rotateContext) {
        return new BlockPlaceOperation(world, player, context, storage, rotateContext.rotate(interaction), rotateContext.rotate(blockState));
    }

    @Override
    public BlockOperation refactor(RefactorContext refactorContext) {
        return new BlockPlaceOperation(world, player, context, storage, interaction, refactorContext.refactor(player, getInteraction()));
    }

}
