package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import com.xiaohe66.mc.meteor.lotus.event.MouseClickEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseDragEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseReleaseEvent;
import com.xiaohe66.mc.meteor.lotus.event.ScreenCloseEvent;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import com.xiaohe66.mc.meteor.lotus.util.ShulkerBoxReader;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;

/**
 * 背包整理 - 背包物品快捷操作
 * <p>
 * 功能：
 * 1. 拖拽移动：按住配置键位拖拽鼠标，整组移动经过的物品
 * 2. 同类操作-移动：按住配置键位点击物品，移动所有同类型物品
 * 3. 同类操作-丢弃：按住配置键位拖拽物品到界面外，丢弃所有同类型物品
 *
 * @author He
 * @since 2026.04.04
 */
public class ItemClearUp extends BaseModule {

    // 排序：按物品ID
    private static final Comparator<ItemStack> ITEM_ID_COMPARATOR = Comparator.comparingInt(stack -> Registries.ITEM.getRawId(stack.getItem()));
    // 排序：按物品数量
    private static final Comparator<ItemStack> ITEM_COUNT_COMPARATOR = (a, b) -> Integer.compare(b.getCount(), a.getCount());
    private static final Comparator<ItemStack> ITEM_ID_COUNT_COMPARATOR = ITEM_ID_COMPARATOR.thenComparing(ITEM_COUNT_COMPARATOR);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> scrollMove = sgGeneral.add(new BoolSetting.Builder()
            .name("拖拽操作")
            .description("按住绑定的 键位 拖拽鼠标，整组移动经过的物品")
            .defaultValue(true)
            .build());

    private final Setting<Keybind> scrollMoveKey = sgGeneral.add(new KeybindSetting.Builder()
            .name("拖拽操作-键位")
            .description("按住绑定的 键位 拖拽鼠标，整组移动经过的物品")
            .defaultValue(Keybind.fromKey(GLFW_KEY_LEFT_SHIFT))
            .visible(scrollMove::get)
            .build());

    private final Setting<Boolean> sameClickMove = sgGeneral.add(new BoolSetting.Builder()
            .name("同类操作")
            .description("按住绑定的 键位 点击物品移动同类型，拖拽到界面外丢弃同类型")
            .defaultValue(true)
            .build());

    private final Setting<Keybind> sameClickMoveKey = sgGeneral.add(new KeybindSetting.Builder()
            .name("同类操作-键位")
            .description("按住绑定的 键位 点击物品移动同类型，拖拽到界面外丢弃同类型")
            .defaultValue(Keybind.fromKey(GLFW_KEY_LEFT_ALT))
            .build());

    private final Setting<Boolean> sortEnabled = sgGeneral.add(new BoolSetting.Builder()
            .name("物品排序")
            .description("按下绑定键位对当前区域物品进行排序")
            .defaultValue(true)
            .build());

    private final Setting<Keybind> sortKeybind = sgGeneral.add(new KeybindSetting.Builder()
            .name("排序键位")
            .description("按下后对当前区域物品进行排序（容器/背包）")
            .defaultValue(Keybind.fromKey(GLFW_KEY_R))
            .visible(sortEnabled::get)
            .action(() -> {
                if (sortEnabled.get()) {
                    startSort();
                }
            })
            .build());

    private final LinkedHashSet<Integer> taskSet = new LinkedHashSet<>();
    private final List<Integer> sortClickTaskList = new ArrayList<>();
    private Item drapItem;
    private boolean isDrapType;

    public ItemClearUp() {
        super("物品整理", "物品快捷操作：shift 拖拽移动、alt 同类移动/丢弃、R 排序", 0);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!checkAndDecrement()) {
            return;
        }

        // 优先执行排序点击任务
        if (!sortClickTaskList.isEmpty()) {
            Integer slotId = sortClickTaskList.removeFirst();
            InvUtils.click().slotId(slotId);
            setDelay();
            return;
        }

        if (taskSet.isEmpty()) {
            return;
        }

        // 执行下一个任务
        Integer moveSlotId = taskSet.removeFirst();
        ItemStack itemStack = getItemStackBySlotId(moveSlotId);
        if (itemStack.isEmpty()) {
            warning("想移动, 但格子空了 : " + moveSlotId);
            return;
        }

        // 检查空位
        if (isContainer(moveSlotId)) {
            if (isInvFull()) {
                warning("背包满了");
                clearQueue();
                return;
            }
        } else {
            if (isContainerFull()) {
                warning("容器满了");
                clearQueue();
                return;
            }
        }

