package com.xiaohe66.mc.meteor.lotus.bo;

import com.xiaohe66.mc.meteor.lotus.util.EnchantmentUtils;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;

import java.util.Objects;

/**
 * TODO : 考虑给这个类增加缓存
 *
 * @author He
 * @since 2026.01.02 17:32
 */
public class ItemBo {

    private final Item item;
    private final String name;
    private final RegistryKey<Enchantment> enchantment;
    private final PotionContentsComponent potionContents;
    private final Integer mapId;

    public ItemBo(ItemStack itemStack) {
        this(itemStack, false);
    }

    public ItemBo(ItemStack itemStack, boolean haveName) {
        this.item = itemStack.getItem();
        this.name = haveName && itemStack.getCustomName() != null ? itemStack.getCustomName().getString() : null;
        this.enchantment = itemStack.getItem() == Items.ENCHANTED_BOOK ? EnchantmentUtils.getEnchantmentOne(itemStack) : null;

        PotionContentsComponent potion = itemStack.get(DataComponentTypes.POTION_CONTENTS);
        this.potionContents = potion != null && !potion.equals(PotionContentsComponent.DEFAULT) ? potion : null;

        MapIdComponent mapIdComponent = itemStack.get(DataComponentTypes.MAP_ID);
        this.mapId = mapIdComponent != null ? mapIdComponent.id() : null;
    }

    public ItemBo(Item item) {
        this.item = item;
        this.name = null;
        this.enchantment = null;
        this.potionContents = null;
        this.mapId = null;
    }

    public ItemBo(Item item, RegistryKey<Enchantment> enchantment) {
        this.item = item;
        this.name = null;
        this.enchantment = enchantment;
        this.potionContents = null;
        this.mapId = null;
    }

    public Item getItem() {
        return item;
    }

    public RegistryKey<Enchantment> getEnchantment() {
        return enchantment;
    }

    public PotionContentsComponent getPotionContents() {
        return potionContents;
    }

    public Integer getMapId() {
        return mapId;
    }

    public String getName() {
        return item == Items.ENCHANTED_BOOK ? Names.get(enchantment) : Names.get(item);
    }

    public boolean isSameItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return false;
        Item testItem = itemStack.getItem();
        if (!Objects.equals(item, testItem)) return false;

        if (testItem == Items.ENCHANTED_BOOK) {
            RegistryKey<Enchantment> testEnchantment = EnchantmentUtils.getEnchantmentOne(itemStack);
            return Objects.equals(enchantment, testEnchantment);
        }

        PotionContentsComponent testPotion = itemStack.get(DataComponentTypes.POTION_CONTENTS);
        PotionContentsComponent testPotionNormalized = testPotion != null && !testPotion.equals(PotionContentsComponent.DEFAULT) ? testPotion : null;
        if (!Objects.equals(potionContents, testPotionNormalized)) return false;

        net.minecraft.component.type.MapIdComponent testMapIdComponent = itemStack.get(DataComponentTypes.MAP_ID);
        Integer testMapId = testMapIdComponent != null ? testMapIdComponent.id() : null;
        return Objects.equals(mapId, testMapId);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        ItemBo itemBo = (ItemBo) o;
        return Objects.equals(item, itemBo.item)
                && Objects.equals(name, itemBo.name)
                && Objects.equals(enchantment, itemBo.enchantment)
                && Objects.equals(potionContents, itemBo.potionContents)
                && Objects.equals(mapId, itemBo.mapId);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(item);
        result = 31 * result + Objects.hashCode(name);
        result = 31 * result + Objects.hashCode(enchantment);
        result = 31 * result + Objects.hashCode(potionContents);
        result = 31 * result + Objects.hashCode(mapId);
        return result;
    }
}
