package com.xiaohe66.mc.meteor.lotus.util;

import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * @author He
 * @since 2025.05.15 23:20
 */
public class HeRotationUtils {

    public static void rotate(Vec3d vec3d) {
        double yaw = Rotations.getYaw(vec3d);
        double pitch = Rotations.getPitch(vec3d);
        Rotations.rotate(yaw, pitch);
        // rotateSilent((float) yaw, (float) pitch);
    }

    public static void rotate(BlockPos blockPos) {
        double yaw = Rotations.getYaw(blockPos);
        double pitch = Rotations.getPitch(blockPos);
        Rotations.rotate(yaw, pitch);
        // rotateSilent((float) yaw, (float) pitch);
    }

    /**
     * 会多发一个 PlayerMoveC2SPacket.LookAndOnGround 包
     */
    public static void rotate(Vec3d vec3d, Runnable runnable) {
        double yaw = Rotations.getYaw(vec3d);
        double pitch = Rotations.getPitch(vec3d);
        Rotations.rotate(yaw, pitch, 6666, runnable);
    }

    /**
     * 会多发一个 PlayerMoveC2SPacket.LookAndOnGround 包
     */
    public static void rotate(BlockPos blockPos, Runnable runnable) {
        double yaw = Rotations.getYaw(blockPos);
        double pitch = Rotations.getPitch(blockPos);
        Rotations.rotate(yaw, pitch, 6666, runnable);
    }

    public static void rotateSilent(float yaw, float pitch) {
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                mc.player.getX(),
                mc.player.getY(),
                mc.player.getZ(),
                yaw,
                pitch,
                mc.player.isOnGround(),
                false
        ));
    }

}
