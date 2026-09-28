package com.xiaohe66.mc.meteor.lotus.util;

import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import meteordevelopment.meteorclient.mixin.ContainerComponentAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author He
 * @since 2025.06.25 13:16
 */
public class ShulkerBoxReader implements Iterable<ItemStack> {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private final ItemStack boxItemStack;

    private ItemStack[] cacheItemStackArr;
    private boolean empty = true;

    private List<ItemStack> condensedList;
    private Set<ItemBo> itemBoSet;

    public ShulkerBoxReader(ItemStack boxItemStack) {

        if (boxItemStack == null || !HeItemUtils.isShulkerBox(boxItemStack.getItem())) {
            throw new IllegalArgumentException("ItemStack is not a ShulkerBox");
        }

        this.boxItemStack = boxItemStack;
    }

    public boolean isEmpty() {
        readItemStackArr();
        return empty;
    }

    public boolean hasItem(ItemBo itemBo) {
        for (ItemStack itemStack : readItemStackArr()) {
            if (new ItemBo(itemStack).equals(itemBo)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasItemAll(Set<ItemBo> itemBos) {
        if (itemBoSet == null) {
            List<ItemStack> condensed = getCondensed();
            itemBoSet = condensed.stream().map(ItemBo::new).collect(Collectors.toSet());
        }

        for (ItemBo itemBo : itemBos) {
            if (!itemBoSet.contains(itemBo)) {
                return false;
            }
        }

        return true;
    }

    public ItemBo getFirstItemBo() {
        for (ItemStack itemStack : readItemStackArr()) {
            if (!itemStack.isEmpty()) {
                return new ItemBo(itemStack);
            }
        }
        return new ItemBo(Items.AIR);
    }

    /**
     * @deprecated 请使用 {@link #getFirstItemBo()}
     */
    @Deprecated
    public Item getFirstItem() {
        for (ItemStack itemStack : readItemStackArr()) {
            if (!itemStack.isEmpty()) {
                return itemStack.getItem();
            }
        }
        return Items.AIR;
    }

    public boolean isSameItem() {
        if (isEmpty()) {
            return false;
        }
        Map<ItemBo, Integer> itemQtyMap = readItemQtyMap();
        return itemQtyMap.size() == 1;
    }

    public Set<ItemBo> getItemBoSet() {
        return Arrays.stream(readItemStackArr())
                .map(ItemBo::new)
                .collect(Collectors.toSet());
    }

    /**
     * @deprecated 请使用 {@link #getItemBoSet()}
     */
    @Deprecated
    public Set<Item> getItemSet() {
        return Arrays.stream(readItemStackArr())
                .map(ItemStack::getItem)
                .collect(Collectors.toSet());
    }

    public ItemStack getMaximumItem() {
        Map<ItemBo, Integer> itemQtyMap = readItemQtyMap();

        double max = 0;
        Map.Entry<ItemBo, Integer> maxentry = null;
        for (Map.Entry<ItemBo, Integer> entry : itemQtyMap.entrySet()) {
            // 按一组的比例来判断数量
            ItemBo newItemBo = entry.getKey();
            double value = entry.getValue().doubleValue() / newItemBo.getItem().getMaxCount();
            if (value > max) {
                max = value;
                maxentry = entry;
            }
        }
        if (maxentry == null) {
            return Items.AIR.getDefaultStack();
        }

        // 找到对应的原始 ItemStack 以保留 component
        ItemBo targetItemBo = maxentry.getKey();
        for (ItemStack itemStack : readItemStackArr()) {
            if (!itemStack.isEmpty() && new ItemBo(itemStack).equals(targetItemBo)) {
                ItemStack result = itemStack.copy();
                result.setCount(maxentry.getValue());
                return result;
            }
        }

        return Items.AIR.getDefaultStack();
    }

    public List<ItemStack> getCondensed() {
        if (condensedList == null) {
            ItemStack[] itemStackArr = readItemStackArr();
            Map<String, ItemStack> map = new LinkedHashMap<>();
            for (ItemStack itemStack : itemStackArr) {
                if (itemStack.isEmpty()) {
                    continue;
                }

                ComponentMap nbtElement = itemStack.getComponents();
                String key = itemStack + "_" + nbtElement.toString();

                ItemStack mapItemStack = map.computeIfAbsent(key, k -> itemStack.copyAndEmpty());
                int count = mapItemStack.getCount() + itemStack.getCount();
                mapItemStack.setCount(count);
            }

            this.condensedList = new ArrayList<>(map.values());
        }

        return condensedList;
    }

    private Map<ItemBo, Integer> readItemQtyMap() {

        ItemStack[] itemStackArr = readItemStackArr();

        Map<ItemBo, Integer> map = new LinkedHashMap<>();
        for (ItemStack itemStack : itemStackArr) {
            if (itemStack.isEmpty()) {
                continue;
            }
            ItemBo itemBo = new ItemBo(itemStack);
            Integer qty = map.computeIfAbsent(itemBo, k -> 0);
            map.put(itemBo, qty + itemStack.getCount());
        }

        return map;
    }

    private ItemStack[] readItemStackArr() {
        if (cacheItemStackArr == null) {

            ItemStack[] itemStackArr = new ItemStack[27];
            Arrays.fill(itemStackArr, Items.AIR.getDefaultStack());

            ComponentMap components = boxItemStack.getComponents();
            if (components.contains(DataComponentTypes.CONTAINER)) {
                ContainerComponentAccessor container = ((ContainerComponentAccessor) (Object) components.get(DataComponentTypes.CONTAINER));
                DefaultedList<ItemStack> stacks = container.meteor$getStacks();

                for (int i = 0; i < stacks.size(); i++) {

                    ItemStack stack = stacks.get(i);
                    itemStackArr[i] = stack.copy();
                    empty = false;
                }
            }
            this.cacheItemStackArr = itemStackArr;
        }

        return cacheItemStackArr;
    }

    public int getOriginItemStackCount() {
        return boxItemStack.getCount();
    }

    public List<ItemStack> getStacks() {
        return Arrays.asList(readItemStackArr());
    }

    public int getColor() {
        net.minecraft.item.Item item = boxItemStack.getItem();
        if (item == net.minecraft.item.Items.WHITE_SHULKER_BOX) return 0xFFF9FFFE;
        if (item == net.minecraft.item.Items.ORANGE_SHULKER_BOX) return 0xFFF9801D;
        if (item == net.minecraft.item.Items.MAGENTA_SHULKER_BOX) return 0xFFC74EBD;
        if (item == net.minecraft.item.Items.LIGHT_BLUE_SHULKER_BOX) return 0xFF3AB3DA;
        if (item == net.minecraft.item.Items.YELLOW_SHULKER_BOX) return 0xFFFED83D;
        if (item == net.minecraft.item.Items.LIME_SHULKER_BOX) return 0xFF80C71F;
        if (item == net.minecraft.item.Items.PINK_SHULKER_BOX) return 0xFFF38BAA;
        if (item == net.minecraft.item.Items.GRAY_SHULKER_BOX) return 0xFF474F52;
        if (item == net.minecraft.item.Items.LIGHT_GRAY_SHULKER_BOX) return 0xFF9D9D97;
        if (item == net.minecraft.item.Items.CYAN_SHULKER_BOX) return 0xFF169C9C;
        if (item == net.minecraft.item.Items.PURPLE_SHULKER_BOX) return 0xFF8932B8;
        if (item == net.minecraft.item.Items.BLUE_SHULKER_BOX) return 0xFF3C44AA;
        if (item == net.minecraft.item.Items.BROWN_SHULKER_BOX) return 0xFF835432;
        if (item == net.minecraft.item.Items.GREEN_SHULKER_BOX) return 0xFF5E7C16;
        if (item == net.minecraft.item.Items.RED_SHULKER_BOX) return 0xFFB02E26;
        if (item == net.minecraft.item.Items.BLACK_SHULKER_BOX) return 0xFF1D1D21;
        return 0xFF9953B0;
    }

    /**
     * 判断盒子中的每格物品是否与模板列表一致（不校验数量，只校验物品类型）
     *
     * @param templateItems 模板物品列表，长度应为27（潜影盒格子数）
     * @return 如果盒子每格的物品类型与模板一致返回true，否则返回false
     */
    public boolean matchesTemplate(List<ItemBo> templateItems) {
        ItemStack[] boxItems = readItemStackArr();

        // 遍历模板的每个格子进行对比
        int size = Math.min(templateItems.size(), boxItems.length);
        for (int i = 0; i < size; i++) {
            ItemBo templateItemBo = templateItems.get(i);
            ItemBo boxItemBo = new ItemBo(boxItems[i]);

            // 只比较物品类型，不比较数量
            if (!Objects.equals(templateItemBo, boxItemBo)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Iterator<ItemStack> iterator() {

        readItemStackArr();

        return new Iterator<>() {

            int i = 0;

            @Override
            public boolean hasNext() {
                return i < cacheItemStackArr.length;
            }

            @Override
            public ItemStack next() {
                return cacheItemStackArr[i++].copy();
            }
        };
    }
}
