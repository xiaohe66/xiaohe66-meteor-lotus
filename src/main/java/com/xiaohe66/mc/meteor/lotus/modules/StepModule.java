package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.bo.StorageItem;
import com.xiaohe66.mc.meteor.lotus.bo.StoragePos;
import com.xiaohe66.mc.meteor.lotus.modules.step.Step;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * @author He
 * @since 2025.08.26 21:57
 */
public class StepModule extends BaseModule {

    protected Step step = Steps.NONE;
    protected Step walkingNext;
    protected Step closeScreenAfter = Steps.NONE;
    protected int closeScreenAfterDelay;

    protected BooleanSupplier closeCheck = () -> true;

    protected Map<Step, Runnable> stepDispatch = new HashMap<>();

    public StepModule(String name, String description) {
        super(name, description);
        init();
    }

    public StepModule(String name, String description, int defaultDelay) {
        super(name, description, defaultDelay);
        init();
    }

    public StepModule(String name, String description, int defaultDelay, int max) {
        super(name, description, defaultDelay, max);
        init();
    }

    private void init() {
        addStep(Steps.NONE, this::none);
        addStep(Steps.CLOSE_SCREEN, this::closeScreen);
    }

    /**
     * TODO : 考虑放到父类
     */
    @EventHandler
    protected void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (!checkAndDecrement()) {
            return;
        }

        Runnable runnable = stepDispatch.get(step);
        if (runnable != null) {
            try {
                runnable.run();

            } catch (Exception e) {
                e.printStackTrace();
                error("发生未知异常 : " + e.getMessage());
                toggle();
            }

        } else {
            warning("未处理状态 : " + step.getName());
            toggle();
        }
    }

    protected void closeNext(Step nextStep) {
        this.closeScreenAfter = nextStep;
        this.step = Steps.CLOSE_SCREEN;
    }

    protected void closeScreen() {
        if (!closeCheck.getAsBoolean()) {
            setDelay();
            return;
        }

        HeInvUtils.closeCurScreen();

        if (closeScreenAfter != null) {
            step = closeScreenAfter;
        }

        if (closeScreenAfterDelay > 0) {
            setDelay(closeScreenAfterDelay);
            closeScreenAfterDelay = 0;

        } else {
            setDelay();
        }
    }

    protected void setCloseScreenAfterDelay(int closeScreenAfterDelay) {
        this.closeScreenAfterDelay = closeScreenAfterDelay;
    }

    protected void delayCloseNext(Step nextStep) {
        delayCloseNext(nextStep, () -> true);
    }

    protected void delayCloseNext(Step nextStep, BooleanSupplier closeCheck) {
        this.closeCheck = closeCheck;
        closeNext(nextStep);
        setDelay();
    }

    protected void delayNext(Step nextStep) {
        step = nextStep;
        setDelay();
    }

    protected void none() {

    }

    protected void breakStep(String msg) {
        warning(msg);
        step = Steps.NONE;
        closeScreenAfter = Steps.NONE;
    }

    protected void addStep(Step step, Runnable runnable) {
        stepDispatch.put(step, runnable);
    }

    // ==================== StoragePos 相关辅助方法 ====================

    /**
     * 检查玩家是否超出 StoragePos 的操作范围
     *
     * @param pos 目标位置
     * @return 如果超出范围返回 true
     */
    protected boolean notInOperationRange(StoragePos pos) {
        return mc.player.getEntityPos().distanceTo(pos.getBtnPos().toCenterPos()) > 1;
    }


    /**
     * 检查并构建 StoragePos 位置信息
     *
     * @param frame       展示框实体
     * @param storageItem 存储物品
     * @return 构建成功的 StoragePos，如果检查失败返回 null
     */
    protected StoragePos checkAndBuildStoragePos(ItemFrameEntity frame, StorageItem storageItem) {
        BlockPos framePos = frame.getBlockPos();
        BlockPos attachedBlockPos = frame.getAttachedBlockPos();

        // 放入位置：展示框附着方块上方，并向外偏移一格
        BlockPos putPos = attachedBlockPos.add(0, 1, 0).offset(frame.getFacing().getOpposite());
        if (mc.world.getBlockState(putPos).getBlock() != Blocks.CHEST) {
            return null;
        }

        // kit位置：展示框下方2格
        BlockPos kitPos = framePos.add(0, -2, 0);
        BlockState kitPosBlockState = mc.world.getBlockState(kitPos);
        if (!kitPosBlockState.isAir() && !(kitPosBlockState.getBlock() instanceof ShulkerBoxBlock)) {
            return null;
        }

        // 活塞位置：kit位置下方1格
        BlockPos pistonPos = kitPos.add(0, -1, 0);
        if (mc.world.getBlockState(pistonPos).getBlock() != Blocks.PISTON) {
            return null;
        }

        // kit位置需要和玩家同一高度
        if (kitPos.getY() != mc.player.getBlockPos().getY()) {
            return null;
        }

        // 按钮位置：kit位置向外偏移一格
        BlockPos btnPos = kitPos.offset(frame.getFacing());
        // 取出位置：放入位置下方2格
        BlockPos takePos = putPos.add(0, -2, 0);
        if (!(mc.world.getBlockState(btnPos).getBlock() instanceof ButtonBlock)) {
            return null;
        }

        return new StoragePos(storageItem, framePos, putPos, takePos, kitPos, btnPos);
    }
}
