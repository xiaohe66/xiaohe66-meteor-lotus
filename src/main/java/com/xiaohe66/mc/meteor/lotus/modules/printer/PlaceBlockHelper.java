package com.xiaohe66.mc.meteor.lotus.modules.printer;

import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * @author He
 * @since 2025.06.02 12:41
 */
public class PlaceBlockHelper {

    private static final Logger log = LoggerFactory.getLogger(PlaceBlockHelper.class);

    private final boolean debug;

    private final BlockPos schematicBlockPos;
    private final BlockState schematicBlockState;
    private final Block schematicBlock;

    private final double printingRange;
    private final boolean rotate;
    private boolean swingHand;

    private boolean airPlace;

    /**
     * 看向 投影方块位置的 方向
     */
    private Direction schematicBlockOffsetDir;

    private Direction blockFacing;

    private BlockPos tryClickBlockPos;
    private BlockState tryClickBlockState;
    private Block tryClickBlock;

    private Vec3d hitPos;

    public PlaceBlockHelper(boolean debug,
                            BlockPos schematicBlockPos,
                            BlockState schematicBlockState,
                            double printingRange,
                            boolean rotate) {

        this.debug = debug;
        this.schematicBlockPos = schematicBlockPos;
        this.schematicBlockState = schematicBlockState;
        this.printingRange = printingRange;
        this.rotate = rotate;

        this.schematicBlock = schematicBlockState.getBlock();
    }

    /**
     * 获取方块的可见放置面
     *
     * @return 看向投影的方向
     */
    public Direction getCanPlaceDirection() {
        blockFacing = HeBlockUtils.getBlockFacingDirection(schematicBlockState);

        // 空气放置
        if (airPlace) {
            Direction direction = blockFacing != null ? blockFacing : Direction.UP;
            setTmpVariable(direction);
            return direction;
        }

        // 带方向的方块
        if (blockFacing != null) {
            return getCanPlaceDirectionHaveFacing(blockFacing);
        }

        // 普通方块
        for (Direction offsetDir : Direction.values()) {
            if (canViewDirection(offsetDir)) {
                return offsetDir;
            }
        }

        return null;
    }

    private Direction getCanPlaceDirectionHaveFacing(Direction blockFacing) {

        // 楼梯
        if (schematicBlock instanceof StairsBlock stairsBlock) {
            return getCanPlaceDirectionStairsBlock(blockFacing);
        }

        // 其他方向方块
        Direction lookDirection = blockFacing.getOpposite();
        if (canViewDirection(lookDirection)) {
            return lookDirection;
        }

        // 可选：检查相反方向（例如门可能需要反向放置）
            /*Direction oppositeFacing = blockFacing.getOpposite();
            if (isFaceVisible(schematicPos, schematicState, oppositeFacing, printingRange)) {
                return oppositeFacing;
            }*/

        // TODO : 带方向性的方块兼容
        return null;
    }

    private Direction getCanPlaceDirectionStairsBlock(Direction blockFacing) {
        BlockHalf blockHalf = schematicBlockState.get(StairsBlock.HALF);

        // 正楼梯
        if (blockHalf == BlockHalf.BOTTOM) {

            printLog("handel bottom StairsBlock : {}", blockFacing);

            if (canViewDirection(Direction.DOWN)) {
                return Direction.DOWN;
            }

            if (canViewDirection(blockFacing)) {
                return blockFacing;
            }

            Direction direction = blockFacing.rotateYClockwise();
            if (canViewDirection(direction)) {
                return direction;
            }

            direction = blockFacing.rotateYCounterclockwise();
            if (canViewDirection(direction)) {
                return direction;
            }
        }

        // TODO : 反楼梯

        return null;
    }

    /**
     * @param offsetDir 看向 投影方块位置的 方向
     */
    private boolean canViewDirection(Direction offsetDir) {

        setTmpVariable(offsetDir);

        if (canNotClick()) {
            return false;
        }

        // 半砖处理
        if (schematicBlock instanceof SlabBlock slabBlock) {

            SlabType slabType = schematicBlockState.get(SlabBlock.TYPE);
            if (offsetDir == Direction.DOWN && slabType == SlabType.TOP) {
                return false;

            } else if (offsetDir == Direction.UP && slabType == SlabType.BOTTOM) {
                return false;
            }
        }
        // 活板门处理
        else if (schematicBlock instanceof TrapdoorBlock) {
            // TODO : 活板门
            // return isTrapdoorFaceVisible(lookDirection, adjacentState);
        }

        // 通用射线追踪检测
        hitPos = rayToFace();

        printLog("raycastToFace : {}, {}, {} {} {} -> {} {} {} : {}", hitPos != null,
                schematicBlockOffsetDir.name(),
                schematicBlockPos.getX(), schematicBlockPos.getY(), schematicBlockPos.getZ(),
                tryClickBlockPos.getX(), tryClickBlockPos.getY(), tryClickBlockPos.getZ(),
                schematicBlock.getName());

        if (hitPos == null) {
            return false;
        }

        // 楼梯
        if (schematicBlock instanceof StairsBlock) {
            boolean ret = isSameHorizontalDirection(mc.player.getEyePos(), hitPos, blockFacing);

            printLog("StairsBlock result : {}, offsetDir : {}", ret, blockFacing);

            return ret;
        }

        return true;
    }

