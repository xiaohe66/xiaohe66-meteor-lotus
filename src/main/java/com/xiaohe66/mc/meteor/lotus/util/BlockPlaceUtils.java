package com.xiaohe66.mc.meteor.lotus.util;

import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * @author He
 * @since 2025.03.16 20:15
 */
public class BlockPlaceUtils {

    private static final Logger log = LoggerFactory.getLogger(BlockPlaceUtils.class);

    private static final double RAYCAST_OFFSET = 1e-5; // 防止浮点误差

    /**
     * @param schematicPos 投影方块
     * @param direction    Direction
     * @param rotate       旋转
     * @param swingHand    挥手
     * @param clientSide   客户端旋转
     * @return
     */
    public static boolean place(BlockPos schematicPos,
                                BlockState schematicBlockState,
                                Direction direction,
                                boolean rotate,
                                boolean swingHand,
                                boolean clientSide,
                                double printingRange) {

        // log.info("do place : {} {} {}, {}", schematicPos.getX(), schematicPos.getY(), schematicPos.getZ(), direction.getName());

        BlockPos adjacentPos = schematicPos.offset(direction);

        if (rotate) {

            BlockState adjacentState = mc.world.getBlockState(adjacentPos);
            if (!canPlaceAgainst(adjacentState, adjacentPos)) {
                return false;
            }

            Vec3d hitPos = raycastToFace(schematicBlockState, adjacentPos, direction, printingRange);

            if (hitPos == null) {
                return false;
            }

            log.info("place hitPos, {} {} {}", hitPos.getX(), hitPos.getY(), hitPos.getZ());

            double yaw = Rotations.getYaw(hitPos);
            double pitch = Rotations.getPitch(hitPos);

            log.info("place rotate, yaw : {}, pitch : {}", yaw, pitch);

            Rotations.rotate(yaw, pitch, 50, clientSide, () ->
                    place(new BlockHitResult(hitPos, direction.getOpposite(), adjacentPos, false), swingHand)
            );

        } else {

            Vec3d hitPos = new Vec3d(schematicPos.getX() + 0.5, schematicPos.getY() + 0.5, schematicPos.getZ() + 0.5);
            place(new BlockHitResult(hitPos, direction.getOpposite(), adjacentPos, false), swingHand);
        }

        return true;
    }

    private static void place(BlockHitResult blockHitResult, boolean swing) {
        if (mc.player == null || mc.interactionManager == null || mc.getNetworkHandler() == null) {
            return;
        }

        boolean wasSneaking = mc.player.isSneaking();
        mc.player.setSneaking(false);

        ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult);

        if (result instanceof ActionResult.Success) {
            if (swing) {
                mc.player.swingHand(Hand.MAIN_HAND);

            } else {
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
        }

        mc.player.setSneaking(wasSneaking);
    }

    /**
     * 获取方块的可见放置面
     *
     * @param schematicPos   投影位置
     * @param schematicState 投影方块
     * @return 最佳可见方向，若无有效面返回 null
     */
    public static Direction getVisiblePlaceSide(BlockPos schematicPos, BlockState schematicState, double printingRange) {
        if (mc.world == null || mc.player == null) {
            return null;
        }

        // 检查方块自身的方向
        Direction blockFacing = getBlockFacingDirection(schematicState);
        if (blockFacing != null) {
            Direction lookDirection = blockFacing.getOpposite();
            // log.info(lookDirection.name());
            if (isFaceVisible(schematicPos, schematicState, lookDirection, printingRange)) {
                return lookDirection;
            }

            // 存在方向性的方块在不匹配方向时直接返回
            return null;

            // 可选：检查相反方向（例如门可能需要反向放置）
            /*Direction oppositeFacing = blockFacing.getOpposite();
            if (isFaceVisible(schematicPos, schematicState, oppositeFacing, printingRange)) {
                return oppositeFacing;
            }*/
        }

        // 优先从玩家视角方向判断
        /*Direction lookDirection = getDirectionFromPlayerView(schematicPos);
        if (lookDirection != null && isFaceVisible(schematicPos, schematicState, lookDirection, printingRange)) {
            return lookDirection;
        }*/

        // 遍历所有方向寻找可见面
        for (Direction direction : Direction.values()) {
            /*if (direction == lookDirection) {
                continue;
            }*/
            if (isFaceVisible(schematicPos, schematicState, direction, printingRange)) {
                return direction;
            }
        }

        return null;
    }

