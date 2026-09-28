package com.xiaohe66.mc.meteor.lotus.util.enchantment.impl;

import com.xiaohe66.mc.meteor.lotus.util.EnchantmentUtils;
import com.xiaohe66.mc.meteor.lotus.util.enchantment.EnchantmentNode;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;

/**
 *
 *
 * @author He
 * @since 2026.03.29 00:11
 */
public class EnchantmentBookNode implements EnchantmentNode {

    private final RegistryKey<Enchantment> registryKey;
    private final int cost;

    public EnchantmentBookNode(RegistryKey<Enchantment> registryKey) {
        this.registryKey = registryKey;
        this.cost = EnchantmentUtils.getCost(registryKey);
    }

    @Override
    public int getRepairCost() {
        return 0;
    }

    @Override
    public int getCost() {
        return cost;
    }

    @Override
    public int getCostSum() {
        return 0;
    }

    public RegistryKey<Enchantment> getEnchantmentKey() {
        return registryKey;
    }

    @Override
    public String toString() {
        return Names.get(registryKey);
    }
}
