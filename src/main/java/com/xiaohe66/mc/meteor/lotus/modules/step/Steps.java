package com.xiaohe66.mc.meteor.lotus.modules.step;

/**
 * @author He
 * @since 2025.08.26 22:03
 */
public class Steps {

    public static final Step NONE = new Step("NONE");
    public static final Step CLOSE_SCREEN = new Step("CLOSE_SCREEN");
    public static final Step READY = new Step("READY");
    public static final Step WAIT = new Step("WAIT");
    public static final Step WALKING = new Step("WALKING");
    public static final Step NEXT = new Step("NEXT");
    public static final Step TOGGLE = new Step("TOGGLE");

    public static final Step PLACE = new Step("PLACE");

    public static final Step TAKE_KIT = new Step("TAKE_KIT");
    public static final Step PUT_KIT = new Step("PUT_KIT");
    public static final Step PLACE_KIT = new Step("PLACE_KIT");
    public static final Step BREAK_KIT = new Step("BREAK_KIT");
    public static final Step CHECK_KIT = new Step("CHECK_KIT");

    public static final Step GOTO_TAKE_ITEM = new Step("GOTO_TAKE_ITEM");
    public static final Step TAKE_ITEM = new Step("TAKE_ITEM");

    public static final Step GOTO_PUT_ITEM = new Step("GOTO_PUT_ITEM");
    public static final Step PUT_ITEM = new Step("PUT_ITEM");
    public static final Step TAKE_LOOSE_ITEM = new Step("TAKE_LOOSE_ITEM");

    public static final Step PICK_UP_ITEM = new Step("PICK_UP_ITEM");

    public static final Step TAKE_EMPTY_KIT = new Step("TAKE_EMPTY_KIT");
    public static final Step PLACE_EMPTY_KIT = new Step("PLACE_EMPTY_KIT");
    public static final Step PUT_EMPTY_KIT = new Step("PUT_EMPTY_KIT");

    public static final Step OPEN_TRADE = new Step("OPEN_TRADE");
    public static final Step EXECUTE_TRADE = new Step("EXECUTE_TRADE");

    public static final Step TAKE_BOOK = new Step("TAKE_BOOK");
    public static final Step GRIND = new Step("GRIND");
    public static final Step USE = new Step("USE");
    public static final Step LEVEL = new Step("LEVEL");

    /**
     * 打开箱子的几种状态
     */
    public static final Step IDLE = new Step("LEVEL");
    public static final Step OPENING = new Step("OPENING");
    public static final Step ROTATION = new Step("ROTATION");

    // public static final Step PLACE_FRAME = new Step("PLACE_FRAME");
    // public static final Step PLACE_MAP = new Step("PLACE_MAP");

    // ElytraAssist 步骤
    public static final Step MONITOR = new Step("MONITOR");
    public static final Step EQUIP_ELYTRA = new Step("EQUIP_ELYTRA");
    public static final Step SWITCH_FIREWORK = new Step("SWITCH_FIREWORK");
    public static final Step DEPLOY_ELYTRA = new Step("DEPLOY_ELYTRA");
    public static final Step USE_FIREWORK = new Step("USE_FIREWORK");

    // VillagerBookRoller 步骤
    public static final Step FINDING_TARGET = new Step("FINDING_TARGET");
    public static final Step GOTO_TARGET = new Step("GOTO_TARGET");
    public static final Step PLACE_LECTERN = new Step("PLACE_LECTERN");
    public static final Step WAIT_PROFESSION = new Step("WAIT_PROFESSION");
    public static final Step CHECK_TRADES = new Step("CHECK_TRADES");
    public static final Step BREAK_LECTERN = new Step("BREAK_LECTERN");
    public static final Step WAIT_PROFESSION_CLEAR = new Step("WAIT_PROFESSION_CLEAR");
}