    /**
     * 核心可见性判断逻辑
     *
     * @param schematicPos   投影方块位置
     * @param schematicState 投影方块
     * @param lookDirection  看向 投影方块位置的 方向
     * @param printingRange  放置距离
     * @return
     */
    private static boolean isFaceVisible(BlockPos schematicPos, BlockState schematicState, Direction lookDirection, double printingRange) {

        // 取反向，即投影方块位置的 （东/南/西/北/上/下） 面的坐标，用于尝试点击放置方块的坐标位置
        BlockPos tryClickBlockPos = schematicPos.offset(lookDirection);

        // Vec3i vector = lookDirection.getVector();
        // BlockPos tryClickPos = new BlockPos(schematicPos.getX() + vector.getX(), schematicPos.getY() + vector.getY(), schematicPos.getZ() + vector.getZ());

        BlockState tryClickBlockState = mc.world.getBlockState(tryClickBlockPos);

        // log.info("adjacent : {}-{}", tryClickPos, adjacentState.getBlock().getName());

        if (!canPlaceAgainst(tryClickBlockState, tryClickBlockPos)) {
            // log.info("canPlaceAgainst false");
            return false;
        }

        // 特殊方块处理（半砖、活板门等）
        if (schematicState.getBlock() instanceof SlabBlock slabBlock) {

            SlabType slabType = schematicState.get(SlabBlock.TYPE);
            if (lookDirection == Direction.DOWN && slabType == SlabType.TOP) {
                return false;

            } else if (lookDirection == Direction.UP && slabType == SlabType.BOTTOM) {
                return false;
            }

        } /*else if (adjacentState.getBlock() instanceof TrapdoorBlock) {
            return isTrapdoorFaceVisible(lookDirection, adjacentState);
        }*/

        // 通用射线追踪检测
        Vec3d target = raycastToFace(schematicState, tryClickBlockPos, lookDirection, printingRange);

        log.info("raycastToFace : {}, {}, {} {} {} -> {} {} {} : {}", target != null,
                lookDirection.name(),
                schematicPos.getX(), schematicPos.getY(), schematicPos.getZ(),
                tryClickBlockPos.getX(), tryClickBlockPos.getY(), tryClickBlockPos.getZ(),
                schematicState.getBlock().getName());

        return target != null;
    }

    /**
     * 辅助方法：从 BlockState 中提取方块的方向
     */
    private static Direction getBlockFacingDirection(BlockState state) {
        // 检查常见方向属性（覆盖大多数方块）
        if (state.contains(Properties.FACING)) {
            return state.get(Properties.FACING);

        } else if (state.contains(Properties.HORIZONTAL_FACING)) {
            return state.get(Properties.HORIZONTAL_FACING);

        } else if (state.contains(Properties.AXIS)) {
            // 处理轴向方块（如原木、石英柱）
            Direction.Axis axis = state.get(Properties.AXIS);
            return Direction.from(axis, Direction.AxisDirection.POSITIVE);
        }

        return null;
    }

    // 半砖可见性判断
    private static boolean isSlabFaceVisible(BlockPos schematicPos, Direction direction, BlockState slabState) {
        SlabType slabType = slabState.get(SlabBlock.TYPE);
        if (slabType == SlabType.DOUBLE) {
            return false;
        }

        // 根据半砖类型修正检测区域
        Box collisionBox = slabState.getCollisionShape(mc.world, schematicPos).getBoundingBox();
        double minY = (slabType == SlabType.BOTTOM) ? 0.0 : 0.5;
        double maxY = (slabType == SlabType.TOP) ? 1.0 : 0.5;
        Box adjustedBox = new Box(collisionBox.minX, minY, collisionBox.minZ, collisionBox.maxX, maxY, collisionBox.maxZ);

        return raycastToAdjustedBox(schematicPos, direction, adjustedBox);
    }

    // 活板门可见性判断
    private static boolean isTrapdoorFaceVisible(Direction dir, BlockState trapdoorState) {
        Direction facing = trapdoorState.get(TrapdoorBlock.FACING);
        Boolean open = trapdoorState.get(TrapdoorBlock.OPEN);
        // 活板门关闭时只能从正面附着
        return !open && dir == facing;
    }

