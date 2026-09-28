package com.xiaohe66.mc.meteor.lotus.util;

import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import meteordevelopment.meteorclient.mixin.ContainerComponentAccessor;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author He
 * @since 2025.05.11 14:53
 */
public class HeInvUtils {

    public static final MinecraftClient mc = MinecraftClient.getInstance();

    public static final int MAX_SLOT = 36;

    public static void closeCurScreen() {
        if (!(mc.player.currentScreenHandler instanceof PlayerScreenHandler)) {
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
            mc.player.closeHandledScreen();
        }
    }

    public static FindItemResult findAndMoveHotbar(Item item) {
        FindItemResult enderChestResult = InvUtils.findInHotbar(item);
        if (enderChestResult.slot() != -1) {
            return enderChestResult;
        }

        enderChestResult = InvUtils.find(item);
        int enderChestSlot = enderChestResult.slot();
        if (enderChestSlot == -1) {
            return null;
        }

        int mainSlot = getMainSlot();
        InvUtils.move().from(enderChestResult.slot()).to(mainSlot);

        return InvUtils.findInHotbar(item);
    }

    public static FindItemResult findShulkerBoxInHotBar(Item item) {
        return InvUtils.find(itemStack -> hasItem(item, itemStack), 0, 9);
    }

    public static FindItemResult findShulkerBoxNotEmpty() {
        return InvUtils.find(itemStack -> {
            if (HeItemUtils.isShulkerBox(itemStack.getItem())) {
                ShulkerBoxReader reader = new ShulkerBoxReader(itemStack);
                return !reader.isEmpty();
            }
            return false;
        }, 0, 36);
    }

    public static FindItemResult findShulkerBox() {
        return InvUtils.find(itemStack -> HeItemUtils.isShulkerBox(itemStack.getItem()), 0, 36);
    }

    public static FindItemResult findShulkerBox(Item item) {
        return InvUtils.find(itemStack -> hasItem(item, itemStack), 0, 36);
    }

    public static List<ItemStack> findAndMargeShulkerBox() {
        List<ItemStack> kitItemStackList = mc.player.currentScreenHandler.slots.stream()
                .map(Slot::getStack)
                .filter(Objects::nonNull)
                .filter(itemStack -> HeItemUtils.isShulkerBox(itemStack.getItem()))
                .toList();

        Map<String, ItemStack> map = new LinkedHashMap<>();
        for (ItemStack kitItemStack : kitItemStackList) {

            ComponentMap components = kitItemStack.getComponents();
            ContainerComponentAccessor container = ((ContainerComponentAccessor) (Object) components.get(DataComponentTypes.CONTAINER));
            DefaultedList<ItemStack> stacks = container.meteor$getStacks();
            String key = stacks.toString();
            if (map.containsKey(key)) {
                ItemStack itemStack = map.get(key);
                itemStack.setCount(itemStack.getCount() + 1);
            } else {
                map.put(key, kitItemStack.copy());
            }
        }

        List<ItemStack> itemStacks = new ArrayList<>(map.values());
        itemStacks.sort((o1, o2) -> Integer.compare(o2.getCount(), o1.getCount()));
        return itemStacks;
    }

    private static boolean hasItem(Item item, ItemStack itemStack) {
        if (HeItemUtils.isShulkerBox(itemStack.getItem())) {

            ShulkerBoxReader reader = new ShulkerBoxReader(itemStack);
            if (item == Items.AIR) {
                return reader.isEmpty();
            } else {
                return reader.hasItem(new ItemBo(item));
            }
        }
        return false;
    }

    public static boolean isHotbar(int slot) {
        return slot >= 0 && slot <= 8;
    }

    public static int findBookSlot(RegistryKey<Enchantment> enchantment) {

        PlayerInventory playerInventory = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {

            ItemStack bookItemStack = playerInventory.getStack(i);
            if (bookItemStack == null || bookItemStack.getItem() != Items.ENCHANTED_BOOK) {
                continue;
            }

            Set<RegistryKey<Enchantment>> bookEnchantMentSet = EnchantmentUtils.getEnchantment(bookItemStack);
            if (bookEnchantMentSet.contains(enchantment)) {
                return i;
            }
        }

        return -1;
    }

    public static int findBookSlot(RegistryKey<Enchantment> enchantment, RegistryKey<Enchantment> enchantment2) {

        PlayerInventory playerInventory = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {

            ItemStack bookItemStack = playerInventory.getStack(i);
            if (bookItemStack == null || bookItemStack.getItem() != Items.ENCHANTED_BOOK) {
                continue;
            }

            Set<RegistryKey<Enchantment>> bookEnchantMentSet = EnchantmentUtils.getEnchantment(bookItemStack);
            if (bookEnchantMentSet.contains(enchantment) && bookEnchantMentSet.contains(enchantment2)) {
                return i;
            }
        }

        return -1;
    }

    public static int findBookSlot(RegistryKey<Enchantment>... enchantmentArr) {
        Set<RegistryKey<Enchantment>> enchantmentSet = new HashSet<>(Arrays.asList(enchantmentArr));
        return findBookSlot(enchantmentSet);
    }

