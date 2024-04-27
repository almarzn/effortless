package dev.huskuraft.effortless.vanilla.core;

import dev.huskuraft.effortless.api.core.Block;
import dev.huskuraft.effortless.api.core.BlockInteraction;
import dev.huskuraft.effortless.api.core.InteractionResult;
import dev.huskuraft.effortless.api.core.Item;
import dev.huskuraft.effortless.api.core.ItemStack;
import dev.huskuraft.effortless.api.core.Player;
import dev.huskuraft.effortless.api.core.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.context.UseOnContext;

public record MinecraftItem(net.minecraft.world.item.Item referenceValue) implements Item {

    public static Item ofNullable(net.minecraft.world.item.Item reference) {
        if (reference == null) return null;
        if (reference instanceof BlockItem blockItem) return new MinecraftBlockItem(blockItem);
        if (reference instanceof BucketItem bucketItem) return new MinecraftBucketItem(bucketItem);
        return new MinecraftItem(reference);
    }

    @Override
    public ItemStack getDefaultStack() {
        return new MinecraftItemStack(referenceValue().getDefaultInstance());
    }

    @Override
    public Block getBlock() {
        return new MinecraftBlock(net.minecraft.world.level.block.Block.byItem(referenceValue()));
    }

    @Override
    public ResourceLocation getId() {
        var minecraftResourceLocation = BuiltInRegistries.ITEM.getKey(referenceValue());
        return new MinecraftResourceLocation(minecraftResourceLocation);
    }

    @Override
    public InteractionResult use(Player player, BlockInteraction blockInteraction) {
        return MinecraftConvertor.toPlatformInteractionResult(referenceValue().useOn(new UseOnContext(player.reference(), MinecraftConvertor.toPlatformInteractionHand(blockInteraction.getHand()), MinecraftConvertor.toPlatformBlockInteraction(blockInteraction))));
    }
}