    /**
     * 通用射线追踪方法
     *
     * @param adjacentPos 被点击方块坐标
     * @param direction   被点击方块 在 投影方块的 方向
     */
    private static Vec3d raycastToFace(BlockState schematicState, BlockPos adjacentPos, Direction direction, double printingRange) {

        Direction opposite = direction.getOpposite();

        // 1. 计算目标面的中心点（世界坐标）
        Vec3d faceCenter = getFaceCenter(adjacentPos, opposite);

        // 兼容上下半砖
        BlockState adjacentBlockState = mc.world.getBlockState(adjacentPos);
        faceCenter = fixSlabBlock(schematicState, adjacentBlockState, faceCenter);

        // log.info("faceCenter : {}", faceCenter);

        // 2. 获取玩家眼睛位置
        Vec3d eyePos = mc.player.getCameraPosVec(1.0F);

        // 3. 计算射线方向（从眼睛到目标面中心）
        Vec3d rayVec = faceCenter.subtract(eyePos);
        double maxDistance = Math.min(eyePos.distanceTo(faceCenter) + 0.5, printingRange);
        Vec3d end = eyePos.add(rayVec.normalize().multiply(maxDistance));

        log.info(" eyePos : {} {} {}, faceCenter : {} {} {}, end : {} {} {}",
                eyePos.getX(), eyePos.getY(), eyePos.getZ(),
                faceCenter.getX(), faceCenter.getY(), faceCenter.getZ(),
                end.getX(), end.getY(), end.getZ());

        // 4. 执行射线追踪
        RaycastContext context = new RaycastContext(
                eyePos,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        );

        BlockHitResult hit = mc.world.raycast(context);

        BlockPos hitBlockPos = hit.getBlockPos();

        log.info("hit pos, {},{},{},{}",
                hitBlockPos.getX(), hitBlockPos.getY(), hitBlockPos.getZ(),
                hit.getSide());

        // 5. 验证命中结果
        boolean ok = hit.getType() == HitResult.Type.BLOCK &&
                hit.getBlockPos().equals(adjacentPos) &&
                hit.getSide() == opposite;

        return ok ? faceCenter : null;
    }

    private static Vec3d fixSlabBlock(BlockState schematicBlockState, BlockState adjacentBlockState, Vec3d faceCenter) {

        Block schematicBlock = schematicBlockState.getBlock();

        if (!(schematicBlock instanceof SlabBlock)) {
            return faceCenter;
        }

        Block adjacentBlock = adjacentBlockState.getBlock();

        if (!(adjacentBlock instanceof SlabBlock)) {

            SlabType schematicBlockSlabType = schematicBlockState.get(SlabBlock.TYPE);

            if (schematicBlockSlabType == SlabType.TOP) {
                return new Vec3d(faceCenter.getX(), faceCenter.getY() + 0.25, faceCenter.getZ());

            } else if (schematicBlockSlabType == SlabType.BOTTOM) {
                return new Vec3d(faceCenter.getX(), faceCenter.getY() - 0.25, faceCenter.getZ());
            }
        }

        // if (schematicBlockSlabType != adjacentBlockSlabType) {
        // TODO : 上半砖 和 下半砖无法互相放置
        // }

        return faceCenter;
    }

    /**
     * 获取目标方块某面的中心点坐标
     */
    private static Vec3d getFaceCenter(BlockPos pos, Direction direction) {

        Box box = mc.world.getBlockState(pos).getOutlineShape(mc.world, pos).getBoundingBox();
        double x = pos.getX() + box.minX + (box.maxX - box.minX) * 0.5;
        double y = pos.getY() + box.minY + (box.maxY - box.minY) * 0.5;
        double z = pos.getZ() + box.minZ + (box.maxZ - box.minZ) * 0.5;

        // 根据方向偏移到面中心
        return new Vec3d(
                x + direction.getOffsetX() * (box.maxX - box.minX) * 0.5,
                y + direction.getOffsetY() * (box.maxY - box.minY) * 0.5,
                z + direction.getOffsetZ() * (box.maxZ - box.minZ) * 0.5
        );
    }

    // 获取玩家视角方向（带优先级过滤）
    private static Direction getDirectionFromPlayerView(BlockPos targetPos) {
        Vec3d eyePos = new Vec3d(mc.player.getX(),
                mc.player.getEyeY(),
                mc.player.getZ());
        Vec3d targetCenter = Vec3d.ofCenter(targetPos);
        Vec3d lookVec = targetCenter.subtract(eyePos).normalize();

        // 优先选择与视线方向最接近的轴向
        return Direction.getFacing(lookVec.x, lookVec.y, lookVec.z);
    }

