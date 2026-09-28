package com.xiaohe66.mc.meteor.lotus.util;

import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author He
 * @since 2026.01.02 17:47
 */
public class HePosUtils {

    public static final MinecraftClient mc = MinecraftClient.getInstance();

    /**
     * 获取展示框下 2格kit的位置, 以 Map 的形式返回
     */
    public static Map<ItemBo, BlockPos> getItemFrameKitPosMap(int range) {
        // 获取玩家周围的所有展示框
        List<ItemFrameEntity> itemFrames = mc.world.getEntitiesByClass(
                ItemFrameEntity.class,
                mc.player.getBoundingBox().expand(range),
                frame -> true);

        Map<ItemBo, BlockPos> kitPosMap = new HashMap<>();

        for (ItemFrameEntity frame : itemFrames) {

            ItemStack frameHeldItemStack = frame.getHeldItemStack();
            if (frameHeldItemStack.isEmpty()) {
                continue;
            }

            BlockPos frameBlockPos = frame.getBlockPos();
            BlockPos putPos = frameBlockPos.offset(Direction.Axis.Y, -2);
            BlockState putPosState = mc.world.getBlockState(putPos);
            if (!HeItemUtils.isShulkerBox(putPosState.getBlock().asItem())) {
                continue;
            }

            ItemBo key = new ItemBo(frameHeldItemStack);
            kitPosMap.put(key, putPos);
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (ItemBo villagerItem : kitPosMap.keySet()) {
            stringBuilder.append(',').append(villagerItem.getName());
        }
        ChatUtils.info("成功识别: " + stringBuilder.substring(1));

        return kitPosMap;
    }

    public static Optional<BlockPos> getOtherChestPos(ItemFrameEntity itemFrame) {
        BlockPos attachedPos = itemFrame.getAttachedBlockPos();
        if (attachedPos == null) {
            return Optional.empty();
        }

        BlockPos chestPos = attachedPos.offset(itemFrame.getFacing().getOpposite());
        BlockState chestState = mc.world.getBlockState(chestPos);
        if (chestState.getBlock() != Blocks.CHEST) {
            return Optional.empty();
        }

        // 尝试获取连接的另一个箱子实体
        ChestType chestType = chestState.get(ChestBlock.CHEST_TYPE);
        if (chestType == ChestType.SINGLE) {
            // 单箱子，没有另一个部分
            return Optional.empty();
        }

        // 获取方块实体
        BlockEntity blockEntity = mc.world.getBlockEntity(chestPos);
        if (!(blockEntity instanceof ChestBlockEntity chestEntity)) {
            return Optional.empty();
        }

        // 5. 获取箱子的朝向
        Direction facing = chestState.get(ChestBlock.FACING);

        // 6. 根据箱子类型计算另一个部分的位置
        BlockPos otherPos = null;

        if (chestType == ChestType.LEFT) {
            // 如果是左箱子，右箱子在facing方向的右侧
            otherPos = getRightChestPos(chestPos, facing);
        } else if (chestType == ChestType.RIGHT) {
            // 如果是右箱子，左箱子在facing方向的左侧
            otherPos = getLeftChestPos(chestPos, facing);
        }

        if (otherPos == null) {
            return Optional.empty();
        }

        // 7. 验证另一个位置确实是箱子
        BlockState otherState = mc.world.getBlockState(otherPos);
        if (otherState.getBlock() != Blocks.CHEST) {
            return Optional.empty();
        }

        // 8. 确保另一个箱子是同一个大箱子的一部分
        ChestType otherChestType = otherState.get(ChestBlock.CHEST_TYPE);
        if (otherChestType == ChestType.SINGLE) {
            return Optional.empty();
        }

        // 9. 验证两个箱子的朝向一致
        Direction otherFacing = otherState.get(ChestBlock.FACING);
        if (facing != otherFacing) {
            return Optional.empty();
        }

        // 10. 验证两个箱子类型匹配（一个左一个右）
        boolean typesMatch = (chestType == ChestType.LEFT && otherChestType == ChestType.RIGHT) ||
                (chestType == ChestType.RIGHT && otherChestType == ChestType.LEFT);

        if (!typesMatch) {
            return Optional.empty();
        }

        return Optional.of(otherPos);
    }

    /**
     * 获取右箱子位置（假设当前是左箱子）
     */
    private static BlockPos getRightChestPos(BlockPos pos, Direction facing) {
        return switch (facing) {
            case NORTH -> pos.east();  // 朝北时，右箱子在东边
            case SOUTH -> pos.west();  // 朝南时，右箱子在西边
            case WEST -> pos.north();  // 朝西时，右箱子在北边
            case EAST -> pos.south();  // 朝东时，右箱子在南边
            default -> null;
        };
    }

    /**
     * 获取左箱子位置（假设当前是右箱子）
     */
    private static BlockPos getLeftChestPos(BlockPos pos, Direction facing) {
        return switch (facing) {
            case NORTH -> pos.west();  // 朝北时，左箱子在西边
            case SOUTH -> pos.east();  // 朝南时，左箱子在东边
            case WEST -> pos.south();  // 朝西时，左箱子在南边
            case EAST -> pos.north();  // 朝东时，左箱子在北边
            default -> null;
        };
    }

}
