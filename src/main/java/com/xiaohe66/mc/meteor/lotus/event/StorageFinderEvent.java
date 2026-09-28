package com.xiaohe66.mc.meteor.lotus.event;

import net.minecraft.block.entity.BlockEntity;

/**
 * @author He
 * @since 2025.07.06 19:20
 */
public class StorageFinderEvent extends Event {

    private static final StorageFinderEvent INSTANCE = new StorageFinderEvent();

    private BlockEntity blockEntity;

    private StorageFinderEvent() {
        super(Stage.Post);
    }

    public static StorageFinderEvent get(BlockEntity blockEntity) {
        INSTANCE.blockEntity = blockEntity;
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }

    public BlockEntity getBlockEntity() {
        return blockEntity;
    }
}