    /**
     * 判断是否可以附着到目标方块
     *
     * @param tryClickBlockState 被附着的方块状态
     * @param tryClickPos        被附着的方块状态
     * @return 是否允许放置
     */
    private static boolean canPlaceAgainst(BlockState tryClickBlockState, BlockPos tryClickPos) {
        // 使用实际位置检测
        if (tryClickBlockState.isAir() || tryClickBlockState.isLiquid()) {
            return false;
        }

        VoxelShape shape = tryClickBlockState.getCollisionShape(mc.world, tryClickPos);
        if (shape.isEmpty()) {
            return false;
        }

        // 特殊方块白名单（如围栏、按钮）
        Block block = tryClickBlockState.getBlock();
        /*if (block instanceof FenceBlock || block instanceof AbstractButtonBlock) {
            return true;
        }*/

        // 黑名单（如玻璃、冰）
        if (block == Blocks.GLASS || block == Blocks.ICE) {
            return false;
        }

        return Block.isShapeFullCube(shape) ||
                tryClickBlockState.getBlock() == Blocks.GLASS ||
                tryClickBlockState.getBlock() instanceof StainedGlassBlock ||
                tryClickBlockState.getBlock() instanceof StairsBlock ||
                tryClickBlockState.getBlock() instanceof SlabBlock;
    }

    /**
     * 对自定义碰撞箱进行射线追踪检测
     *
     * @param schematicPos 目标方块位置
     * @param direction    检测方向
     * @param adjustedBox  自定义碰撞箱
     * @return 是否可见
     */
    private static boolean raycastToAdjustedBox(BlockPos schematicPos, Direction direction, Box adjustedBox) {

        Direction opposite = direction.getOpposite();

        Vec3d eyePos = mc.player.getCameraPosVec(1.0F);

        // 在碰撞箱内生成采样点（优化性能：每个轴采样3个点）
        final int samples = 3;
        double xStep = (adjustedBox.maxX - adjustedBox.minX) / (samples + 1);
        double yStep = (adjustedBox.maxY - adjustedBox.minY) / (samples + 1);
        double zStep = (adjustedBox.maxZ - adjustedBox.minZ) / (samples + 1);

        for (int i = 1; i <= samples; i++) {
            for (int j = 1; j <= samples; j++) {
                for (int k = 1; k <= samples; k++) {
                    // 计算采样点坐标（局部坐标系）
                    double x = adjustedBox.minX + xStep * i;
                    double y = adjustedBox.minY + yStep * j;
                    double z = adjustedBox.minZ + zStep * k;

                    // 转换为世界坐标
                    Vec3d point = new Vec3d(
                            schematicPos.getX() + x,
                            schematicPos.getY() + y,
                            schematicPos.getZ() + z
                    );

                    // 执行射线追踪
                    BlockHitResult hit = mc.world.raycast(new RaycastContext(
                            eyePos,
                            point,
                            RaycastContext.ShapeType.COLLIDER,
                            RaycastContext.FluidHandling.NONE,
                            mc.player
                    ));

                    // 验证命中结果
                    if (hit.getType() == HitResult.Type.BLOCK &&
                            hit.getBlockPos().equals(schematicPos) &&
                            hit.getSide() == opposite) {

                        log.info("raycastToAdjustedBox return false");

                        return true;
                    }
                }
            }
        }

        log.info("raycastToAdjustedBox return false");

        return false;
    }

