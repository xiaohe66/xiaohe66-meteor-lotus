package com.xiaohe66.mc.meteor.lotus.modules.clearup;

import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author He
 * @since 2026.04.14
 */
public class ClearUpMapping implements ISerializable<ClearUpMapping> {

    private Item targetItem;
    private Set<Item> relatedItems;
    private boolean enabled;

    public ClearUpMapping(Item targetItem, Collection<Item> relatedItems, boolean enabled) {
        this.targetItem = targetItem;
        this.relatedItems = new HashSet<>(relatedItems);
        this.enabled = enabled;
    }

    public ClearUpMapping() {
        this(Items.AIR, new HashSet<>(), true);
    }

    public Item getTargetItem() {
        return targetItem;
    }

    public void setTargetItem(Item targetItem) {
        this.targetItem = targetItem;
    }

    public Set<Item> getRelatedItems() {
        return relatedItems;
    }

    public void setRelatedItems(Collection<Item> relatedItems) {
        this.relatedItems = new HashSet<>(relatedItems);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.putString("targetItem", Registries.ITEM.getId(targetItem).toString());
        NbtList list = new NbtList();
        for (Item item : relatedItems) {
            NbtCompound itemTag = new NbtCompound();
            itemTag.putString("id", Registries.ITEM.getId(item).toString());
            list.add(itemTag);
        }
        tag.put("relatedItems", list);
        tag.putBoolean("enabled", enabled);
        return tag;
    }

    @Override
    public ClearUpMapping fromTag(NbtCompound tag) {
        Identifier id = Identifier.tryParse(tag.getString("targetItem", ""));
        targetItem = id != null ? Registries.ITEM.get(id) : Items.AIR;
        relatedItems = new HashSet<>();
        NbtList list = tag.getListOrEmpty("relatedItems");
        for (NbtElement e : list) {
            if (e.getType() != NbtElement.COMPOUND_TYPE) continue;
            NbtCompound itemTag = (NbtCompound) e;
            Identifier itemId = Identifier.tryParse(itemTag.getString("id", ""));
            if (itemId != null) {
                relatedItems.add(Registries.ITEM.get(itemId));
            }
        }
        enabled = tag.getBoolean("enabled", true);
        return this;
    }
}
