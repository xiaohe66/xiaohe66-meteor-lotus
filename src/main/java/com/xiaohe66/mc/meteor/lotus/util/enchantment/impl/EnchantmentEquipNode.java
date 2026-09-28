package com.xiaohe66.mc.meteor.lotus.util.enchantment.impl;

import com.xiaohe66.mc.meteor.lotus.util.enchantment.EnchantmentNode;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;

/**
 *
 *
 * @author He
 * @since 2026.03.29 00:13
 */
public class EnchantmentEquipNode implements EnchantmentNode {

    private final ItemStack itemStack;

    public EnchantmentEquipNode(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public int getRepairCost() {
        return itemStack.getOrDefault(DataComponentTypes.REPAIR_COST, 0);
    }

    @Override
    public int getCost() {
        // 装备不会放到右边进行合并, 所以不会有费用, 只有累计惩罚
        return 0;
    }

    @Override
    public int getCostSum() {
        return 0;
    }

    @Override
    public String toString() {
        return Names.get(itemStack) + "(" + getRepairCost() + ")";
    }
}
