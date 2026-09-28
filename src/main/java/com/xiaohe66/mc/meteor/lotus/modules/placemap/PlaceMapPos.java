package com.xiaohe66.mc.meteor.lotus.modules.placemap;

import net.minecraft.util.math.BlockPos;

/**
 * @author He
 * @since 2025.12.03 22:51
 */
public class PlaceMapPos {

    private final BlockPos blockPos;
    private String name;
    private boolean done;

    public PlaceMapPos(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public PlaceMapPos(int x, int y, int z) {
        this.blockPos = new BlockPos(x, y, z);
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}
