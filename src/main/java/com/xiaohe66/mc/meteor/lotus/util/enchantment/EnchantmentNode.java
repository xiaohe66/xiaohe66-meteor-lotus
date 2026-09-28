package com.xiaohe66.mc.meteor.lotus.util.enchantment;

import org.jetbrains.annotations.NotNull;

/**
 *
 *
 * @author He
 * @since 2026.03.29 00:08
 */
public interface EnchantmentNode extends Comparable<EnchantmentNode> {

    /**
     * 累计惩罚
     */
    int getRepairCost();

    /**
     * 自己的费用, 不算累计惩罚
     */
    int getCost();

    /**
     * 累计费用, 这个步骤执行完后, 累计花了多少
     */
    int getCostSum();
    
    @Override
    default int compareTo(@NotNull EnchantmentNode node) {
        return Integer.compare(node.getCost(), this.getCost());
    }
}
