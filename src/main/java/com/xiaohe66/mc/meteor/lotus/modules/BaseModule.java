package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.step.Step;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.Const;
import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeRotationUtils;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author He
 * @since 2025.06.14 10:26
 */
public abstract class BaseModule extends Module {

    protected final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Integer> delay;

    private int delayTimer;
    private int curPlayerSlot;
    private int curScreenSlot;

    private Step openStatus = Steps.IDLE;
    private long lastOpenTime = 0;
    // private Step rotationStatus = Steps.IDLE;

    public BaseModule(String name, String description) {
        this(name, description, 5);

    }

    public BaseModule(String name, String description, int defaultDelay) {
        this(name, description, defaultDelay, 40);
    }

    public BaseModule(String name, String description, int defaultDelay, int max) {
        super(Const.CATEGORY, name, description);

        delay = sgGeneral.add(new IntSetting.Builder()
                .name("延迟")
                .min(0)
                .sliderMax(max)
                .defaultValue(defaultDelay)
                .build());
    }

    protected boolean checkAndDecrement() {
        if (delayTimer > 0) {
            delayTimer--;
            return false;
        }

        return true;
    }

    protected PlayerInventory getPlayerInventory() {
        return mc.player.getInventory();
    }

    protected ItemStack getItemStack(int slot) {
        return getPlayerInventory().getStack(slot);
    }

    protected ItemStack getItemStackBySlotId(int slotId) {
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        return screenHandler.getSlot(slotId).getStack();
    }

    protected ItemStack nextPlayerStack(Predicate<ItemStack> predicate) {
        int startSlot = curPlayerSlot;
        int max = getPlayerMainSize() - 1;
        while (true) {
            if (curPlayerSlot >= max) {
                curPlayerSlot = 0;
            } else {
                curPlayerSlot++;
            }

            ItemStack itemStack = getItemStack(curPlayerSlot);
            if (!itemStack.isEmpty() && predicate.test(itemStack)) {
                return itemStack;
            }
            if (curPlayerSlot == startSlot) {
                return ItemStack.EMPTY;
            }
        }
    }

    /**
     * 只能用于打开同一个屏幕
     */
    protected ItemStack nextScreenStack(Predicate<ItemStack> predicate) {
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        int max = getScreenMainSize() - 1;

        int startSlot = curScreenSlot;
        while (true) {
            if (curScreenSlot >= max) {
                curScreenSlot = 0;
            } else {
                curScreenSlot++;
            }

            ItemStack itemStack = screenHandler.getSlot(curScreenSlot).getStack();
            if (!itemStack.isEmpty() && predicate.test(itemStack)) {
                return itemStack;
            }
            if (curScreenSlot == startSlot) {
                return ItemStack.EMPTY;
            }
        }
    }

    protected boolean hasScreenFull() {
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        int size = getScreenMainSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = screenHandler.getSlot(i).getStack();
            if (stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    protected void openChest(BlockPos blockPos, Consumer<Inventory> inventoryConsumer) {
        if (mc.player.currentScreenHandler instanceof PlayerScreenHandler) {
            tryRotateAndOpen(blockPos, null);

        } else if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler screenHandler) {
            openStatus = Steps.IDLE;
            Inventory inventory = screenHandler.getInventory();
            inventoryConsumer.accept(inventory);

        } else {
            openStatus = Steps.IDLE;
            HeInvUtils.closeCurScreen();
            warning("打开的容器错误");

            setDelay();
        }
    }

    protected void openKit(BlockPos blockPos, Consumer<ShulkerBoxScreenHandler> screenHandlerConsumer) {
        openKit(blockPos, null, screenHandlerConsumer);
    }

    protected void openKit(BlockPos blockPos, Direction direction, Consumer<ShulkerBoxScreenHandler> screenHandlerConsumer) {
        if (mc.player.currentScreenHandler instanceof PlayerScreenHandler) {
            BlockState blockState = mc.world.getBlockState(blockPos);
            if (blockState.getBlock() instanceof ShulkerBoxBlock) {
                tryRotateAndOpen(blockPos, direction);
            }
        } else if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler screenHandler) {
            openStatus = Steps.IDLE;
            screenHandlerConsumer.accept(screenHandler);

        } else {
            openStatus = Steps.IDLE;
            HeInvUtils.closeCurScreen();
            warning("打开的容器错误");

            setDelay();
        }
    }