    public static int findBookSlot(Set<RegistryKey<Enchantment>> enchantmentSet) {

        PlayerInventory playerInventory = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {

            ItemStack bookItemStack = playerInventory.getStack(i);
            if (bookItemStack == null || bookItemStack.getItem() != Items.ENCHANTED_BOOK) {
                continue;
            }

            Set<RegistryKey<Enchantment>> bookEnchantMentSet = EnchantmentUtils.getEnchantment(bookItemStack);
            boolean exist = true;
            for (RegistryKey<Enchantment> enchantment : enchantmentSet) {
                if (!bookEnchantMentSet.contains(enchantment)) {
                    exist = false;
                    break;
                }
            }
            if (exist) {
                return i;
            }
        }

        return -1;
    }

    public static int findBookSlotInChest(GenericContainerScreenHandler screenHandler, RegistryKey<Enchantment> enchantment) {

        Inventory inventory = screenHandler.getInventory();

        for (int slotId = 0, n = inventory.size(); slotId < n; slotId++) {

            ItemStack bookItemStack = screenHandler.getSlot(slotId).getStack();

            if (bookItemStack == null || bookItemStack.getItem() != Items.ENCHANTED_BOOK) {
                continue;
            }

            Set<RegistryKey<Enchantment>> bookEnchantMentSet = EnchantmentUtils.getEnchantment(bookItemStack);
            if (bookEnchantMentSet.contains(enchantment)) {
                return slotId;
            }
        }

        return -1;
    }

    /**
     * 找没有附魔的装备
     */
    public static int findEquipSlotInChest(Inventory inventory, Item item) {

        for (int slotId = 0, n = inventory.size(); slotId < n; slotId++) {

            ItemStack bookItemStack = inventory.getStack(slotId);

            if (bookItemStack == null || bookItemStack.getItem() != item) {
                continue;
            }

            Set<RegistryKey<Enchantment>> bookEnchantMentSet = EnchantmentUtils.getEnchantment(bookItemStack);
            if (bookEnchantMentSet.isEmpty()) {
                return slotId;
            }
        }

        return -1;
    }

    public static int findItemSlot(Item item) {
        PlayerInventory inventory = mc.player.getInventory();
        for (int i = 0; i < MAX_SLOT; i++) {

            ItemStack itemStack = inventory.getStack(i);
            if (itemStack.getItem() == item) {
                return i;
            }
        }

        return -1;
    }

    public static int findFullItemSlot(Item item) {
        PlayerInventory inventory = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {

            ItemStack itemStack = inventory.getStack(i);
            if (itemStack.getItem() == item && itemStack.getCount() == itemStack.getMaxCount()) {
                return i;
            }
        }

        return -1;
    }

    public static boolean findItemAndSwitch(Item item) {
        int slot = findItemSlot(item);
        if (slot < 0) {
            return false;
        }

        swapToSlot(slot);

        return true;
    }

    public static void swapToSlot(int slot) {
        if (mc.player.getInventory().getSelectedSlot() != slot) {
            mc.player.getInventory().setSelectedSlot(slot);
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }

    /**
     * 将背包的物品拿在手上
     */
    public static void swapMainHand(int formSlot) {

        if (isHotbar(formSlot)) {
            swapToSlot(formSlot);

        } else {
            boolean needSwap = !InvUtils.testInMainHand(Items.AIR);
            InvUtils.move().from(formSlot).to(HeInvUtils.getMainSlot());
            if (needSwap) {
                // TODO : 需要延迟
                InvUtils.click().to(formSlot);
            }
        }
    }

    public static void swap(int formSlot, int toSlot) {

        if (isHotbar(formSlot)) {
            swapToSlot(formSlot);

        } else {
            ItemStack itemStack = mc.player.getInventory().getStack(formSlot);
            boolean needSwap = !itemStack.isEmpty();
            InvUtils.move().from(formSlot).to(toSlot);
            if (needSwap) {
                // TODO : 需要延迟
                InvUtils.click().to(formSlot);
            }
        }
    }

    /**
     * 背包切换, 若物品在快捷栏上就直接切换。若物品在背包, 则将背包物品切换到主手上。
     *
     * @param item 物品, Items.?
     */
    public static boolean swap(Item item) {

        FindItemResult enderChestResult = InvUtils.findInHotbar(item);
        if (enderChestResult.slot() != -1) {
            return InvUtils.swap(enderChestResult.slot(), true);
        }

        enderChestResult = InvUtils.find(item);
        int enderChestSlot = enderChestResult.slot();
        if (enderChestSlot == -1) {
            return false;
        }

        int mainSlot = getMainSlot();
        InvUtils.move().from(enderChestResult.slot()).to(mainSlot);

        return true;
    }


    public static boolean isKitInMainHand() {
        return InvUtils.testInMainHand(
                Items.SHULKER_BOX,
                Items.WHITE_SHULKER_BOX,
                Items.ORANGE_SHULKER_BOX,
                Items.MAGENTA_SHULKER_BOX,
                Items.LIGHT_BLUE_SHULKER_BOX,
                Items.YELLOW_SHULKER_BOX,
                Items.LIME_SHULKER_BOX,
                Items.PINK_SHULKER_BOX,
                Items.GRAY_SHULKER_BOX,
                Items.LIGHT_GRAY_SHULKER_BOX,
                Items.CYAN_SHULKER_BOX,
                Items.PURPLE_SHULKER_BOX,
                Items.BLUE_SHULKER_BOX,
                Items.BROWN_SHULKER_BOX,
                Items.GREEN_SHULKER_BOX,
                Items.RED_SHULKER_BOX,
                Items.BLACK_SHULKER_BOX);
    }

    public static int getMainSlot() {
        return mc.player.getInventory().getSelectedSlot();
    }
}
