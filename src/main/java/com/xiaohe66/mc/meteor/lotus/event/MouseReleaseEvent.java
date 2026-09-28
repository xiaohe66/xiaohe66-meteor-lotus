package com.xiaohe66.mc.meteor.lotus.event;

import meteordevelopment.meteorclient.events.Cancellable;

/**
 * 鼠标释放事件
 *
 * @author He
 * @since 2026.04.04
 */
public class MouseReleaseEvent extends Cancellable {

    private static final MouseReleaseEvent INSTANCE = new MouseReleaseEvent();

    public double mouseX;
    public double mouseY;
    public int button;
    public int screenX;
    public int screenY;

    public static MouseReleaseEvent get(double mouseX, double mouseY, int button, int screenX, int screenY) {
        INSTANCE.mouseX = mouseX;
        INSTANCE.mouseY = mouseY;
        INSTANCE.button = button;
        INSTANCE.screenX = screenX;
        INSTANCE.screenY = screenY;
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }
}