        if (isDrapType) {
            InvUtils.drop().slotId(moveSlotId);
        } else {
            InvUtils.shiftClick().slotId(moveSlotId);
        }
        setDelay();
    }

    private void startSort() {
        if (!(mc.currentScreen instanceof HandledScreen<?>)) {
            return;
        }

        ScreenHandler handler = mc.player.currentScreenHandler;

        // 确定排序区域
        boolean containerOpen = isContainerOpen();
        if (!containerOpen) {
            warning("暂不支持对背包排序");
            return;
        }

        int size = getScreenMainSize();

        // 收集物品
        List<ItemStack> tmpItemStackList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            tmpItemStackList.add(handler.getSlot(i).getStack().copy());
        }

        // 合并相同物品
        List<ItemStack> correctItemStackList = mergeAndSortItemStacks(tmpItemStackList);

        /*for (int i = 0; i < correctItemStackList.size(); i++) {
            ItemStack itemStack = correctItemStackList.get(i);
            System.out.println(i + ":" + Names.get(itemStack) + "x" + itemStack.getCount());
        }*/

        // 清空其他任务，避免冲突
        clearQueue();

        // 逐个归位
        for (int targetSlotId = 0; targetSlotId < correctItemStackList.size(); targetSlotId++) {
            ItemStack targetSlotCorrect = correctItemStackList.get(targetSlotId);
            ItemStack targetSlotTmp = tmpItemStackList.get(targetSlotId);
            // 已是预期状态, 跳过
            if (isSameCount(targetSlotTmp, targetSlotCorrect)) {
                continue;
            }

            // 找到目标物品当前在哪里, 倒着找
            int sourceSlotId;
            for (sourceSlotId = size - 1; sourceSlotId >= 0; sourceSlotId--) {
                ItemStack sourceSlotTmp = tmpItemStackList.get(sourceSlotId);
                if (isSame(sourceSlotTmp, targetSlotCorrect)) {
                    // 超过需要需要排序的位置, 明显是未排序好的
                    if (sourceSlotId >= correctItemStackList.size()) {
                        break;
                    }
                    // 找到的物品应该是没有排序好的
                    ItemStack sourceSlotCorrect = correctItemStackList.get(sourceSlotId);
                    if (!isSame(sourceSlotTmp, sourceSlotCorrect)) {
                        break;
                    }
                }
            }

            // 没找到
            if (sourceSlotId < 0 || sourceSlotId == targetSlotId) {
                // warning("错误的状态1");
                continue;
            }

            // 第3步：把正确物品移到目标位置
            sortClickTaskList.add(sourceSlotId);
            sortClickTaskList.add(targetSlotId);

            ItemStack sourceSlotTmp = tmpItemStackList.get(sourceSlotId);
            // 更新预期状态
            tmpItemStackList.set(sourceSlotId, ItemStack.EMPTY);

            // 处理合并的情况
            ItemStack nextSourceSlotTmp;
            if (canMarge(targetSlotTmp, sourceSlotTmp)) {
                // 是合并的情况
                int supCount = targetSlotTmp.getMaxCount() - targetSlotTmp.getCount() - sourceSlotTmp.getCount();
                if (supCount < 0) {
                    // 有剩余
                    nextSourceSlotTmp = sourceSlotTmp.copyWithCount(-supCount);
                    // 更新预期状态
                    ItemStack itemStack = tmpItemStackList.get(targetSlotId);
                    itemStack.setCount(itemStack.getMaxCount());
                } else {
                    // 没有剩余
                    // 更新预期状态
                    ItemStack itemStack = tmpItemStackList.get(targetSlotId);
                    itemStack.increment(sourceSlotTmp.getCount());
                    // 若数量不够则还需要找后面的来合并归位
                    targetSlotId--;
                    continue;
                }
            } else {
                // 不是合并的情况
                nextSourceSlotTmp = targetSlotTmp;
                // 更新预期状态
                tmpItemStackList.set(targetSlotId, sourceSlotTmp);
                // 如果剩下的物品需要合并
                targetSlotId--;
            }

            // 第4步：处理被挤走的物品
            while (!nextSourceSlotTmp.isEmpty()) {
                // 找到被挤走物品在 correctItemStackList 中第一个出现的位置
                int nextTargetSlotId;
                for (nextTargetSlotId = targetSlotId + 1; nextTargetSlotId < correctItemStackList.size(); nextTargetSlotId++) {
                    ItemStack nextTargetSlotCorrect = correctItemStackList.get(nextTargetSlotId);
                    // 物品相同
                    if (nextTargetSlotCorrect.getItem() != nextSourceSlotTmp.getItem()) {
                        continue;
                    }
                    // 找到的物品应该是没有排序好的
                    ItemStack nextTargetSlotTmp = tmpItemStackList.get(nextTargetSlotId);
                    if (nextTargetSlotTmp.getItem() != nextTargetSlotCorrect.getItem()) {

                        sortClickTaskList.add(nextTargetSlotId);

                        // 没有合并, 交换物品
                        tmpItemStackList.set(nextTargetSlotId, nextSourceSlotTmp);
                        nextSourceSlotTmp = nextTargetSlotTmp;
                        break;
                    }
                    if (canMarge(nextTargetSlotTmp, nextSourceSlotTmp)) {
                        sortClickTaskList.add(nextTargetSlotId);
                        // 判断合并
                        int supCount = nextTargetSlotTmp.getMaxCount() - nextTargetSlotTmp.getCount() - nextSourceSlotTmp.getCount();
                        if (supCount < 0) {
                            // 有剩余
                            // 更新预期状态
                            nextTargetSlotTmp.setCount(nextTargetSlotTmp.getMaxCount());
                            nextSourceSlotTmp.setCount(-supCount);
                        } else {
                            // 没有剩余
                            // 更新预期状态
                            nextTargetSlotTmp.increment(nextSourceSlotTmp.getCount());
                            nextSourceSlotTmp = ItemStack.EMPTY;
                            // 若数量不够则还需要找后面的来合并归位
                            targetSlotId--;
                        }
                        break;
                    }
                }

                // 没找到
                if (nextTargetSlotId >= correctItemStackList.size()) {
                    warning("错误的状态2");
                    break;
                }
            }
        }

        // System.out.println(sortClickTaskList);
    }

    /**
     * 合并并排序物品堆叠
     */
    private List<ItemStack> mergeAndSortItemStacks(List<ItemStack> items) {
        // 使用Map来分组和合并相同物品
        Map<ItemBo, ItemStack> itemStackCountMap = new HashMap<>();

        // 第一遍：统计总数
        for (ItemStack itemStack : items) {
            if (itemStack.isEmpty()) {
                continue;
            }

            ItemBo key = new ItemBo(itemStack, true);
            ItemStack sumItemStack = itemStackCountMap.get(key);
            if (sumItemStack == null) {
                itemStackCountMap.put(key, itemStack.copy());
            } else {
                int newCount = sumItemStack.getCount() + itemStack.getCount();
                sumItemStack.setCount(newCount);
            }
        }

        // 第二遍：拆分回单个堆叠
        List<ItemStack> result = new ArrayList<>();
        for (Map.Entry<ItemBo, ItemStack> entry : itemStackCountMap.entrySet()) {
            ItemStack sumItemStack = entry.getValue();
            int maxStackCount = sumItemStack.getMaxCount();
            int supCount = sumItemStack.getCount();
            while (supCount >= maxStackCount) {
                ItemStack copy = sumItemStack.copyWithCount(maxStackCount);
                result.add(copy);
                supCount -= maxStackCount;
            }
            if (supCount > 0) {
                ItemStack copy = sumItemStack.copyWithCount(supCount);
                result.add(copy);
            }
        }

        result.sort(ITEM_ID_COUNT_COMPARATOR);

        return result;
    }

    @EventHandler
    private void onMouseClick(MouseClickEvent event) {
        if (!(mc.currentScreen instanceof HandledScreen<?>)) {
            return;
        }

        if (event.button != 0) {
            return;
        }

        // 检查当前鼠标位置的槽位
        Slot sourceSlot = getSlotAt(event.mouseX, event.mouseY, event.screenX, event.screenY);
        if (sourceSlot == null || !sourceSlot.hasStack()) {
            return;
        }

        boolean heldAlt = isHeldAlt();
        boolean heldShift = isHeldShift();
        if (!heldAlt && !heldShift) {
            // 记录用于丢弃物品
            drapItem = sourceSlot.getStack().getItem();
            return;
        }

        if (heldAlt) {
            boolean container = isContainer(sourceSlot.id);
            addTaskSameItem(container, sourceSlot.getStack());
            event.cancel();

        } else if (heldShift) {
            // addTask(sourceSlot);
        }
    }

    private void addTaskSameItem(boolean isContainer, ItemStack sourceStack) {
        DefaultedList<Slot> slots = mc.player.currentScreenHandler.slots;

        int start;
        int end;
        if (isContainer) {
            start = 0;
            end = getScreenMainSize();
        } else {
            start = getScreenMainSize();
            end = start + getPlayerMainSize();
        }

        Item sourceItem = sourceStack.getItem();

        // 如果是潜影盒，单独处理
        if (HeItemUtils.isShulkerBox(sourceItem)) {
            ShulkerBoxReader reader = new ShulkerBoxReader(sourceStack);
            Set<Item> sourceItemSet = reader.getItemSet();
            // 比较盒子内部物品
            for (int i = start; i < end; i++) {
                Slot slot = slots.get(i);
                ItemStack itemStack = slot.getStack();
                if (HeItemUtils.isShulkerBox(itemStack.getItem())) {
                    ShulkerBoxReader targetReader = new ShulkerBoxReader(itemStack);
                    Set<Item> targetItemSet = targetReader.getItemSet();
                    if (sourceItemSet.equals(targetItemSet)) {
                        addTask(slot);
                    }
                }
            }
            return;
        }

        // 普通物品，只比较物品类型
        for (int i = start; i < end; i++) {
            Slot slot = slots.get(i);
            ItemStack itemStack = slot.getStack();
            if (itemStack.getItem() == sourceItem) {
                addTask(slot);
            }
        }
    }

    @EventHandler
    private void onMouseDrag(MouseDragEvent event) {
        if (!scrollMove.get() || !(mc.currentScreen instanceof HandledScreen<?>)) {
            return;
        }

        // 不在滑动中 或者 没有打开容器
        if (!isHeldShift()) {
            return;
        }

        // 获取当前鼠标下的槽位
        Slot slot = getSlotAt(event.mouseX, event.mouseY, event.screenX, event.screenY);
        if (slot == null || !slot.hasStack()) {
            return;
        }

        // info("onMouseDrag");
        addTask(slot);
        event.cancel();
    }

    @EventHandler
    private void onMouseRelease(MouseReleaseEvent event) {
        // 处理鼠标释放
        if (event.button != 0) {
            return;
        }

        if (isHeldAlt()) {
            Slot currentSlot = getSlotAt(event.mouseX, event.mouseY, event.screenX, event.screenY);
            if (currentSlot == null && drapItem != null) {
                // TODO : 暂不可在箱子里使用
                if (!isContainerOpen()) {
                    Item item = drapItem;
                    clearQueue();
                    isDrapType = true;
                    // 构建一个临时的 ItemStack 用于比较
                    ItemStack tempStack = item.getDefaultStack();
                    addTaskSameItem(false, tempStack);
                    return;
                }
            }
        }
        isDrapType = false;
    }

    @EventHandler
    private void onScreenClose(ScreenCloseEvent event) {
        // 界面关闭时清空队列
        clearQueue();
    }

    /**
     * 检查容器界面是否打开
     */
    private boolean isContainerOpen() {
        // 检查是否为容器界面
        if (mc.currentScreen instanceof HandledScreen<?>) {
            // 排除创造模式背包
            return !(mc.player.currentScreenHandler instanceof PlayerScreenHandler);
        }
        return false;
    }

    /**
     * 获取鼠标位置下的槽位
     */
    private Slot getSlotAt(double mouseX, double mouseY, int screenX, int screenY) {
        ScreenHandler handler = mc.player.currentScreenHandler;
        for (Slot slot : handler.slots) {
            // 获取槽位在屏幕上的位置
            int slotX = slot.x + screenX;
            int slotY = slot.y + screenY;

            // 槽位大小为 18x18（包含边框）
            boolean isMouseOverSlot = mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18;
            if (isMouseOverSlot) {
                return slot;
            }
        }
        return null;
    }

    private void addTask(Slot slot) {
        // info("add :" + Names.get(slot.getStack()) + "," + slot.id);
        taskSet.add(slot.id);
    }

    private void clearQueue() {
        isDrapType = false;
        drapItem = null;
        taskSet.clear();
        sortClickTaskList.clear();
    }

    private boolean isHeldShift() {
        long handle = mc.getWindow().getHandle();
        return scrollMove.get() && GLFW.glfwGetKey(handle, scrollMoveKey.get().getValue()) == GLFW.GLFW_PRESS;
    }

    private boolean isHeldAlt() {
        long handle = mc.getWindow().getHandle();
        return sameClickMove.get() && GLFW.glfwGetKey(handle, sameClickMoveKey.get().getValue()) == GLFW.GLFW_PRESS;
    }

    public static boolean isSame(ItemStack left, ItemStack right) {
        if (left.getItem() != right.getItem()) {
            return false;
        }

        return isSameName(left, right);
    }

    public static boolean isSameCount(ItemStack left, ItemStack right) {
        if (left.getItem() != right.getItem()) {
            return false;
        }

        if (left.getCount() != right.getCount()) {
            return false;
        }

        return isSameName(left, right);
    }

    public static boolean canMarge(ItemStack left, ItemStack right) {
        if (left.getItem() != right.getItem()) {
            return false;
        }

        if (left.getCount() >= left.getMaxCount()) {
            return false;
        }

        return isSameName(left, right);
    }

    private static boolean isSameName(ItemStack left, ItemStack right) {
        Text leftName = left.getCustomName();
        Text rightName = right.getCustomName();
        if (leftName == null && rightName == null) {
            // 两个都没改名
            return true;
        } else if (leftName == null || rightName == null) {
            // 其中一个改名了
            return false;
        }

        return leftName.getString().equals(rightName.getString());
    }

    @Override
    public void onDeactivate() {
        clearQueue();
    }
}
