package com.xiaohe66.mc.meteor.lotus.bo;

import net.minecraft.item.Item;

/**
 * @author He
 * @since 2025.11.08 14:55
 */
public class ItemWarp {

    private Item item;
    private int count;
    private float minDistance = Float.MAX_VALUE;

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public float getMinDistance() {
        return minDistance;
    }

    public void setMinDistance(float minDistance) {
        this.minDistance = minDistance;
    }
}