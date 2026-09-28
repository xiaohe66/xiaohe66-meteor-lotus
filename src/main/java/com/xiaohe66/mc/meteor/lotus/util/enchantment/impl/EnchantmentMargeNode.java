package com.xiaohe66.mc.meteor.lotus.util.enchantment.impl;

import com.xiaohe66.mc.meteor.lotus.util.enchantment.EnchantmentNode;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;

import java.util.HashSet;
import java.util.Set;

/**
 *
 *
 * @author He
 * @since 2026.03.29 00:10
 */
public class EnchantmentMargeNode implements EnchantmentNode {

    private final EnchantmentNode left;
    private final EnchantmentNode right;

    private final int repairCost;
    private final int cost;
    private int costSum = -1;
    private Set<RegistryKey<Enchantment>> allEnchantmentKey;

    public EnchantmentMargeNode(EnchantmentNode left, EnchantmentNode right) {
        this.left = left;
        this.right = right;

        this.repairCost = Math.max(left.getRepairCost(), right.getRepairCost()) + 1;
        this.cost = left.getCost() + right.getCost();
    }

    @Override
    public int getRepairCost() {
        return repairCost;
    }

    @Override
    public int getCost() {
        return cost;
    }

    @Override
    public int getCostSum() {
        // 左边的累计惩罚 + 左边的历史费用 + 右边的累计惩罚 + 右边的历史费用 + 右边的费用
        if (costSum < 0) {
            this.costSum = left.getRepairCost() + left.getCostSum() + right.getRepairCost() + right.getCostSum() + right.getCost();
        }
        return costSum;
    }

    public EnchantmentNode getLeft() {
        return left;
    }

    public EnchantmentNode getRight() {
        return right;
    }

    public Set<RegistryKey<Enchantment>> getAllEnchantmentKey() {
        if (allEnchantmentKey == null) {
            Set<RegistryKey<Enchantment>> all = new HashSet<>();
            if (left instanceof EnchantmentMargeNode margeNode) {
                all.addAll(margeNode.getAllEnchantmentKey());
            } else if (left instanceof EnchantmentBookNode bookNode) {
                all.add(bookNode.getEnchantmentKey());
            }
            if (right instanceof EnchantmentMargeNode margeNode) {
                all.addAll(margeNode.getAllEnchantmentKey());
            } else if (right instanceof EnchantmentBookNode bookNode) {
                all.add(bookNode.getEnchantmentKey());
            }
            this.allEnchantmentKey = all;
        }
        return allEnchantmentKey;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        if (left instanceof EnchantmentMargeNode) {
            stringBuilder.append('(').append(left).append('=').append(left.getRepairCost()).append(',').append(left.getCost()).append(',').append(left.getCostSum()).append(')');
        } else {
            stringBuilder.append(left);
        }
        stringBuilder.append('+');
        if (right instanceof EnchantmentMargeNode) {
            stringBuilder.append('(').append(right).append('=').append(right.getRepairCost()).append(',').append(right.getCost()).append(',').append(right.getCostSum()).append(')');
        } else {
            stringBuilder.append(right).append('(').append(right.getCost()).append(')');
        }
        return stringBuilder.toString();
    }
}
