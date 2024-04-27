package dev.huskuraft.effortless.api.core;


public interface BlockItem extends Item {

    Block getBlock();

    InteractionResult place(Player player, BlockInteraction blockInteraction);
}
