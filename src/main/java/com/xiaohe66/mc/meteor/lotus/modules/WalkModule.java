package com.xiaohe66.mc.meteor.lotus.modules;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.event.events.PathEvent;
import baritone.api.event.listener.AbstractGameEventListener;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.process.ICustomGoalProcess;
import com.xiaohe66.mc.meteor.lotus.bo.StoragePos;
import com.xiaohe66.mc.meteor.lotus.modules.step.Step;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import net.minecraft.util.math.BlockPos;

/**
 *
 *
 * @author He
 * @since 2026.04.20 09:33
 */
public class WalkModule extends StepModule implements AbstractGameEventListener {

    protected static final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
    protected static final ICustomGoalProcess customGoalProcess = baritone.getCustomGoalProcess();
    protected static final Settings baritoneSettings = BaritoneAPI.getSettings();


    public WalkModule(String name, String description) {
        super(name, description);
        init();
    }

    public WalkModule(String name, String description, int defaultDelay) {
        super(name, description, defaultDelay);
        init();
    }

    public WalkModule(String name, String description, int defaultDelay, int max) {
        super(name, description, defaultDelay, max);
        init();
    }

    private void init() {
        baritone.getGameEventHandler().registerEventListener(this);
        addStep(Steps.WALKING, this::none);
    }

    @Override
    public void onActivate() {
        super.onActivate();
        // 禁止挖、禁止放
        baritoneSettings.allowBreak.value = false;
        baritoneSettings.allowPlace.value = false;
    }

    @Override
    public void onPathEvent(PathEvent event) {
        if (event == PathEvent.CANCELED && walkingNext != null) {
            step = walkingNext;
            walkingNext = null;
        }
    }

    protected void gotoTargetIfNeed(BlockPos targetPos, int range, Step walkingNext, String gotoTargetMsg) {
        double distance = mc.player.getEntityPos().distanceTo(targetPos.toCenterPos());
        if (distance > 1000) {
            warning("移动距离超过1000格, 功能关闭");
            toggle();
            return;
        }
        if (distance > range) {

            if (gotoTargetMsg != null) {
                info(gotoTargetMsg);
            }

            this.walkingNext = walkingNext;
            gotoTarget(targetPos, range);

        } else {
            step = walkingNext;
        }
    }

    protected void gotoTarget(BlockPos targetPos, int distance) {
        customGoalProcess.setGoalAndPath(new GoalNear(targetPos, distance));
        step = Steps.WALKING;
    }

    protected void gotoTarget(BlockPos targetPos, int distance, Step walkingNext) {
        this.walkingNext = walkingNext;
        gotoTarget(targetPos, distance);
    }

    /**
     * 前往 StoragePos 的按钮位置
     *
     * @param pos      目标位置
     * @param msg      提示信息
     * @param nextStep 到达后执行的步骤
     */
    protected void gotoBtnPos(StoragePos pos, String msg, Step nextStep) {
        info(msg);
        gotoTarget(pos.getBtnPos(), 0);
        this.walkingNext = nextStep;
    }

    @Override
    public void onDeactivate() {
        baritone.getCommandManager().execute("cancel");
        // 允许挖、允许放
        baritoneSettings.allowBreak.value = true;
        baritoneSettings.allowPlace.value = true;
    }
}