    /*protected void rotate(Vec3d vec3d, Runnable runnable) {
        if (rotationStatus == Steps.IDLE) {
            HeRotationUtils.rotate(vec3d);
            rotationStatus = Steps.ROTATION;
        } else {
            rotationStatus = Steps.IDLE;
            runnable.run();
        }
    }*/

    protected void rotateAndOpen(BlockPos blockPos) {
        rotateAndOpen(blockPos, null);
    }

    protected void rotateAndOpen(BlockPos blockPos, Direction direction) {
        Direction finalDirection = direction == null ? BlockUtils.getDirection(blockPos) : direction;
        Vec3i vector = finalDirection.getVector();
        double offset = 0.45;
        Vec3d vec3d = new Vec3d(blockPos.getX() + 0.5 + vector.getX() * offset, blockPos.getY() + 0.5 + vector.getY() * offset, blockPos.getZ() + 0.5 + vector.getZ() * offset);

        HeRotationUtils.rotate(vec3d, () -> {
            HeBlockUtils.open(blockPos, finalDirection, vec3d);
        });
    }

    private void tryRotateAndOpen(BlockPos blockPos, Direction direction) {
        long curTime = System.currentTimeMillis();
        if (openStatus == Steps.IDLE || curTime - lastOpenTime > 1000) {

            rotateAndOpen(blockPos, direction);

            // NOTE : 在打开背包处需要将 openStatus 设置为 Steps.IDLE
            openStatus = Steps.OPENING;
            lastOpenTime = curTime;
        } else {
            warning("打开没有反应...");
        }
        setDelay();
    }

    /**
     * 带边缘偏移的打开方法
     * 当从 UP 方向打开时，点击位置会偏向玩家最近的水平边缘（0.1 或 0.9），而非中心 0.5
     * 用于绕过潜影盒上方的阻挡物
     */
    protected void rotateAndOpenWithOffset(BlockPos blockPos, Direction direction) {
        Vec3d vec3d;

        // TODO : 再考虑兼容其实面
        if (direction == Direction.UP) {
            double playerX = mc.player.getX();
            double playerZ = mc.player.getZ();
            double blockCenterX = blockPos.getX() + 0.5;
            double blockCenterZ = blockPos.getZ() + 0.5;

            // 计算玩家在 X 和 Z 方向相对于方块中心的距离
            double distX = Math.abs(playerX - blockCenterX);
            double distZ = Math.abs(playerZ - blockCenterZ);

            // 偏移量：从中心 0.5 偏移到 0.1 或 0.9
            double offset = 0.4;

            double hitX = 0.5;
            double hitZ = 0.5;

            // 只在离玩家更近的那个轴上偏移
            if (distX > distZ) {
                // X 轴距离更远，优先向 X 方向偏移
                hitX = playerX > blockCenterX ? 0.5 + offset : 0.5 - offset;
            } else {
                // Z 轴距离更远，优先向 Z 方向偏移
                hitZ = playerZ > blockCenterZ ? 0.5 + offset : 0.5 - offset;
            }

            // Y 坐标：方块顶部 + 小偏移（确保在方块上方）
            double hitY = blockPos.getY() + 0.95;

            vec3d = new Vec3d(blockPos.getX() + hitX, hitY, blockPos.getZ() + hitZ);
        } else {
            // 其他方向保持原逻辑
            Vec3i vector = direction.getVector();
            double offset = 0.5;
            vec3d = new Vec3d(
                    blockPos.getX() + 0.5 + vector.getX() * offset,
                    blockPos.getY() + 0.5 + vector.getY() * offset,
                    blockPos.getZ() + 0.5 + vector.getZ() * offset
            );
        }

        HeRotationUtils.rotate(vec3d, () -> {
            HeBlockUtils.open(blockPos, direction, vec3d);
        });
    }

