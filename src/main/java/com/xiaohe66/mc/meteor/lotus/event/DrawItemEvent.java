package com.xiaohe66.mc.meteor.lotus.event;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

/**
 * @author He
 * @since 2025.07.26 16:48
 */
public class DrawItemEvent extends Event {

    private static final DrawItemEvent INSTANCE = new DrawItemEvent();

    private DrawContext drawContext;
    private ItemStack itemStack;
    private int x;
    private int y;

    public static DrawItemEvent get(DrawContext drawContext, ItemStack stack, int x, int y) {
        INSTANCE.drawContext = drawContext;
        INSTANCE.itemStack = stack;
        INSTANCE.x = x;
        INSTANCE.y = y;

        return INSTANCE;
    }

    public DrawItemEvent() {
        super(Stage.Post);
    }

    public DrawContext getDrawContext() {
        return drawContext;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
