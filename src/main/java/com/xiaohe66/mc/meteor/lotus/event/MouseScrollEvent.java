package com.xiaohe66.mc.meteor.lotus.event;

import meteordevelopment.meteorclient.events.Cancellable;

/**
 * 鼠标滚动事件
 *
 * @author He
 * @since 2026.04.03
 */
public class MouseScrollEvent extends Cancellable {

    private static final MouseScrollEvent INSTANCE = new MouseScrollEvent();

    private double verticalAmount;
    private double mouseX;
    private double mouseY;
    private int screenX;
    private int screenY;

    public static MouseScrollEvent get(double mouseX, double mouseY, double verticalAmount, int screenX, int screenY) {
        INSTANCE.mouseX = mouseX;
        INSTANCE.mouseY = mouseY;
        INSTANCE.verticalAmount = verticalAmount;
        INSTANCE.screenX = screenX;
        INSTANCE.screenY = screenY;
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }

    public double getVerticalAmount() {
        return verticalAmount;
    }

    public double getMouseX() {
        return mouseX;
    }

    public double getMouseY() {
        return mouseY;
    }

    public int getScreenX() {
        return screenX;
    }

    public int getScreenY() {
        return screenY;
    }
}