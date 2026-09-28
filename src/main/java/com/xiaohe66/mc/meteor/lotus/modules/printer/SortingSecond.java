package com.xiaohe66.mc.meteor.lotus.modules.printer;

import net.minecraft.util.math.BlockPos;

import java.util.Comparator;

/**
 * @author kkllffaa
 * @author cat
 */
public enum SortingSecond {
    None(SortAlgorithm.None.algorithm),
    最近的(SortAlgorithm.最近的.algorithm),
    最远的(SortAlgorithm.最远的.algorithm);

    public final Comparator<BlockPos> algorithm;

    SortingSecond(Comparator<BlockPos> algorithm) {
        this.algorithm = algorithm;
    }
}
