package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.event.KeyboardInputTickEvent;
import com.xiaohe66.mc.meteor.lotus.util.Const;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.BlockState;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author He
 * @since 2025.03.17 10:12
 */
public class SafeWalkPlus extends Module {

    private static final Logger log = LoggerFactory.getLogger(SafeWalkPlus.class);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
            .name("边缘距离")
            .description("站住的地面剩下多少时下蹲")
            .min(0.1)
            .sliderMax(1.0)
            .defaultValue(0.2)
            .build());

    public SafeWalkPlus() {
        super(Const.CATEGORY, "安全行走+", "自动在边缘下蹲");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onKeyboardInputTickEvent(KeyboardInputTickEvent event) {

        PlayerInput playerInput = event.getPlayerInput();
        if (playerInput.sneak() || playerInput.jump()) {
            return;
        }

        boolean needSneak = isOnEdge();

        PlayerInput newPlayerInput = new PlayerInput(
                playerInput.forward(),
                playerInput.backward(),
                playerInput.left(),
                playerInput.right(),
                playerInput.jump(),
                needSneak,
                playerInput.sprint()
        );

        event.setPlayerInput(newPlayerInput);
    }

    /*@EventHandler(priority = EventPriority.LOW)
    public void onMove(MoveEvent event) {
        if (mc.player == null) return;

        if (mc.player.isOnGround()) {
            // 原有安全行走逻辑
            *//*double x = event.getX();
            double y = event.getY();
            double z = event.getZ();

            double increment = 0.05;
            while (x != 0.0 && isOffsetBBEmpty(x, y, 0.0)) {
                if (x < increment && x >= -increment) x = 0.0;
                else x += (x > 0.0) ? -increment : increment;
            }
            while (z != 0.0 && isOffsetBBEmpty(0.0, y, z)) {
                if (z < increment && z >= -increment) z = 0.0;
                else z += (z > 0.0) ? -increment : increment;
            }
            while (x != 0.0 && z != 0.0 && isOffsetBBEmpty(x, y, z)) {
                x = (Math.abs(x) < increment) ? 0.0 : x + (x > 0.0 ? -increment : increment);
                z = (Math.abs(z) < increment) ? 0.0 : z + (z > 0.0 ? -increment : increment);
            }

            if (event.getX() != x || event.getZ() != z) {
                event.setX(x);
                event.setZ(z);
            }*//*

            // 新增自动蹲下逻辑
            this.needSneak = isOnEdge();

        } else {

            this.needSneak = false;
        }
    }*/

    private boolean isOnEdge() {
        Box playerBB = mc.player.getBoundingBox();
        // 检测支撑区域四个角是否悬空
        Double distanceValue = distance.get();

        double minX = playerBB.minX + distanceValue;
        double maxX = playerBB.maxX - distanceValue;
        double minZ = playerBB.minZ + distanceValue;
        double maxZ = playerBB.maxZ - distanceValue;

        if (!isCornerSupported(minX, playerBB.minY, minZ)) {
            return true;
        }
        if (!isCornerSupported(minX, playerBB.minY, maxZ)) {
            return true;
        }
        if (!isCornerSupported(maxX, playerBB.minY, minZ)) {
            return true;
        }

        return !isCornerSupported(maxX, playerBB.minY, maxZ);
    }

    private boolean isCornerSupported(double x, double y, double z) {
        // 检测脚底下方0.001处是否有支撑
        Box supportCheck = new Box(
                x - 0.05, y - 0.001, z - 0.05,
                x + 0.05, y, z + 0.05
        );
        return hasSolidSupport(supportCheck);
    }

    /*private boolean isOffsetBBEmpty(double offsetX, double offsetY, double offsetZ) {
        Box movedBB = mc.player.getBoundingBox().offset(offsetX, offsetY, offsetZ);
        Box supportArea = new Box(
                movedBB.minX, movedBB.minY - 0.001, movedBB.minZ,
                movedBB.maxX, movedBB.minY, movedBB.maxZ
        );
        return !hasSolidSupport(supportArea);
    }*/

    private boolean hasSolidSupport(Box area) {
        for (BlockPos pos : getBlocksInBox(area)) {
            BlockState state = mc.world.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(mc.world, pos);
            if (!shape.isEmpty() && shape.getBoundingBoxes().stream()
                    .anyMatch(box -> box.offset(pos).intersects(area))) {
                return true;
            }
        }
        return false;
    }

    private Iterable<BlockPos> getBlocksInBox(Box box) {
        int minX = MathHelper.floor(box.minX);
        int minY = MathHelper.floor(box.minY);
        int minZ = MathHelper.floor(box.minZ);
        int maxX = MathHelper.floor(box.maxX);
        int maxY = MathHelper.floor(box.maxY);
        int maxZ = MathHelper.floor(box.maxZ);
        return BlockPos.iterate(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
