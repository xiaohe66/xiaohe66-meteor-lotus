package com.xiaohe66.mc.meteor.lotus.modules.villager;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnchantmentListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author He
 * @since 2026.01.03 00:02
 */
public class VillagerSettingWarp {

    private final Setting<Boolean> openSetting;
    private final SettingGroup itemGroup;
    private final Setting<VillagerType> typeSetting;
    private final Setting<List<Item>> buyItemSetting;
    private final Setting<Integer> supplyQty;
    private final Setting<Integer> maxMoenySetting;
    private final Setting<Set<RegistryKey<Enchantment>>> buyEnchantmentSetting;
    // private final Setting<Integer> minMoneySetting;

    public VillagerSettingWarp(Settings settings,
                               SettingGroup sgGeneral,
                               String name,
                               VillagerType defaultType,
                               int defaultSupplyQty) {

        openSetting = sgGeneral.add(new BoolSetting.Builder()
                .name("交易_" + name)
                .defaultValue(false)
                .build());

        itemGroup = settings.createGroup("交易_" + name);

        typeSetting = itemGroup.add(new EnumSetting.Builder<VillagerType>()
                .name(name + "_村民类型")
                .defaultValue(defaultType)
                .visible(() -> openSetting.get() && defaultType == VillagerType.石匠)
                .build());

        buyItemSetting = itemGroup.add(new ItemListSetting.Builder()
                .name(name + "_交易")
                .filter(item -> typeSetting.get().getCanVillagerItemSet().contains(item))
                .defaultValue(defaultType.getDefaultVillagerItemList().toArray(new Item[0]))
                .visible(openSetting::get)
                .build());

        buyEnchantmentSetting = itemGroup.add(new EnchantmentListSetting.Builder()
                .name(name + "_附魔书")
                .visible(() -> openSetting.get() && typeSetting.get() == VillagerType.图书管理员)
                .build());

        supplyQty = itemGroup.add(new IntSetting.Builder()
                .name(name + "_补给数量(组)")
                .description("每次补给的数量")
                .range(1, 27)
                .sliderMax(27)
                .defaultValue(defaultSupplyQty)
                .visible(openSetting::get)
                .build());

        maxMoenySetting = itemGroup.add(new IntSetting.Builder()
                .name(name + "_价格上限")
                .range(1, 64)
                .sliderRange(1, 64)
                .defaultValue(1)
                .visible(openSetting::get)
                .build());

        /*minMoneySetting = sgGeneral.add(new IntSetting.Builder()
                .name("最少绿宝石量(组)_" + name)
                .defaultValue(6)
                .sliderRange(1, 12)
                .visible(() -> openSetting.get() && typeSetting.get() == VillagerType.图书管理员)
                .build());*/
    }

    public VillagerSettingWarp(SettingGroup itemGroup,
                               Setting<Boolean> openSetting,
                               Setting<VillagerType> typeSetting,
                               Setting<List<Item>> buyItemSetting,
                               Setting<Integer> supplyQty,
                               Setting<Integer> maxMoenySetting,
                               Setting<Set<RegistryKey<Enchantment>>> buyEnchantmentSetting
            /*Setting<Integer> minMoneySetting*/) {
        this.itemGroup = itemGroup;
        this.openSetting = openSetting;
        this.typeSetting = typeSetting;
        this.buyItemSetting = buyItemSetting;
        this.supplyQty = supplyQty;
        this.maxMoenySetting = maxMoenySetting;
        this.buyEnchantmentSetting = buyEnchantmentSetting;
        // this.minMoneySetting = minMoneySetting;
    }

    public boolean needBuyEnchantment() {
        return isOpen() && !getBuyEnchantment().isEmpty();
    }

    public boolean isOpen() {
        return openSetting.get();
    }

    public VillagerType getType() {
        return typeSetting.get();
    }

    public List<Item> getBuyItem() {
        return buyItemSetting.get();
    }

    public int getSupplyQty() {
        return supplyQty.get();
    }

    public int getMaxMoney() {
        return maxMoenySetting.get();
    }

    public Set<RegistryKey<Enchantment>> getBuyEnchantment() {
        if (getType() == VillagerType.图书管理员) {
            return buyEnchantmentSetting.get();
        }
        return Collections.emptySet();
    }

    /*public int getMinMoney() {
        return minMoneySetting.get();
    }*/
}