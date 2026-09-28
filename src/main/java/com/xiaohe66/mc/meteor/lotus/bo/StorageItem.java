package com.xiaohe66.mc.meteor.lotus.bo;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 存储物品包装类，支持单物品或多物品组合
 *
 * @author He
 * @since 2025.08.26 22:28
 */
public class StorageItem {

    private static final Map<ItemBo, StorageItem> itemCache = new HashMap<>();
    private static final Map<Set<ItemBo>, StorageItem> itemsCache = new HashMap<>();

    private final Set<ItemBo> items;

    private StorageItem(Set<ItemBo> items) {
        this.items = Set.copyOf(items);
    }

    public Item getItem() {
        return items.stream().findFirst().map(ItemBo::getItem).orElse(Items.AIR);
    }

    public Set<ItemBo> getItems() {
        return items;
    }

    public static StorageItem valueOf(ItemBo itemBo) {
        return itemCache.computeIfAbsent(itemBo, k -> new StorageItem(Collections.singleton(itemBo)));
    }

    public static StorageItem valueOf(Item item) {
        return valueOf(new ItemBo(item));
    }

    public static StorageItem valueOf(Item... items) {
        Set<ItemBo> itemBoSet = Arrays.stream(items).map(ItemBo::new).collect(Collectors.toSet());
        if (itemBoSet.size() == 1) {
            return valueOf(itemBoSet.iterator().next());
        } else {
            return itemsCache.computeIfAbsent(itemBoSet, k -> new StorageItem(itemBoSet));
        }
    }

    public static StorageItem valueOf(ItemBo... itemBos) {
        Set<ItemBo> itemBoSet = Set.of(itemBos);
        if (itemBoSet.size() == 1) {
            return valueOf(itemBoSet.iterator().next());
        } else {
            return itemsCache.computeIfAbsent(itemBoSet, k -> new StorageItem(itemBoSet));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        StorageItem that = (StorageItem) o;
        return items.equals(that.items);
    }

    @Override
    public int hashCode() {
        return items.hashCode();
    }
}
