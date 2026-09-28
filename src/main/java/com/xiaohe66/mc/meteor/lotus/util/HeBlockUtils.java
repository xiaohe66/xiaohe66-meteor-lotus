package com.xiaohe66.mc.meteor.lotus.util;

import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.List;

/**
 * @author He
 * @since 2025.05.11 21:00
 */
public class HeBlockUtils {

    public static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void open(BlockPos pos) {
        Direction clickSide = BlockUtils.getDirection(pos);
        open(pos, clickSide);
    }

    public static void open(BlockPos pos, Direction side) {
        Vec3i vector = side.getVector();
        double offset = 0.45;
        Vec3d directionVec = new Vec3d(pos.getX() + 0.5 + vector.getX() * offset, pos.getY() + 0.5 + vector.getY() * offset, pos.getZ() + 0.5 + vector.getZ() * offset);
        open(pos, side, directionVec);
    }

    public static void open(BlockPos pos, Direction side, Vec3d directionVec) {
        BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, result);
    }

    /**
     * @param blockPos 放置的位置
     * @param slot     热栏的哪个格子
     * @param side     在需要放置的哪个方向点击
     */
    public static boolean place(BlockPos blockPos, int slot, boolean checkEntities, Direction side) {
        if (slot < 0 || slot > 8) {
            return false;
        }

        Block toPlace = Blocks.OBSIDIAN;
        PlayerInventory inventory = mc.player.getInventory();
        ItemStack itemStack = inventory.getStack(slot);

        if (itemStack.getItem() instanceof BlockItem blockItem) {
            toPlace = blockItem.getBlock();
        }

        if (!BlockUtils.canPlaceBlock(blockPos, checkEntities, toPlace)) {
            return false;
        }

        BlockPos neighbour = blockPos.offset(side);

        Vec3i vector = side.getOpposite().getVector();
        double offset = 0.45;
        Vec3d directionVec = new Vec3d(neighbour.getX() + 0.5 + vector.getX() * offset, neighbour.getY() + 0.5 + vector.getY() * offset, neighbour.getZ() + 0.5 + vector.getZ() * offset);
        BlockHitResult bhr = new BlockHitResult(directionVec, side.getOpposite(), neighbour, false);

        if (inventory.getSelectedSlot() != slot) {
            InvUtils.swap(slot, false);
        }
        HeRotationUtils.rotate(directionVec, () -> {
            BlockUtils.interact(bhr, Hand.MAIN_HAND, true);
        });

        return true;
    }

    public static List<BlockPos> listPosInSphere(int range, BlockPos pos) {
        Vec3d centerPos = pos.toCenterPos();

        List<BlockPos> list = new ArrayList<>();
        for (int x = pos.getX() - range; x < pos.getX() + range; ++x) {
            for (int z = pos.getZ() - range; z < pos.getZ() + range; ++z) {
                for (int y = pos.getY() - range; y < pos.getY() + range; ++y) {

                    BlockPos curPos = new BlockPos(x, y, z);
                    if (curPos.toCenterPos().distanceTo(centerPos) > range) {
                        continue;
                    }
                    if (!list.contains(curPos)) {
                        list.add(curPos);
                    }
                }
            }
        }
        return list;
    }

    public static List<BlockPos> listPosInSphere(int range, int height, BlockPos pos) {
        List<BlockPos> list = new ArrayList<>();
        for (int x = pos.getX() - range; x < pos.getX() + range; ++x) {
            for (int z = pos.getZ() - range; z < pos.getZ() + range; ++z) {
                for (int y = pos.getY() - height; y < pos.getY() + height; ++y) {

                    BlockPos curPos = new BlockPos(x, y, z);
                    list.add(curPos);
                }
            }
        }
        return list;
    }

    /**
     * 辅助方法：从 BlockState 中提取方块的方向
     */
    public static Direction getBlockFacingDirection(BlockState state) {
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


    /**
     * 获取目标方块某面的中心点坐标
     */
    public static Vec3d getFaceCenter(BlockPos pos, Direction direction) {

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

    /**
     * 判断是否可以站立，但仅判断空气
     */
    public static BlockPos getCanStandPos(BlockPos pos, int range) {
        for (int x = pos.getX() - range; x <= pos.getX() + range; ++x) {
            for (int z = pos.getZ() - range; z <= pos.getZ() + range; ++z) {
                BlockPos curPos = new BlockPos(x, pos.getY(), z);
                if (HeBlockUtils.canStand(curPos)) {
                    return curPos;
                }
            }
        }
        return null;
    }

    public static boolean canStand(BlockPos blockPos) {
        return mc.world.getBlockState(blockPos).isAir() && mc.world.getBlockState(blockPos.up()).isAir();
    }
}
