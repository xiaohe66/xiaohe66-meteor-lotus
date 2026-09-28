package com.xiaohe66.mc.meteor.lotus.modules.printer;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.Comparator;

/**
 * @author kkllffaa
 * @author cat
 */
public enum SortAlgorithm {
    None(false, (a, b) -> 0),
    TopDown(true, Comparator.comparingInt(value -> value.getY() * -1)),
    DownTop(true, Comparator.comparingInt(Vec3i::getY)),
    最近的(false, Comparator.comparingDouble(value -> MeteorClient.mc.player != null ? Utils.squaredDistance(MeteorClient.mc.player.getX(), MeteorClient.mc.player.getY(), MeteorClient.mc.player.getZ(), value.getX() + 0.5, value.getY() + 0.5, value.getZ() + 0.5) : 0)),
    最远的(false, Comparator.comparingDouble(value -> MeteorClient.mc.player != null ? (Utils.squaredDistance(MeteorClient.mc.player.getX(), MeteorClient.mc.player.getY(), MeteorClient.mc.player.getZ(), value.getX() + 0.5, value.getY() + 0.5, value.getZ() + 0.5)) * -1 : 0));


    public final boolean applySecondSorting;
    public final Comparator<BlockPos> algorithm;

    SortAlgorithm(boolean applySecondSorting, Comparator<BlockPos> algorithm) {
        this.applySecondSorting = applySecondSorting;
        this.algorithm = algorithm;
    }
}
