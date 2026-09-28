package com.xiaohe66.mc.meteor.lotus.bo;

import net.minecraft.util.math.BlockPos;

/**
 * 存储位置信息，用于简易仓库中整理物品
 *
 * @author He
 * @since 2025.08.26 22:10
 */
public class StoragePos {

    private final StorageItem item;
    private final BlockPos framePos;
    private final BlockPos putPos;
    private final BlockPos takePos;
    private final BlockPos kitPos;
    private final BlockPos btnPos;

    public StoragePos(StorageItem item, BlockPos framePos, BlockPos putPos, BlockPos takePos, BlockPos kitPos, BlockPos btnPos) {
        this.item = item;
        this.framePos = framePos;
        this.putPos = putPos;
        this.takePos = takePos;
        this.kitPos = kitPos;
        this.btnPos = btnPos;
    }

    public StorageItem getItem() {
        return item;
    }

    public BlockPos getFramePos() {
        return framePos;
    }

    public BlockPos getPutPos() {
        return putPos;
    }

    public BlockPos getTakePos() {
        return takePos;
    }

    public BlockPos getKitPos() {
        return kitPos;
    }

    public BlockPos getBtnPos() {
        return btnPos;
    }
}