    private void setTmpVariable(Direction offsetDir) {
        schematicBlockOffsetDir = offsetDir;

        // 取反向，即投影方块位置的 （东/南/西/北/上/下） 面的坐标，用于尝试点击放置方块的坐标位置
        tryClickBlockPos = schematicBlockPos.offset(offsetDir);
        tryClickBlockState = mc.world.getBlockState(tryClickBlockPos);
        tryClickBlock = tryClickBlockState.getBlock();
    }

    public boolean isSameHorizontalDirection(Vec3d eyePos, Vec3d hitPos, Direction offsetDir) {
        // 1. 计算从眼睛位置到点击位置的向量
        Vec3d directionVector = hitPos.subtract(eyePos);

        // 2. 提取水平分量（忽略Y轴）
        double horizontalX = directionVector.x;
        double horizontalZ = directionVector.z;

        // 3. 计算水平向量的长度（避免除零错误）
        double length = Math.sqrt(horizontalX * horizontalX + horizontalZ * horizontalZ);
        if (length < 1e-5) { // 如果向量太小，视为无效
            return false;
        }

        // 4. 归一化水平向量
        double normalizedX = horizontalX / length;
        double normalizedZ = horizontalZ / length;

        // 5. 获取楼梯方向的单位向量
        Vec3d stairVector = Vec3d.of(offsetDir.getVector());
        double stairX = stairVector.x;
        double stairZ = stairVector.z;

        // 6. 计算点积（余弦值）
        double dotProduct = normalizedX * stairX + normalizedZ * stairZ;

        printLog("dotProduct : {}", dotProduct);

        // 7. 45度角的余弦值约为0.707
        return dotProduct > 0.707;
    }

    private boolean canNotClick() {
        boolean canClick = canClick();

        printLog("canClick : {}, {} {} {}", canClick, tryClickBlockPos.getX(), tryClickBlockPos.getY(), tryClickBlockPos.getZ());

        return !canClick;
    }

    /**
     * TODO : 考虑移到 HeBlockUtils
     */
    private boolean canClick() {
        // 使用实际位置检测
        if (tryClickBlockState.isAir() || tryClickBlockState.isLiquid()) {
            printLog("canClick liquid");
            return false;
        }

        VoxelShape shape = tryClickBlockState.getCollisionShape(mc.world, tryClickBlockPos);
        if (shape.isEmpty()) {
            printLog("CollisionShape empty");
            return false;
        }

        // 特殊方块白名单（如围栏、按钮）
        /*if (block instanceof FenceBlock || block instanceof AbstractButtonBlock) {
            return true;
        }*/

        // 黑名单（如玻璃、冰）
        /*if (tryClickBlock == Blocks.GLASS || tryClickBlock == Blocks.ICE) {
            return false;
        }*/

        return Block.isShapeFullCube(shape) ||
                tryClickBlock == Blocks.GLASS ||
                tryClickBlock instanceof StainedGlassBlock ||
                tryClickBlock instanceof StairsBlock ||
                tryClickBlock instanceof SlabBlock;
    }

    private Vec3d rayToFace() {

        // 尝试点击的方向(方块内部->方块外部)
        Direction hitDir = schematicBlockOffsetDir.getOpposite();

        // 1. 计算点击面的中心点（世界坐标）
        Vec3d hitCenter = HeBlockUtils.getFaceCenter(tryClickBlockPos, hitDir);

        // 兼容上下半砖、楼梯等方块
        hitCenter = tryFixHitCenter(hitCenter);

        // printLog("hitCenter : {}", hitCenter);

        // 2. 获取玩家眼睛位置
        Vec3d eyePos = mc.player.getCameraPosVec(1.0F);

        // 3. 计算射线方向（从眼睛到目标面中心）
        Vec3d rayVec = hitCenter.subtract(eyePos);
        double maxDistance = Math.min(eyePos.distanceTo(hitCenter) + 0.5, printingRange);
        Vec3d end = eyePos.add(rayVec.normalize().multiply(maxDistance));

        /*printLog(" eyePos : {} {} {}, hitCenter : {} {} {}, end : {} {} {}",
                eyePos.getX(), eyePos.getY(), eyePos.getZ(),
                hitCenter.getX(), hitCenter.getY(), hitCenter.getZ(),
                end.getX(), end.getY(), end.getZ());*/

        // 4. 执行射线追踪
        RaycastContext context = new RaycastContext(eyePos, end, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, mc.player);

        BlockHitResult hitResult = mc.world.raycast(context);

        BlockPos hitBlockPos = hitResult.getBlockPos();

        printLog("hitBlockPos, {},{},{},{}",
                hitBlockPos.getX(), hitBlockPos.getY(), hitBlockPos.getZ(),
                hitResult.getSide());

        // 5. 验证命中结果
        boolean ok = hitResult.getType() == HitResult.Type.BLOCK &&
                hitResult.getBlockPos().equals(tryClickBlockPos) &&
                hitResult.getSide() == hitDir;

        return ok ? hitCenter : null;
    }


