package com.xiaohe66.mc.meteor.lotus.event;

/**
 * 界面关闭事件
 *
 * @author He
 * @since 2026.04.04
 */
public class ScreenCloseEvent extends Event {

    private static final ScreenCloseEvent INSTANCE = new ScreenCloseEvent();

    public static ScreenCloseEvent get() {
        return INSTANCE;
    }

    public ScreenCloseEvent() {
        super(Stage.Pre);
    }
}