package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.util.Const;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author He
 * @since 2026.04.06 22:48
 */
public class RedstoneAssist extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup lightOverlayGroup = settings.createGroup("亮度显示");

    private final BoolSetting lightOverlay = lightOverlayGroup.add(new BoolSetting.Builder()
            .name("亮度显示")
            .description("渲染亮度数值")
            .defaultValue(true)
            .build());

    private final Setting<Integer> horizontalRange = lightOverlayGroup.add(new IntSetting.Builder()
            .name("水平范围")
            .description("水平扫描范围（格）")
            .defaultValue(16)
            .min(1)
            .sliderMax(64)
            .build());

    private final Setting<Integer> verticalRange = lightOverlayGroup.add(new IntSetting.Builder()
            .name("垂直范围")
            .description("垂直扫描范围（格）")
            .defaultValue(12)
            .min(1)
            .sliderMax(64)
            .build());

    private final Setting<Boolean> seeThroughBlocks = lightOverlayGroup.add(new BoolSetting.Builder()
            .name("透视显示")
            .description("允许透过方块看到显示")
            .defaultValue(false)
            .build());

    private final Setting<Integer> spawnThreshold = lightOverlayGroup.add(new IntSetting.Builder()
            .name("刷怪阈值")
            .description("低于此亮度可能刷怪（原版1.21为0, 旧版为7, 下界猪人为11）")
            .defaultValue(0)
            .min(0)
            .sliderMax(15)
            .build());

    private final Setting<Boolean> showBox = lightOverlayGroup.add(new BoolSetting.Builder()
            .name("显示方框")
            .description("危险区域显示方框")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> showNumber = lightOverlayGroup.add(new BoolSetting.Builder()
            .name("显示数字")
            .description("显示亮度数值")
            .defaultValue(true)
            .build());

    private final Setting<Double> numberScale = lightOverlayGroup.add(new DoubleSetting.Builder()
            .name("数字大小")
            .description("数字显示大小")
            .defaultValue(0.1)
            .min(0.01)
            .sliderMax(0.2)
            .decimalPlaces(2)
            .build());

    /*private final Setting<Double> lineThickness = lightOverlayGroup.add(new DoubleSetting.Builder()
            .name("线粗细")
            .description("数字线段的粗细")
            .defaultValue(0.025)
            .min(0.005)
            .max(0.05)
            .decimalPlaces(3)
            .visible(() -> false)
            .build());

    private final Setting<Double> digitSpacing = lightOverlayGroup.add(new DoubleSetting.Builder()
            .name("两位数间隔")
            .description("两位数字之间的间隔距离")
            .defaultValue(0.05)
            .min(0.0)
            .max(0.1)
            .visible(() -> false)
            .decimalPlaces(2)
            .build());*/

    // Colors
    private final Setting<SettingColor> spawnColor = lightOverlayGroup.add(new ColorSetting.Builder()
            .name("刷怪颜色")
            .description("可能刷怪时的显示颜色")
            .defaultValue(new SettingColor(190, 0, 0))
            .build());

    private final Setting<SettingColor> safeColor = lightOverlayGroup.add(new ColorSetting.Builder()
            .name("安全颜色")
            .description("亮度安全时的显示颜色")
            .defaultValue(new SettingColor(0, 190, 0))
            .build());

    private final Pool<Marker> markerPool = new Pool<>(Marker::new);
    private final List<Marker> markers = new ArrayList<>();

    // 7段数码管段定义 (0-9)
    private static final boolean[][] DIGIT_SEGMENTS = {
            {true, true, true, false, true, true, true},     // 0
            {false, false, true, false, false, true, false}, // 1
            {true, false, true, true, true, false, true},    // 2
            {true, false, true, true, false, true, true},    // 3
            {false, true, true, true, false, true, false},   // 4
            {true, true, false, true, false, true, true},    // 5
            {true, true, false, true, true, true, true},     // 6
            {true, false, true, false, false, true, false},  // 7
            {true, true, true, true, true, true, true},      // 8
            {true, true, true, true, false, true, true},     // 9
    };

    /**
     * 预计算的7段数码管线段坐标
     * 格式: [digit][segment][x1, z1, x2, z2]
     * 坐标是归一化的（0.0 ~ 1.0），基于单位正方形
     * <p>
     * 段索引: 0=上, 1=左上, 2=右上, 3=中, 4=左下, 5=右下, 6=下
     */
    private static final double[][][] SEGMENT_LINES = new double[10][7][4];

    static {
        // 预计算每个数字的7段线段坐标
        for (int digit = 0; digit <= 9; digit++) {
            boolean[] segs = DIGIT_SEGMENTS[digit];
            double[][] lines = SEGMENT_LINES[digit];

            // 段0: 上 (水平) - 从左上到右上
            if (segs[0]) {
                lines[0][0] = 0.0;
                lines[0][1] = 0.0;
                lines[0][2] = 1.0;
                lines[0][3] = 0.0;
            }
            // 段1: 左上 (垂直) - 从左上到中左
            if (segs[1]) {
                lines[1][0] = 0.0;
                lines[1][1] = 0.0;
                lines[1][2] = 0.0;
                lines[1][3] = 1.0;
            }
            // 段2: 右上 (垂直) - 从右上到中右
            if (segs[2]) {
                lines[2][0] = 1.0;
                lines[2][1] = 0.0;
                lines[2][2] = 1.0;
                lines[2][3] = 1.0;
            }
            // 段3: 中 (水平) - 从中左到中右
            if (segs[3]) {
                lines[3][0] = 0.0;
                lines[3][1] = 1.0;
                lines[3][2] = 1.0;
                lines[3][3] = 1.0;
            }
            // 段4: 左下 (垂直) - 从中左到左下
            if (segs[4]) {
                lines[4][0] = 0.0;
                lines[4][1] = 1.0;
                lines[4][2] = 0.0;
                lines[4][3] = 2.0;
            }
            // 段5: 右下 (垂直) - 从中右到右下
            if (segs[5]) {
                lines[5][0] = 1.0;
                lines[5][1] = 1.0;
                lines[5][2] = 1.0;
                lines[5][3] = 2.0;
            }
            // 段6: 下 (水平) - 从左下到右下
            if (segs[6]) {
                lines[6][0] = 0.0;
                lines[6][1] = 2.0;
                lines[6][2] = 1.0;
                lines[6][3] = 2.0;
            }
        }
    }

    // 渲染用的临时数组，避免重复创建
    private final double[] cornersCache = new double[8];

    public RedstoneAssist() {
        super(Const.CATEGORY, "生电辅助", "显示方块亮度等级和刷怪风险区域");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!lightOverlay.get()) {
            return;
        }
        markerPool.freeAll(markers);
        markers.clear();

        Vec3d center = getCameraPos();
        int px = (int) Math.floor(center.x);
        int py = (int) Math.floor(center.y);
        int pz = (int) Math.floor(center.z);
        int hRange = horizontalRange.get();
        int vRange = verticalRange.get();

        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = px - hRange; x <= px + hRange; x++) {
            for (int z = pz - hRange; z <= pz + hRange; z++) {
                for (int y = Math.max(mc.world.getBottomY(), py - vRange); y <= py + vRange; y++) {
                    if (y > mc.world.getHeight()) break;

                    pos.set(x, y, z);
                    BlockState blockState = mc.world.getBlockState(pos);
                    BlockUtils.MobSpawn spawn = BlockUtils.isValidMobSpawn(pos, blockState, spawnThreshold.get());

                    if (spawn == BlockUtils.MobSpawn.Always || spawn == BlockUtils.MobSpawn.Potential) {
                        int lightLevel = getLightLevel(pos);
                        markers.add(markerPool.get().set(pos, false, lightLevel));
                    } else if (blockState.isAir()) {
                        BlockPos downPos = pos.down();
                        if (isValidSpawnBase(downPos)) {
                            int lightLevel = getLightLevel(pos);
                            markers.add(markerPool.get().set(pos, true, lightLevel));
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (markers.isEmpty()) return;

        Renderer3D renderer = seeThroughBlocks.get() ? event.renderer : event.depthRenderer;
        Direction facing = getCameraFacing();

        for (Marker marker : markers) {
            renderMarker(renderer, marker, facing);
        }
    }

    // ============ 渲染逻辑 ============

    private void renderMarker(Renderer3D renderer, Marker marker, Direction facing) {
        Color color = marker.safe ? safeColor.get() : spawnColor.get();
        double yo = marker.y + 0.01;

        // 危险区域显示方框，安全区域只显示数字
        if (showBox.get() && !marker.safe) {
            renderBox(renderer, marker, color, yo);
        }

        // 渲染数字（安全区域和危险区域都显示）
        if (showNumber.get()) {
            renderNumber(renderer, marker, color, yo, facing);
        }
    }

    private void renderBox(Renderer3D renderer, Marker m, Color c, double yo) {
        renderer.line(m.x, yo, m.z, m.x + 1, yo, m.z, c);
        renderer.line(m.x + 1, yo, m.z, m.x + 1, yo, m.z + 1, c);
        renderer.line(m.x + 1, yo, m.z + 1, m.x, yo, m.z + 1, c);
        renderer.line(m.x, yo, m.z + 1, m.x, yo, m.z, c);
    }

    private void renderNumber(Renderer3D renderer, Marker m, Color color, double yo, Direction facing) {
        double cx = m.x + 0.5;
        double cz = m.z + 0.5;
        double halfSize = numberScale.get();

        // 1位数
        if (m.lightLevel >= 0 && m.lightLevel <= 9) {
            calculateCorners(cx, cz, halfSize, facing, cornersCache);
            renderDigit7Segment(renderer, color, cornersCache, yo, m.lightLevel);
        }
        // 2位数
        else if (m.lightLevel >= 10 && m.lightLevel <= 15) {
            double spacing = 0.05;
            double digitHalfSize = halfSize * 0.9;
            double offset = digitHalfSize + spacing;
            double tensCx, tensCz, onesCx, onesCz;

            switch (facing) {
                case NORTH:
                    tensCx = cx - offset - 0.1;
                    tensCz = cz;
                    onesCx = cx + offset - 0.1;
                    onesCz = cz;
                    break;
                case SOUTH:
                    tensCx = cx - offset + 0.1;
                    tensCz = cz;
                    onesCx = cx + offset + 0.1;
                    onesCz = cz;
                    break;
                case WEST:
                    tensCx = cx;
                    tensCz = cz - offset + 0.1;
                    onesCx = cx;
                    onesCz = cz + offset + 0.1;
                    break;
                case EAST:
                    tensCx = cx;
                    tensCz = cz - offset - 0.1;
                    onesCx = cx;
                    onesCz = cz + offset - 0.1;
                    break;
                default:
                    tensCx = cx - offset;
                    tensCz = cz;
                    onesCx = cx + offset;
                    onesCz = cz;
            }

            if (facing == Direction.NORTH || facing == Direction.EAST) {
                calculateCorners(onesCx, onesCz, digitHalfSize, facing, cornersCache);
                renderDigit7Segment(renderer, color, cornersCache, yo, m.lightLevel % 10);
                calculateCorners(tensCx, tensCz, digitHalfSize, facing, cornersCache);
                renderDigit7Segment(renderer, color, cornersCache, yo, m.lightLevel / 10);
            } else {
                calculateCorners(onesCx, onesCz, digitHalfSize, facing, cornersCache);
                renderDigit7Segment(renderer, color, cornersCache, yo, m.lightLevel / 10);
                calculateCorners(tensCx, tensCz, digitHalfSize, facing, cornersCache);
                renderDigit7Segment(renderer, color, cornersCache, yo, m.lightLevel % 10);
            }
        }
    }

    // 计算数字的四个角坐标，结果写入 out 数组
    // out 格式: [tl.x, tl.z, tr.x, tr.z, br.x, br.z, bl.x, bl.z]
    // 注意：返回的是 1:2 宽高比的矩形（高度是宽度的2倍），以正确显示7段数码管
    private void calculateCorners(double cx, double cz, double halfWidth, Direction facing, double[] out) {
        // 7段数码管的宽高比为 1:2（宽:高）
        // 水平方向有1段，垂直方向有2段，每段长度相等
        // 所以总高度 = 2 * 总宽度
        // halfHeight 是垂直方向的一半，应该是 halfWidth 的2倍
        double halfHeight = halfWidth * 2;
        switch (facing) {
            case NORTH:
                // tl (左上)
                out[0] = cx - halfWidth;
                out[1] = cz - halfHeight;
                // tr (右上)
                out[2] = cx + halfWidth;
                out[3] = cz - halfHeight;
                // br (右下)
                out[4] = cx + halfWidth;
                out[5] = cz + halfHeight;
                // bl (左下)
                out[6] = cx - halfWidth;
                out[7] = cz + halfHeight;
                break;
            case SOUTH:
                // 南朝向时，左右翻转
                out[0] = cx + halfWidth;
                out[1] = cz + halfHeight;
                out[2] = cx - halfWidth;
                out[3] = cz + halfHeight;
                out[4] = cx - halfWidth;
                out[5] = cz - halfHeight;
                out[6] = cx + halfWidth;
                out[7] = cz - halfHeight;
                break;
            case WEST:
                // 西朝向时，旋转90度
                out[0] = cx - halfHeight;
                out[1] = cz + halfWidth;
                out[2] = cx - halfHeight;
                out[3] = cz - halfWidth;
                out[4] = cx + halfHeight;
                out[5] = cz - halfWidth;
                out[6] = cx + halfHeight;
                out[7] = cz + halfWidth;
                break;
            case EAST:
                // 东朝向时，旋转90度
                out[0] = cx + halfHeight;
                out[1] = cz - halfWidth;
                out[2] = cx + halfHeight;
                out[3] = cz + halfWidth;
                out[4] = cx - halfHeight;
                out[5] = cz + halfWidth;
                out[6] = cx - halfHeight;
                out[7] = cz - halfWidth;
                break;
            default:
                out[0] = cx - halfWidth;
                out[1] = cz - halfHeight;
                out[2] = cx + halfWidth;
                out[3] = cz - halfHeight;
                out[4] = cx + halfWidth;
                out[5] = cz + halfHeight;
                out[6] = cx - halfWidth;
                out[7] = cz + halfHeight;
        }
    }

    private void renderDigit7Segment(Renderer3D renderer, Color color, double[] corners, double y, int digit) {
        // 从 corners 数组解包四个角的坐标
        // corners: [tl.x, tl.z, tr.x, tr.z, br.x, br.z, bl.x, bl.z]
        // 注意：现在 corners 定义的是 1:2 宽高比的矩形区域（高度是宽度的2倍）
        double tlx = corners[0];
        double tlz = corners[1];
        double trx = corners[2];
        double trz = corners[3];
        double blx = corners[6];
        double blz = corners[7];

        // 计算变换参数
        // 水平方向向量: tr - tl (对应归一化坐标 u: 0~1)
        double hVecX = trx - tlx;
        double hVecZ = trz - tlz;

        // 垂直方向向量: bl - tl (对应归一化坐标 v: 0~2)
        // 由于 corners 已经是 1:2 的矩形，(bl-tl) 的长度正好是 (tr-tl) 的2倍
        // 所以每单位 v 对应 (bl-tl)/2
        double vVecX = (blx - tlx) * 0.5;
        double vVecZ = (blz - tlz) * 0.5;

        // 使用预计算的线段坐标进行渲染
        // 通过 DIGIT_SEGMENTS 判断该段是否需要绘制
        boolean[] segs = DIGIT_SEGMENTS[digit];
        double[][] lines = SEGMENT_LINES[digit];
        for (int i = 0; i < 7; i++) {
            if (segs[i]) {
                double[] line = lines[i];
                // line[0]=u1, line[1]=v1, line[2]=u2, line[3]=v2 (归一化坐标)
                // 将归一化坐标(u,v)变换到实际世界坐标:
                // world = tl + u * hVec + v * vVec_per_unit
                // 注意 vVec 已经乘了 0.5，所以直接乘以 v 即可
                double x1 = tlx + line[0] * hVecX + line[1] * vVecX;
                double z1 = tlz + line[0] * hVecZ + line[1] * vVecZ;
                double x2 = tlx + line[2] * hVecX + line[3] * vVecX;
                double z2 = tlz + line[2] * hVecZ + line[3] * vVecZ;
                drawThickLine(renderer, color, x1, y, z1, x2, y, z2);
            }
        }
    }

    private void drawThickLine(Renderer3D renderer, Color color,
                               double x1, double y1, double z1,
                               double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dz * dz);

        if (length < 0.0001) return;

        double nx = dx / length;
        double nz = dz / length;

        double perpX = -nz * 0.025;
        double perpZ = nx * 0.025;

        // 直接使用 double 值，不创建 Vec3d 对象
        double p1x = x1 + perpX, p1z = z1 + perpZ;
        double p2x = x1 - perpX, p2z = z1 - perpZ;
        double p3x = x2 - perpX, p3z = z2 - perpZ;
        double p4x = x2 + perpX, p4z = z2 + perpZ;

        renderer.quad(p1x, y1, p1z, p2x, y1, p2z, p3x, y2, p3z, p4x, y2, p4z, color);
    }

    private int getLightLevel(BlockPos pos) {
        return mc.world.getLightLevel(LightType.BLOCK, pos);
    }

    private boolean isValidSpawnBase(BlockPos pos) {
        var state = mc.world.getBlockState(pos);
        return state.isSolidBlock(mc.world, pos);
    }

    private Vec3d getCameraPos() {
        Freecam freecam = Modules.get().get(Freecam.class);
        if (freecam != null && freecam.isActive()) {
            return new Vec3d(freecam.pos.x, freecam.pos.y, freecam.pos.z);
        }
        return mc.player.getEntityPos();
    }

    private Direction getCameraFacing() {
        Freecam freecam = Modules.get().get(Freecam.class);
        if (freecam != null && freecam.isActive()) {
            return Direction.fromHorizontalDegrees(freecam.yaw);
        }
        return mc.player.getHorizontalFacing();
    }

    // ============ 数据类 ============

    public static class Marker {

        public double x, y, z;
        public boolean safe;
        public int lightLevel;

        public Marker set(BlockPos blockPos, boolean safe, int lightLevel) {
            this.x = blockPos.getX();
            this.y = blockPos.getY();
            this.z = blockPos.getZ();
            this.safe = safe;
            this.lightLevel = lightLevel;
            return this;
        }
    }
}
