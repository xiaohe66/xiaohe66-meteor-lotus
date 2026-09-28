package com.xiaohe66.mc.meteor.lotus.util;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author He
 * @since 2025.03.15 13:59
 */
public class PosUtils {

    private static final Logger log = LoggerFactory.getLogger(PosUtils.class);

    public static void printPlayerDiff(BlockPos blockPos, ClientPlayerEntity player) {

        double xDiff = blockPos.getX() - player.getX();
        double yDiff = blockPos.getY() - player.getY();
        double zDiff = blockPos.getZ() - player.getZ();

        log.info("[{},{},{}] - [{},{},{}] = [{},{},{}]", blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                String.format("%.2f", player.getX()), String.format("%.2f", player.getY()), String.format("%.2f", player.getZ()),
                String.format("%.2f", xDiff), String.format("%.2f", yDiff), String.format("%.2f", zDiff));

    }
}