    //     ---------------
    /*public static boolean place(BlockPos targetPos, Direction placeAgainst,
                                SlabType slabType, BlockHalf blockHalf,
                                Direction facing, Axis axis,
                                boolean airPlace, boolean swingHand,
                                boolean rotatePlayer, int range) {
        if (mc.player == null || !canPlaceAt(targetPos, airPlace)) return false;

        // 获取附着面信息
        PlacementContext context = resolvePlacementContext(targetPos, placeAgainst, airPlace);
        if (context == null) return false;

        // 计算放置参数
        BlockState stateToPlace = buildBlockState(mc.player.getMainHandStack().getItem(),
                slabType, blockHalf, facing, axis);

        // 执行放置
        return doPlacement(context, stateToPlace, swingHand, rotatePlayer, range);
    }

    // region 核心逻辑分解
    private static boolean canPlaceAt(BlockPos pos, boolean airPlace) {
        // 空中放置直接允许（如末地烛）
        if (airPlace) return true;
        // 常规放置需检查附着面
        BlockState state = mc.world.getBlockState(pos.offset(placeAgainst.getOpposite()));
        return !state.isAir() && state.isSolid();
    }

    private static PlacementContext resolvePlacementContext(BlockPos targetPos, Direction placeAgainst, boolean airPlace) {
        if (airPlace) {
            // 空中放置的附着点为目标位置本身
            return new PlacementContext(targetPos, placeAgainst, Vec3d.ofCenter(targetPos));
        }

        // 计算精确附着点
        BlockPos adjacentPos = targetPos.offset(placeAgainst.getOpposite());
        BlockState adjacentState = mc.world.getBlockState(adjacentPos);
        Box collisionBox = adjacentState.getCollisionShape(mc.world, adjacentPos).getBoundingBox();

        // 特殊方块处理（半砖、楼梯等）
        Vec3d hitPos = calculateHitPos(adjacentPos, placeAgainst, collisionBox);
        return new PlacementContext(adjacentPos, placeAgainst, hitPos);
    }

    private static Vec3d calculateHitPos(BlockPos adjacentPos, Direction dir, Box box) {
        // 根据方向计算碰撞箱面上的中心点
        double x = adjacentPos.getX() + box.minX + (box.maxX - box.minX) * 0.5;
        double y = adjacentPos.getY() + box.minY + (box.maxY - box.minY) * 0.5;
        double z = adjacentPos.getZ() + box.minZ + (box.maxZ - box.minZ) * 0.5;

        // 偏移到面边界
        return new Vec3d(x, y, z)
                .add(dir.getOffsetX() * (0.5 - RAYCAST_OFFSET),
                        dir.getOffsetY() * (0.5 - RAYCAST_OFFSET),
                        dir.getOffsetZ() * (0.5 - RAYCAST_OFFSET));
    }

    private static BlockState buildBlockState(Item item, SlabType slabType,
                                              BlockHalf half, Direction facing, Axis axis) {

        Block block = Block.getBlockFromItem(item);
        BlockState state = block.getDefaultState();

        // 半砖类型
        if (block instanceof SlabBlock && slabType != null) {
            state = state.with(SlabBlock.TYPE, slabType);
        }

        // 分块设置（如楼梯）
        if (block instanceof StairsBlock && half != null) {
            state = state.with(StairsBlock.HALF, half);
        }

        // 水平朝向（如活板门）
        if (block instanceof HorizontalFacingBlock && facing != null) {
            state = state.with(HorizontalFacingBlock.FACING, facing);
        }

        // 轴对齐（如原木）
        if (block instanceof PillarBlock && axis != null) {
            state = state.with(PillarBlock.AXIS, axis);
        }

        return state;
    }

    private static boolean doPlacement(PlacementContext context, BlockState state,
                                       boolean swingHand, boolean rotate, int range) {
        // 计算需要的玩家朝向
        Rotation targetRot = calculateRotation(context.hitPos(), range);

        // 执行视角旋转
        if (rotate) {
            Rotations.rotate(targetRot.yaw(), targetRot.pitch(), 50, true, () ->
                    performPlacement(context, state, swingHand));
        } else {
            performPlacement(context, state, swingHand);
        }

        return true;
    }

    private static Rotation calculateRotation(Vec3d hitPos, int range) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d direction = hitPos.subtract(eyePos).normalize();
        return new Rotation(
                (float) Math.toDegrees(Math.atan2(-direction.x, direction.z)),
                (float) Math.toDegrees(Math.asin(direction.y))
        );
    }

    private static void performPlacement(PlacementContext context, BlockState state, boolean swing) {
        // 客户端模拟
        if (mc.interactionManager != null) {
            mc.interactionManager.interactBlock(
                    mc.player, Hand.MAIN_HAND,
                    new BlockHitResult(context.hitPos(), context.side(),
                            context.pos(), false)
            );
        }

        // 服务端同步
        ClientPlayerInteractionManager interaction = mc.interactionManager;
        if (interaction != null) {
            interaction.attackBlock(context.pos(), context.side());
            interaction.placeBlock(context.pos(), context.side(), state);
        }

        if (swing) mc.player.swingHand(Hand.MAIN_HAND);
    }*/
    // endregion

    // 辅助记录类
    private record PlacementContext(BlockPos pos, Direction side, Vec3d hitPos) {

    }
}
