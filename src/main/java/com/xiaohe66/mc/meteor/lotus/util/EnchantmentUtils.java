package com.xiaohe66.mc.meteor.lotus.util;

import com.xiaohe66.mc.meteor.lotus.util.enchantment.EnchantmentNode;
import com.xiaohe66.mc.meteor.lotus.util.enchantment.impl.EnchantmentBookNode;
import com.xiaohe66.mc.meteor.lotus.util.enchantment.impl.EnchantmentEquipNode;
import com.xiaohe66.mc.meteor.lotus.util.enchantment.impl.EnchantmentMargeNode;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author He
 * @since 2025.06.13 12:03
 */
public class EnchantmentUtils {

    public static final MinecraftClient mc = MinecraftClient.getInstance();

    private EnchantmentUtils() {

    }

    public static Set<RegistryKey<Enchantment>> getEnchantment(ItemStack itemStack) {
        return getEnchantment(itemStack, false);
    }

    public static Set<RegistryKey<Enchantment>> getEnchantment(ItemStack itemStack, boolean isMaxLevel) {

        ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(itemStack);

        if (enchantments.isEmpty()) {
            return Collections.emptySet();
        }

        Set<RegistryKey<Enchantment>> list = new HashSet<>(enchantments.getSize());

        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments.getEnchantmentEntries()) {
            RegistryEntry<Enchantment> registryEntry = entry.getKey();
            Optional<RegistryKey<Enchantment>> enchantmentKeyOptional = registryEntry.getKey();
            if (enchantmentKeyOptional.isPresent()) {
                if (!isMaxLevel || entry.getIntValue() >= registryEntry.value().getMaxLevel()) {
                    list.add(enchantmentKeyOptional.get());
                }
            }
        }

