package com.xiaohe66.mc.meteor.lotus.modules.villager;

import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/**
 * @author He
 * @since 2025.06.08 15:02
 */
public class VillagerEntityWarp {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private final VillagerType villagerType;

    private final UUID uuid;

    private final BlockPos operatePos;

    private final Vec3d operatePosCenter;

    /**
     * 村民朝向（刷附魔书用）
     */
    private final Direction facing;

    /**
     * 工作方块位置，如讲台位置（刷附魔书用）
     */
    private final BlockPos workPos;

    /**
     * 最后交易时间, MC时间
     */
    private long lastTradeTime;

    private long lastFailTime;

    public VillagerEntityWarp(VillagerType villagerType, UUID uuid, BlockPos operatePos) {
        this(villagerType, uuid, operatePos, null, null);
    }

    public VillagerEntityWarp(VillagerType villagerType, UUID uuid, BlockPos operatePos, Direction facing, BlockPos workPos) {
        this.villagerType = villagerType;
        this.uuid = uuid;
        this.operatePos = operatePos;
        this.operatePosCenter = operatePos.toCenterPos();
        this.facing = facing;
        this.workPos = workPos;
    }

    public UUID getUuid() {
        return uuid;
    }

    public VillagerEntity getVillager() {
        try {
            return (VillagerEntity) mc.world.getEntityLookup().get(uuid);

        } catch (Exception e) {
            ChatUtils.error("获取村民状态异常 : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public VillagerType getVillagerType() {
        return villagerType;
    }

    public BlockPos getOperatePos() {
        return operatePos;
    }

    public Vec3d getOperatePosCenter() {
        return operatePosCenter;
    }

    public long getLastTradeTime() {
        return lastTradeTime;
    }

    public void setLastTradeTime(long lastTradeTime) {
        this.lastTradeTime = lastTradeTime;
    }

    public long getLastFailTime() {
        return lastFailTime;
    }

    public void setLastFailTime(long lastFailTime) {
        this.lastFailTime = lastFailTime;
    }

    public long getTimeOfDay() {
        return lastTradeTime % 24000;
    }

    public long getDay() {
        return lastTradeTime / 24000;
    }

    public Direction getFacing() {
        return facing;
    }

    public BlockPos getWorkPos() {
        return workPos;
    }
}
