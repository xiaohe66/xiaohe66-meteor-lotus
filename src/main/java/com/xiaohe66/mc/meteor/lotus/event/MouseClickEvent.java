package com.xiaohe66.mc.meteor.lotus.event;

import meteordevelopment.meteorclient.events.Cancellable;

/**
 * 鼠标点击事件
 *
 * @author He
 * @since 2026.04.04
 */
public class MouseClickEvent extends Cancellable {

    private static final MouseClickEvent INSTANCE = new MouseClickEvent();

    public double mouseX;
    public double mouseY;
    public int button;
    public boolean doubled;
    public int screenX;
    public int screenY;

    public static MouseClickEvent get(double mouseX, double mouseY, int button, boolean doubled, int screenX, int screenY) {
        INSTANCE.mouseX = mouseX;
        INSTANCE.mouseY = mouseY;
        INSTANCE.button = button;
        INSTANCE.doubled = doubled;
        INSTANCE.screenX = screenX;
        INSTANCE.screenY = screenY;
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }
}