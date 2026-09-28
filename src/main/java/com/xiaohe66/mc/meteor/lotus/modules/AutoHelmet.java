package com.xiaohe66.mc.meteor.lotus.modules;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnchantmentListSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.Set;

/**
 * @author He
 * @since 2025.05.19 22:48
 */
public class AutoHelmet extends BaseModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> distance = sgGeneral.add(new IntSetting.Builder()
            .name("检测距离")
            .description("检测猪灵的距离。")
            .defaultValue(10)
            .min(1)
            .sliderMax(40)
            .build());

    private final Setting<Integer> minTime = sgGeneral.add(new IntSetting.Builder()
            .name("检测时间-tick")
            .description("检测持续多少时间后开始切换头盔")
            .defaultValue(5)
            .min(1)
            .sliderMax(64)
            .build());

    private final Setting<Boolean> ignoreEmpty = sgGeneral.add(new BoolSetting.Builder()
            .name("忽略空")
            .description("没有穿戴头盔时, 不做切换")
            .defaultValue(true)
            .build());

    private final Setting<Set<RegistryKey<Enchantment>>> avoidedEnchantments = sgGeneral.add(new EnchantmentListSetting.Builder()
            .name("避免的附魔")
            .description("应该避免的附魔.")
            .defaultValue(Enchantments.BINDING_CURSE, Enchantments.FROST_WALKER)
            .build());


    private final Object2IntMap<RegistryEntry<Enchantment>> enchantments = new Object2IntOpenHashMap<>();
    private long lastPiglinTime;
    private boolean timing;

    public AutoHelmet() {
        super("自动头盔", "附近有猪灵时自动换上金头盔", 20);
    }

    @Override
    public void onActivate() {
        timing = false;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (!isReady()) {
            return;
        }

        if (!checkAndDecrement()) {
            return;
        }

        ItemStack currentItemStack = mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD);

        // 绑定诅咒判断
        Utils.getEnchantments(currentItemStack, enchantments);
        if (enchantments.containsKey(Enchantments.BINDING_CURSE)) {
            return;
        }

        boolean needGold = needGold();
        Item currentItem = currentItemStack.getItem();
        if (needGold) {
            if (currentItem == Items.GOLDEN_HELMET) {
                return;
            }
            if (!timing) {
                timing = true;
                lastPiglinTime = mcTime();
                return;
            }
            ItemStack nextStack = nextPlayerStack(itemStack -> itemStack.getItem() == Items.GOLDEN_HELMET && !hasAvoidedEnchantment());
            if (nextStack.isEmpty()) {
                // 保证有盔甲穿, 没有金的穿钻石的也好
                nextStack = nextPlayerStack(itemStack -> (itemStack.getItem() == Items.DIAMOND_HELMET
                        || itemStack.getItem() == Items.NETHERITE_HELMET)
                        && !hasAvoidedEnchantment());
            }
            if (!nextStack.isEmpty()) {
                if (currentItem != nextStack.getItem() && lastPiglinTime + minTime.get() < mcTime()) {
                    // 未穿戴时, 功能禁用, 用于手动脱下装备的时刻
                    if (ignoreEmpty.get() && currentItem == Items.AIR) {
                        return;
                    }
                    swap(getCurPlayerSlot(), 3);
                    setDelay();
                }
            }
        } else {
            timing = false;
            if (currentItem == Items.DIAMOND_HELMET || currentItem == Items.NETHERITE_HELMET) {
                return;
            }
            ItemStack nextStack = nextPlayerStack(itemStack -> (itemStack.getItem() == Items.DIAMOND_HELMET
                    || itemStack.getItem() == Items.NETHERITE_HELMET)
                    && !hasAvoidedEnchantment());

            if (!nextStack.isEmpty()) {
                if (currentItem != nextStack.getItem()) {
                    // 未穿戴时, 功能禁用, 用于手动脱下装备的时刻
                    if (ignoreEmpty.get() && currentItem == Items.AIR) {
                        return;
                    }
                    swap(getCurPlayerSlot(), 3);
                    setDelay();
                }
            }
        }
    }

    private boolean needGold() {

        boolean hasPiglin = false;

        for (Entity entity : mc.world.getEntities()) {

            if (entity == mc.player) {
                continue;
            }

            // 玩家
            if (entity instanceof PlayerEntity) {
                if (isWithinDistance(entity, mc.player, distance.get())) {
                    return false;
                }
            }
            // 猪灵
            else if (entity instanceof PiglinEntity) {
                if (isWithinDistance(entity, mc.player, distance.get())) {
                    hasPiglin = true;
                }
            }
        }

        return hasPiglin;
    }

    // 高效判断是否在指定范围内（推荐使用）
    public static boolean isWithinDistance(Entity a, Entity b, double distance) {
        return a.squaredDistanceTo(b) <= distance * distance;
    }

    private boolean hasAvoidedEnchantment() {
        for (RegistryEntry<Enchantment> enchantment : enchantments.keySet()) {
            if (enchantment.matches(avoidedEnchantments.get()::contains)) {
                return true;
            }
        }

        return false;
    }

    private void swap(int from, int armorSlotId) {
        InvUtils.move().from(from).toArmor(armorSlotId);
        // InvUtils.click().slot(from);
        info("切换盔甲");
    }
}