    protected void interactEntity(Entity entity) {
        Vec3d playerPos = mc.player.getEntityPos();
        Vec3d entityPos = entity.getEntityPos();
        EntityHitResult entityHitResult = ProjectileUtil.raycast(mc.player, playerPos, entityPos, entity.getBoundingBox(), Entity::canHit, playerPos.squaredDistanceTo(entityPos));
        if (entityHitResult == null) {

            // NOTE : 这里是中间被挡了
            /*if (debug.get()) {
                info("射线检测不通过");
            }*/

            // 确保玩家面向村民
            HeRotationUtils.rotate(entity.getEyePos(), () -> {
                mc.interactionManager.interactEntity(mc.player, entity, Hand.MAIN_HAND);
            });

        } else {

            /*if (debug.get()) {
                info("射线检测通过");
            }*/

            // 面向碰撞点
            HeRotationUtils.rotate(entityHitResult.getPos(), () -> {
                ActionResult actionResult = mc.interactionManager.interactEntityAtLocation(mc.player, entity, entityHitResult, Hand.MAIN_HAND);
                if (!actionResult.isAccepted()) {
                    mc.interactionManager.interactEntity(mc.player, entity, Hand.MAIN_HAND);
                }
            });
        }
    }

    protected boolean swapToMainHand(int slot) {
        if (slot != getMainSlot()) {
            // 将空盒切换到主手
            HeInvUtils.swap(slot, getMainSlot());
            setDelay();
            return true;
        }
        return false;
    }

    protected int getMainSlot() {
        return HeInvUtils.getMainSlot();
    }

    protected boolean isReady() {
        return mc.player != null && mc.world != null;
    }

    protected long mcTime() {
        return mc.world.getTimeOfDay();
    }

    protected long mcDay() {
        return mc.world.getTimeOfDay() / 24000;
    }

    protected long mcTimeOfDay() {
        return mc.world.getTimeOfDay() % 24000;
    }

    protected void setDelay() {
        delayTimer = delay.get();
    }

    protected void setDelay(int delay) {
        delayTimer = delay;
    }

    public int getDelayTimer() {
        return delayTimer;
    }

    public int getPlayerMainSize() {
        return mc.player.getInventory().getMainStacks().size();
    }

    public int getScreenMainSize() {
        if (mc.player.currentScreenHandler instanceof PlayerScreenHandler) {
            return mc.player.currentScreenHandler.slots.size() - getPlayerMainSize() - 1;
        } else {
            return mc.player.currentScreenHandler.slots.size() - getPlayerMainSize();
        }
    }

    public boolean isContainer(int slotId) {
        return slotId < getScreenMainSize();
    }

    public boolean isContainerFull() {
        int n = getScreenMainSize();
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        for (int i = 0; i < n; i++) {
            ItemStack itemStack = screenHandler.getSlot(i).getStack();
            if (itemStack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean isInvFull() {
        int n = getPlayerMainSize();
        PlayerInventory playerInventory = getPlayerInventory();
        for (int i = 0; i < n; i++) {
            ItemStack itemStack = playerInventory.getStack(i);
            if (itemStack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void initCurPlayerSlot() {
        this.curPlayerSlot = getPlayerMainSize() - 1;
    }

    public int getCurPlayerSlot() {
        return curPlayerSlot;
    }

    public void initCurScreenSlot() {
        this.curPlayerSlot = getScreenMainSize() - 1;
    }

    public int getCurScreenSlot() {
        return curScreenSlot;
    }
}
