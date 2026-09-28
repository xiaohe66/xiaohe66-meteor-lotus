package com.xiaohe66.mc.meteor.lotus.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.Collection;

/**
 * @author He
 * @since 2025.08.05 12:28
 */
public class RaidHelper extends BaseModule {

    private final Setting<Integer> before = sgGeneral.add(new IntSetting.Builder()
            .name("喝药提前量")
            .description("在<袭击之兆>结束的多少tick前喝药")
            .defaultValue(30)
            .sliderRange(0, 32)
            .build());

    private final Setting<Boolean> one = sgGeneral.add(new BoolSetting.Builder()
            .name("保留一瓶")
            .description("当药快喝完时, 至少保留一瓶")
            .defaultValue(true)
            .build());

    public boolean drinking;
    private long drinkStartTime;
    private int slot;

    public RaidHelper() {
        super("袭击助手", "挂机袭击塔使用。定时从背包里拿药、自动喝药后开启杀戮光环", 560, 800);
    }

    @Override
    public void onActivate() {
        setDelay(0);
        drinkStartTime = System.currentTimeMillis();
        if (!needDrink()) {
            stopDrinking();
            enableKillAura();
            setDelay();
        }
        drinking = false;
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onTick(TickEvent.Pre event) {
        if (!checkAndDecrement()) {
            int delayTimer = getDelayTimer();
            if (delayTimer % 20 == 0) {
                int s = delayTimer / 20;
                if (/*s < 6 || */s % 5 == 0) {
                    info("喝药计时 : " + s);
                }
            }
            return;
        }

        if (drinking) {
            if (needDrink()) {
                drink();

            } else {
                info("喝药结束");
                stopDrinking();
                enableKillAura();
                setDelay();
                drinkStartTime = System.currentTimeMillis();
            }

        } else {
            if (!needDrink()) {
                // 当出现卡顿或其他可能导致时序无法对准的时, 在这里循环
                return;
            }

            // 关闭杀戮光环
            disableKillAura();

            slot = findSlot();
            if (slot != -1) {
                info("开始喝药");
                drink();

            } else {
                error("缺少<不详之瓶>");
                toggle();
            }
        }
    }

    private boolean needDrink() {
        return !hasBadOmen() && !hasRaidOmen() && System.currentTimeMillis() - drinkStartTime > 5000;
    }

    private void drink() {
        changeSlot(slot);
        setPressed(true);
        if (!mc.player.isUsingItem()) {
            Utils.rightClick();
        }

        drinking = true;
    }

    private void stopDrinking() {
        setPressed(false);
        drinking = false;
    }

    private void enableKillAura() {
        KillAura killAura = Modules.get().get(KillAura.class);
        if (!killAura.isActive()) {
            killAura.toggle();
        }
    }

    private void disableKillAura() {
        KillAura killAura = Modules.get().get(KillAura.class);
        if (killAura.isActive()) {
            killAura.toggle();
        }
    }

    private void setPressed(boolean pressed) {
        mc.options.useKey.setPressed(pressed);
    }

    private void changeSlot(int slot) {
        InvUtils.swap(slot, false);
        this.slot = slot;
    }

    private int findSlot() {
        // 在快捷栏和背包中寻找不详之瓶
        PlayerInventory inv = mc.player.getInventory();

        // 先检查快捷栏 (0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.getItem() == Items.OMINOUS_BOTTLE && (!one.get() || stack.getCount() > 1)) {
                return i;
            }
        }

        // 检查背包 (9-35)
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.getItem() == Items.OMINOUS_BOTTLE && (!one.get() || stack.getCount() > 1)) {
                // 将药水移动到快捷栏
                int emptySlot = findEmptyHotbarSlot();
                if (emptySlot != -1) {
                    InvUtils.move().from(i).toHotbar(emptySlot);
                    return emptySlot;
                }
            }
        }
        return -1;
    }

    private int findEmptyHotbarSlot() {
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 检查是否有"不详之兆"
     */
    private boolean hasBadOmen() {
        // 获取玩家所有状态效果
        Collection<StatusEffectInstance> effects = mc.player.getStatusEffects();

        // 遍历查找BAD_OMEN效果
        for (StatusEffectInstance effect : effects) {
            RegistryEntry<StatusEffect> effectType = effect.getEffectType();
            if (effectType == StatusEffects.BAD_OMEN) {
                return true;
            }
        }

        return false;
    }

    private boolean hasRaidOmen() {
        // 获取玩家所有状态效果
        Collection<StatusEffectInstance> effects = mc.player.getStatusEffects();

        // 遍历查找BAD_OMEN效果
        for (StatusEffectInstance effect : effects) {
            RegistryEntry<StatusEffect> effectType = effect.getEffectType();


            if (effectType == StatusEffects.RAID_OMEN) {
                // 喝药的需要32tick, 所以起码得保证喝药
                return effect.getDuration() >= before.get();
            }
        }

        return false;
    }

    @Override
    public void onDeactivate() {
        // prevSlot = -1;
        stopDrinking();
        disableKillAura();
    }
}
