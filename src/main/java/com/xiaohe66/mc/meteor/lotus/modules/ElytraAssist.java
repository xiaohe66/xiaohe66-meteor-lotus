package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;

/**
 * 鞘翅辅助模块
 * 提供自动起飞等鞘翅辅助功能
 *
 * @author He
 * @since 2025.04.10
 */
public class ElytraAssist extends StepModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAutoTakeoff = settings.createGroup("自动起飞");

    //--------------------General--------------------//
    private final Setting<Boolean> autoTakeoff = sgAutoTakeoff.add(new BoolSetting.Builder()
            .name("自动起飞")
            .description("当不在飞行状态超过一定时间时，自动穿鞘翅并使用烟花起飞")
            .defaultValue(true)
            .build());

    private final Setting<Integer> takeoffDelay = sgAutoTakeoff.add(new IntSetting.Builder()
            .name("起飞延迟")
            .description("不在飞行状态超过多少tick后触发自动起飞")
            .defaultValue(20)
            .min(1)
            .sliderMax(100)
            .visible(autoTakeoff::get)
            .build());

    private final Setting<Integer> minDurability = sgAutoTakeoff.add(new IntSetting.Builder()
            .name("最小耐久")
            .description("鞘翅耐久低于此值时不自动装备")
            .defaultValue(5)
            .min(1)
            .sliderMax(100)
            .visible(autoTakeoff::get)
            .build());

    private final Setting<Double> takeoffPitch = sgAutoTakeoff.add(new DoubleSetting.Builder()
            .name("起飞角度")
            .description("起飞时的俯仰角，负数为向上，0为水平，正数为向下")
            .defaultValue(-45)
            .min(-90.0)
            .max(90.0)
            .sliderMin(-90.0)
            .sliderMax(90.0)
            .visible(autoTakeoff::get)
            .build());

    private long lastGlidingTime = 0;
    private long lastJumpTime = 0;

    public ElytraAssist() {
        super("鞘翅辅助", "提供自动起飞等鞘翅辅助功能");

        // 注册步骤处理函数
        addStep(Steps.MONITOR, this::monitor);
        addStep(Steps.EQUIP_ELYTRA, this::equipElytra);
        addStep(Steps.SWITCH_FIREWORK, this::switchFirework);
        addStep(Steps.DEPLOY_ELYTRA, this::deployElytra);
        addStep(Steps.USE_FIREWORK, this::useFirework);
    }

    @Override
    public void onActivate() {
        super.onActivate();
        step = Steps.MONITOR;
        lastGlidingTime = mcTime();
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
    }

    /**
     * 监控状态 - 检测是否需要自动起飞
     */
    private void monitor() {
        // 检测是否在飞行
        if (mc.player.isGliding()) {
            lastGlidingTime = mcTime();
            return;
        }

        // 检查是否需要自动起飞
        if (autoTakeoff.get()) {
            long timeSinceLastGlide = mcTime() - lastGlidingTime;
            if (timeSinceLastGlide >= takeoffDelay.get()) {
                step = Steps.EQUIP_ELYTRA;
            }
        }
    }

    /**
     * 装备鞘翅
     */
    private void equipElytra() {
        // 检查当前是否已装备鞘翅
        ItemStack chestItem = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (chestItem.getItem() == Items.ELYTRA) {
            // 已经装备了鞘翅，检查耐久
            if (chestItem.getDamage() < chestItem.getMaxDamage() - minDurability.get()) {
                // 已经装备且耐久足够，重置重试计数，切换到烟花
                step = Steps.SWITCH_FIREWORK;
                return;
            }
            // 耐久不足，需要更换
            info("当前鞘翅耐久不足，尝试更换");
        }

        // 在背包中查找耐久足够的鞘翅
        int elytraSlot = findElytraWithDurability();
        if (elytraSlot == -1) {
            warning("未找到耐久足够的鞘翅，无法自动起飞");
            lastGlidingTime = mcTime(); // 重置计时，避免重复警告
            step = Steps.MONITOR;
            return;
        }

        // 装备鞘翅
        InvUtils.move().from(elytraSlot).toArmor(2); // 2 是胸甲槽位
        // info("自动装备鞘翅");
        // 装备后等待2tick让客户端/服务器同步
        step = Steps.SWITCH_FIREWORK;
    }

    /**
     * 切换烟花火箭到主手
     */
    private void switchFirework() {
        // 查找烟花火箭
        int fireworkSlot = findFireworkRocket();
        if (fireworkSlot == -1) {
            warning("未找到烟花火箭，无法自动起飞");
            step = Steps.MONITOR;
            return;
        }

        // 切换到烟花火箭
        if (HeInvUtils.isHotbar(fireworkSlot)) {
            HeInvUtils.swapToSlot(fireworkSlot);
        } else {
            // 将背包中的烟花切换到主手
            HeInvUtils.swapMainHand(fireworkSlot);
        }
        // info("切换烟花火箭到主手");
        step = Steps.DEPLOY_ELYTRA;
    }

    /**
     * 展开鞘翅
     */
    private void deployElytra() {
        // 检查鞘翅是否已装备
        ItemStack chestItem = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (chestItem.getItem() != Items.ELYTRA) {
            warning("鞘翅装备失败，取消自动起飞");
            step = Steps.MONITOR;
            return;
        }

        // 如果已经在飞行状态，直接进入使用烟花步骤
        // 注意：此时烟花应该已经在主手了（由 SWITCH_FIREWORK 步骤完成）
        if (mc.player.isGliding()) {
            step = Steps.USE_FIREWORK;
            return;
        }

        // 判断玩家是否在地面上
        if (mc.player.isOnGround()) {
            // info("跳");
            // 在地面上，需要先跳跃离地
            tryJump();
            return;
        }

        // 检查下落距离 - Minecraft 要求 fallDistance > 0 才能展开鞘翅
        // 刚取消鞘翅后 fallDistance 会被重置，需要等待下落
        if (mc.player.fallDistance <= 0.0f) {
            // info("等待下落...");
            // 尝试稍微向下看并移动来产生下落
            return;
        }

        // info("展开");
        tryJump();
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));

        step = Steps.USE_FIREWORK;
    }

    /**
     * 使用烟花火箭
     */
    private void useFirework() {
        // 检查鞘翅是否已展开
        if (!mc.player.isGliding()) {
            // 鞘翅还没展开，回到 DEPLOY_ELYTRA 重试
            info("鞘翅未展开，继续尝试");
            deployElytra();
            return;
        }

        // 设置起飞角度
        float targetPitch = takeoffPitch.get().floatValue();
        float currentPitch = mc.player.getPitch();
        if (Math.abs(currentPitch - targetPitch) > 1.0f) {
            // 需要调整角度
            // 旋转完成后使用烟花
            Rotations.rotate(mc.player.getYaw(), targetPitch, this::useFireworkInternal);
        } else {
            useFireworkInternal();
        }
    }

    /**
     * 内部方法：使用烟花火箭（角度已调整好）
     */
    private void useFireworkInternal() {
        // 使用烟花火箭（已经在主手）
        if (mc.interactionManager != null) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        }
        info("自动起飞完成，角度：" + String.format("%.1f", mc.player.getPitch()));

        // 重置状态
        lastGlidingTime = mcTime();
        delayNext(Steps.MONITOR);
    }

    private void tryJump() {
        long curTime = mcTime();
        if (curTime - lastJumpTime > 20) {
            lastJumpTime = curTime;
            mc.player.jump();
        }
    }

    /**
     * 在背包中查找耐久足够的鞘翅
     */
    private int findElytraWithDurability() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.ELYTRA) {
                // 检查耐久
                int remainingDurability = stack.getMaxDamage() - stack.getDamage();
                if (remainingDurability > minDurability.get()) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 查找烟花火箭
     */
    private int findFireworkRocket() {
        // 优先查找快捷栏
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.FIREWORK_ROCKET) {
                return i;
            }
        }
        // 查找整个背包
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.FIREWORK_ROCKET) {
                return i;
            }
        }
        return -1;
    }
}
