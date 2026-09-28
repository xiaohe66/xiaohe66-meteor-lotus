package com.xiaohe66.mc.meteor.lotus.util;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.network.packet.s2c.play.*;

/**
 * @author He
 * @since 2025.12.05 15:37
 */
public class TestPacketUtils {

    public static void sendPacketPrint(Packet<?> packet) {
        if (packet instanceof PlayerInteractEntityC2SPacket interactEntityC2SPacket) {
            System.out.println("send packet : " + interactEntityC2SPacket.getPacketType());
        } else if (!(packet instanceof CommonPongC2SPacket
                || packet instanceof KeepAliveC2SPacket
                || packet instanceof PlayerMoveC2SPacket.PositionAndOnGround
                || packet instanceof ClientTickEndC2SPacket)) {
            System.out.println("send : " + packet);
        }
    }

    public static void readPacketPrint(Packet<?> packet) {
        if (packet instanceof ScreenHandlerSlotUpdateS2CPacket packet1) {
            // 更新单个槽位, 在单机时不会发送这个包
            System.out.println("read 更新单个槽位 packet : " + packet1);
        } else if (packet instanceof InventoryS2CPacket packet1) {
            // 更新整个容器
            System.out.println("read 更新整个容器 packet : " + packet1);
        } else if (packet instanceof OpenScreenS2CPacket packet1) {
            System.out.println("read 打开屏幕 packet : " + packet1);
        } else if (packet instanceof ClickSlotC2SPacket packet1) {
            // 这个包在单机时移动格子物品时同步
            System.out.println("read 移动物品 packet : " + packet1);

        } else if (!(packet instanceof ClientTickEndC2SPacket
                || packet instanceof BundleS2CPacket
                || packet instanceof BlockEventS2CPacket
                || packet instanceof BlockUpdateS2CPacket
                || packet instanceof ParticleS2CPacket
                || packet instanceof PlaySoundS2CPacket
                || packet instanceof WorldEventS2CPacket
                || packet instanceof WorldTimeUpdateS2CPacket
                || packet instanceof KeepAliveS2CPacket
                || packet instanceof ChunkDeltaUpdateS2CPacket
                || packet instanceof PlayerListHeaderS2CPacket
                || packet instanceof PlayerListS2CPacket
                || packet instanceof PlayerRemoveS2CPacket
                || packet instanceof ChunkDataS2CPacket
                || packet instanceof CommonPingS2CPacket
                || packet instanceof ItemPickupAnimationS2CPacket
                || packet instanceof HealthUpdateS2CPacket
                || packet instanceof EntitySetHeadYawS2CPacket
                || packet instanceof EntityPositionSyncS2CPacket
                || packet instanceof EntityDamageS2CPacket
                || packet instanceof EntityStatusEffectS2CPacket
                || packet instanceof EntityEquipmentUpdateS2CPacket
                || packet instanceof EntityTrackerUpdateS2CPacket
                || packet instanceof EntityVelocityUpdateS2CPacket
                || packet instanceof EntityStatusS2CPacket
                || packet instanceof EntitiesDestroyS2CPacket
                || packet instanceof EntityAttributesS2CPacket
                || packet instanceof PlayerMoveC2SPacket.PositionAndOnGround
                || packet instanceof UnloadChunkS2CPacket
                || packet instanceof EntityS2CPacket)) {
            System.out.println("read packet : " + packet);
        }
    }
}