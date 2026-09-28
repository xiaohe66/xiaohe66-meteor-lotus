package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.util.Const;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * @author He
 * @since 2025.05.16 22:47
 */
public class PortalEsp extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
            .name("radius")
            .description("检测半径")
            .defaultValue(32)
            .min(16)
            .max(128)
            .build());

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("渲染模式")
            .description("方块的渲染方式")
            .defaultValue(ShapeMode.Both)
            .build());

    private final Setting<SettingColor> frameColor = sgGeneral.add(new ColorSetting.Builder()
            .name("框架颜色")
            .description("未激活传送门的颜色")
            .defaultValue(new SettingColor(0, 150, 255, 50))
            .build());

    private final Setting<SettingColor> portalColor = sgGeneral.add(new ColorSetting.Builder()
            .name("传送门颜色")
            .description("已激活传送门的颜色")
            .defaultValue(new SettingColor(255, 0, 255, 50))
            .build());

    private final Setting<Boolean> obsidianFloor = sgGeneral.add(new BoolSetting.Builder()
            .name("忽略遗迹")
            .description("忽略下界传送门遗迹")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> ignoreRelic = sgGeneral.add(new BoolSetting.Builder()
            .name("忽略遗迹")
            .description("忽略下界传送门遗迹")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> ignoreLake = sgGeneral.add(new BoolSetting.Builder()
            .name("忽略岩浆湖")
            .description("忽略岩浆湖")
            .defaultValue(true)
            .build());

    // 缓存检测到的传送门结构
    private final List<PortalFrame> portals = new ArrayList<>();

    public PortalEsp() {
        super(Const.CATEGORY, "!下界门透视", "(计划中的功能, 暂不可用)自动搜索可疑的下界门(激活的、未激活的、破损的), 忽略自然生成的遗迹");
    }

    @Override
    public void onActivate() {
        portals.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (true) {
            return;
        }
        portals.clear();

        // 获取玩家位置并计算检测范围
        BlockPos playerPos = mc.player.getBlockPos();
        int radius = this.radius.get();

        // 遍历周围方块
        for (BlockPos pos : BlockPos.iterateOutwards(playerPos, radius, radius, radius)) {
            BlockState state = mc.world.getBlockState(pos);

            // 检测传送门方块（已激活）
            if (state.getBlock() == Blocks.NETHER_PORTAL) {
                portals.add(new PortalFrame(pos, true));
                continue;
            }

            // 检测潜在框架结构（未激活）
            if (state.getBlock() == Blocks.OBSIDIAN) {
                checkPortalFrame(pos);
            }
        }
    }

    private void checkPortalFrame(BlockPos startPos) {
        // 可能的门框方向（直立/平躺）
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            // 检测直立方向的门框结构
            if (isValidPortalFrame(startPos, axis)) {
                portals.add(new PortalFrame(startPos, false));
                return;
            }
        }
    }

    private boolean isValidPortalFrame(BlockPos center, Direction.Axis axis) {
        // 标准门框尺寸（最小3x3到最大23x23）
        int maxSize = 23;

        // 检查横向延伸
        int width = findFrameLength(center, Direction.EAST, axis, maxSize)
                + findFrameLength(center, Direction.WEST, axis, maxSize) + 1;

        // 检查纵向延伸
        int height = findFrameLength(center, Direction.NORTH, axis, maxSize)
                + findFrameLength(center, Direction.SOUTH, axis, maxSize) + 1;

        // 验证是否符合门框尺寸要求
        return (width >= 4 && height >= 5) || (width >= 5 && height >= 4);
    }

    private int findFrameLength(BlockPos start, Direction direction, Direction.Axis axis, int max) {
        int length = 0;
        BlockPos.Mutable pos = start.mutableCopy();

        while (length < max) {
            pos.move(direction);
            BlockState state = mc.world.getBlockState(pos);

            // 需要保持相同轴向的黑曜石
            /*if (state.getBlock() != Blocks.OBSIDIAN || state.get(PistonBlock.AXIS) != axis) {
                break;
            }*/

            length++;
        }
        return length;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (true) {
            return;
        }
        for (PortalFrame portal : portals) {
            /*Renderer3D.renderBox(
                    event.matrices,
                    portal.pos,
                    portal.isActive ? portalColor.get() : frameColor.get(),
                    shapeMode.get()
            );*/
        }
    }

    // 传送门结构数据类
    private static class PortalFrame {

        public final BlockPos pos;
        public final boolean isActive;

        public PortalFrame(BlockPos pos, boolean isActive) {
            this.pos = pos;
            this.isActive = isActive;
        }
    }
}