    private Vec3d tryFixHitCenter(Vec3d hitCenter) {
        // 放置半砖
        if (schematicBlock instanceof SlabBlock) {
            if (tryClickBlock instanceof SlabBlock) {
                return hitCenter;
            }

            SlabType schematicBlockSlabType = schematicBlockState.get(SlabBlock.TYPE);

            if (schematicBlockSlabType == SlabType.TOP) {
                return new Vec3d(hitCenter.getX(), hitCenter.getY() + 0.25, hitCenter.getZ());

            } else if (schematicBlockSlabType == SlabType.BOTTOM) {
                return new Vec3d(hitCenter.getX(), hitCenter.getY() - 0.25, hitCenter.getZ());
            }

            // if (schematicBlockSlabType != adjacentBlockSlabType) {
            // TODO : 上半砖 和 下半砖无法互相放置
            // }

            return hitCenter;

        }
        // 放置楼梯
        else if (schematicBlock instanceof StairsBlock) {
            // TODO : 被点击的方块是楼梯

        }
        return hitCenter;
    }

    public boolean tryPlace() {

        printLog("tryPlace : {} {} {}", schematicBlockPos.getX(), schematicBlockPos.getY(), schematicBlockPos.getZ());

        if (mc.player == null || mc.world == null) {
            return false;
        }

        /*if (!schematicBlockState.isReplaceable()) {
            printLog("tryPlace fail, isReplaceable false : {}", schematicBlockState);
            return false;
        }*/

        BlockState worldBlockState = mc.world.getBlockState(schematicBlockPos);
        if (!worldBlockState.isReplaceable()) {
            printLog("tryPlace fail, isReplaceable false : {}", schematicBlockState);
            return false;
        }

        if (airPlace || canViewDirection(schematicBlockOffsetDir)) {
            return place();

        } else {
            printLog("tryPlace fail, canViewDirection false : {}", schematicBlockOffsetDir);
            return false;
        }
    }

    private boolean place() {

        printLog("place : {}", schematicBlockPos);

        if (airPlace) {
            // TODO : 空气放置时, 放置的方向是玩家的朝向
            /*if(blockFacing != null){

                Rotations.rotate(,0);
            }*/

            // Direction opposite = schematicBlockOffsetDir.getOpposite();
            placeAir(schematicBlockPos);

            return true;
        }

        if (canNotClick()) {
            return false;
        }

        Direction opposite = schematicBlockOffsetDir.getOpposite();

        if (rotate) {

            printLog("place hitPos, {} {} {}", hitPos.getX(), hitPos.getY(), hitPos.getZ());

            double yaw = Rotations.getYaw(hitPos);
            double pitch = Rotations.getPitch(hitPos);

            printLog("place rotate, yaw : {}, pitch : {}", yaw, pitch);

            // return BlockPlaceUtils.place(schematicPos, required, placeSide, rotate.get(), swing.get(), clientSide.get(), printingRange);

            Rotations.rotate(yaw, pitch, 50, true, () -> {

                BlockHitResult blockHitResult = new BlockHitResult(hitPos, opposite, tryClickBlockPos, false);
                place(blockHitResult, true);
            });

        } else {

            Vec3d hitPos = new Vec3d(schematicBlockPos.getX() + 0.5, schematicBlockPos.getY() + 0.5, schematicBlockPos.getZ() + 0.5);
            place(new BlockHitResult(hitPos, opposite, tryClickBlockPos, false), swingHand);
        }

        return true;
    }

    private void place(BlockHitResult blockHitResult, boolean swing) {
        if (mc.player == null || mc.interactionManager == null || mc.getNetworkHandler() == null) {
            return;
        }

        // boolean wasSneaking = mc.player.isSneaking();
        // mc.player.setSneaking(false);

        ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult);

        if (result instanceof ActionResult.Success) {
            if (swing) {
                mc.player.swingHand(Hand.MAIN_HAND);

            } else {
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
        }
    }

    private void placeAir(BlockPos blockPos) {

        BlockHitResult blockHitResult = new BlockHitResult(Vec3d.ofCenter(blockPos), Direction.UP, blockPos, false);
        
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
        mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.OFF_HAND, blockHitResult, mc.player.currentScreenHandler.getRevision() + 2));
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
    }

    private void printLog(String str, Object arg) {
        if (debug) {
            log.info(str, arg);
        }
    }

    private void printLog(String str, Object... args) {
        if (debug) {
            log.info(str, args);
        }
    }

    // 下面是get/set

    public BlockPos getSchematicBlockPos() {
        return schematicBlockPos;
    }


    public BlockState getSchematicBlockState() {
        return schematicBlockState;
    }

    public Block getSchematicBlock() {
        return schematicBlock;
    }

    public void setAirPlace(boolean airPlace) {
        this.airPlace = airPlace;
    }
}
