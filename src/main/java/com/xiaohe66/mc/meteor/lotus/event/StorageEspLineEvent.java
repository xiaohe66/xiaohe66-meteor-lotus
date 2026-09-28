package com.xiaohe66.mc.meteor.lotus.event;

import net.minecraft.block.entity.BlockEntity;

/**
 * @author He
 * @since 2025.05.29 13:01
 */
public class StorageEspLineEvent extends Event {

    private BlockEntity blockEntity;

    public StorageEspLineEvent(BlockEntity blockEntity) {
        super(Stage.Pre);
        this.blockEntity = blockEntity;
    }

    public BlockEntity getBlockEntity() {
        return blockEntity;
    }
}