        return list;
    }

    public static RegistryKey<Enchantment> getEnchantmentOne(ItemStack itemStack) {
        ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(itemStack);
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> enchantmentEntry : enchantments.getEnchantmentEntries()) {
            RegistryEntry<Enchantment> registryEntry = enchantmentEntry.getKey();
            Optional<RegistryKey<Enchantment>> enchantmentKeyOptional = registryEntry.getKey();
            if (enchantmentKeyOptional.isPresent()) {
                RegistryKey<Enchantment> enchantmentRegistryKey = enchantmentKeyOptional.get();

                Enchantment enchantment = registryEntry.value();
                if (enchantmentEntry.getIntValue() == enchantment.getMaxLevel()) {
                    return enchantmentRegistryKey;
                }
            }
        }
        return null;
    }

    public static boolean existAll(List<RegistryKey<Enchantment>> needEnchantmentList, ItemStack itemStack) {

        Set<RegistryKey<Enchantment>> existSet = getEnchantment(itemStack);

        return existAll(needEnchantmentList, existSet);

    }

    public static boolean existAll(List<RegistryKey<Enchantment>> needEnchantmentList, Set<RegistryKey<Enchantment>> checkSet) {

        for (RegistryKey<Enchantment> registryKey : needEnchantmentList) {
            if (!checkSet.contains(registryKey)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 拿到装备下一步的操作
     */
    public static EnchantmentMargeNode bestStepSimple(ItemStack equipItemStack, Set<RegistryKey<Enchantment>> missEnchantmentSet) {
        EnchantmentNode node = bestStep(equipItemStack, missEnchantmentSet);
        while (true) {
            if (node instanceof EnchantmentMargeNode margeNode) {
                if (margeNode.getLeft() instanceof EnchantmentEquipNode equipNode) {
                    return margeNode;
                } else {
                    node = margeNode.getLeft();
                }
            } else {
                throw new IllegalStateException("不应该运行到这里");
            }
        }
    }

    public static EnchantmentNode bestStep(ItemStack equipItemStack, Set<RegistryKey<Enchantment>> missEnchantmentSet) {
        EnchantmentEquipNode equipNode = new EnchantmentEquipNode(equipItemStack);

        Set<EnchantmentNode> bookNodeSet = missEnchantmentSet.stream()
                .map(EnchantmentBookNode::new)
                .collect(Collectors.toSet());

        return bestStep(equipNode, bookNodeSet);
    }

    public static EnchantmentNode bestStep(EnchantmentNode originEquipNode, Set<EnchantmentNode> bookNodeSet) {
        return bestStep(originEquipNode, bookNodeSet, false);
    }

    /**
     * 计算合并顺序
     * 将剩余的书按费用排序, 然后进行首尾合并, 用这种方法合并出与装备相同惩罚的书
     */
    public static EnchantmentNode bestStep(EnchantmentNode originEquipNode, Set<EnchantmentNode> bookNodeSet, boolean fast) {
        if (bookNodeSet.isEmpty()) {
            return null;
        }

        List<EnchantmentNode> bookList = new LinkedList<>(bookNodeSet);
        bookList.sort(null);

        // 分组管理：repairCost -> 节点列表
        Map<Integer, List<EnchantmentNode>> groups = new HashMap<>();
        groups.put(0, bookList);

        int sup = bookList.size();

        // 当前装备状态
        EnchantmentNode equipNode = originEquipNode;
        while (sup > 0) {
            List<EnchantmentNode> group = groups.computeIfAbsent(equipNode.getRepairCost(), k -> new LinkedList<>());
            if (!group.isEmpty()) {
                // 装备 合并 书
                EnchantmentNode right = group.removeFirst();
                equipNode = new EnchantmentMargeNode(equipNode, right);
                sup--;
                if (fast) {
                    return equipNode;
                }
                continue;
            }
            // 将低等级的合并
            int margeQty = 0;
            for (int i = 0; i < equipNode.getRepairCost(); i++) {
                List<EnchantmentNode> lessGroup = groups.computeIfAbsent(i, k -> new LinkedList<>());
                List<EnchantmentNode> nextGroup = groups.computeIfAbsent(equipNode.getRepairCost(), k -> new LinkedList<>());
                while (lessGroup.size() >= 2) {
                    EnchantmentNode left = lessGroup.removeFirst();
                    EnchantmentNode right = lessGroup.removeLast();
                    EnchantmentNode newNode = new EnchantmentMargeNode(left, right);
                    margeQty++;
                    nextGroup.add(newNode);
                }
                // 将下一组重新排序
                nextGroup.sort(null);
            }

            if (margeQty > 0) {
                sup -= margeQty;
            } else {
                // 无法再合并了, 可能存在无法完美合并的
                break;
            }
        }

        // 检查是否有未完美合并的
        int repairCost = 0;
        EnchantmentNode right = null;
        while (sup > 1 && repairCost < 10) {
            List<EnchantmentNode> group = groups.get(repairCost);
            // 当低等级的合并到高等级后, 一个组里面的数量就可能会超过1个, 所以要用while
            while (!group.isEmpty()) {
                if (right == null) {
                    right = group.removeLast();
                } else {
                    EnchantmentNode left = group.removeFirst();
                    right = new EnchantmentMargeNode(left, right);
                    sup--;
                }
            }
            repairCost++;
        }

        if (sup > 0) {
            if (right == null) {
                for (Map.Entry<Integer, List<EnchantmentNode>> entry : groups.entrySet()) {
                    if (!entry.getValue().isEmpty()) {
                        right = entry.getValue().removeFirst();
                        // 只会有一本书
                        break;
                    }
                }
                if (right == null) {
                    throw new IllegalStateException("未知状态");
                }
            }
            equipNode = new EnchantmentMargeNode(equipNode, right);
        }

        return equipNode;
    }

    /**
     * 获取物品/附魔书的累计惩罚
     */
    public static int getRepairCost(ItemStack itemStack) {
        return itemStack.getOrDefault(DataComponentTypes.REPAIR_COST, 0);
    }

    /**
     * 获取附魔的费用乘数（书需要除以2）
     */
    public static int getAnvilCost(RegistryKey<Enchantment> registryKey) {
        return getEnchantmentInstance(registryKey).map(Enchantment::getAnvilCost).orElseThrow();
    }

    /**
     * 获取附魔的最高等级
     */
    public static int getMaxLevel(RegistryKey<Enchantment> registryKey) {
        return getEnchantmentInstance(registryKey).map(Enchantment::getMaxLevel).orElseThrow();
    }

    /**
     * 获取附魔书的费用(最高等级)
     */
    public static int getCost(RegistryKey<Enchantment> registryKey) {
        Enchantment enchantment = getEnchantmentInstance(registryKey).orElseThrow();
        return Math.max(1, enchantment.getAnvilCost() / 2) * enchantment.getMaxLevel();
    }

    /**
     * 计算附魔集合的总价值
     */
    public static int getCost(Set<RegistryKey<Enchantment>> enchantments) {
        int total = 0;
        for (RegistryKey<Enchantment> enchantment : enchantments) {
            total += getCost(enchantment);
        }
        return total;
    }

    public static Optional<Enchantment> getEnchantmentInstance(RegistryKey<Enchantment> registryKey) {
        return mc.world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT).map(item -> item.get(registryKey));
    }

    /**
     * 从累积惩罚计算操作数
     *
     * @param penalty 累积惩罚
     * @return 操作数
     */
    public static int penaltyToWork(int penalty) {
        if (penalty <= 0) return 0;
        // 惩罚 = 2^work - 1，反推 work
        return 32 - Integer.numberOfLeadingZeros(penalty);
    }

    /**
     * 从操作数计算累积惩罚
     *
     * @param work 操作数
     * @return 累积惩罚
     */
    public static int workToPenalty(int work) {
        if (work <= 0) return 0;
        return (1 << work) - 1;
    }

}
