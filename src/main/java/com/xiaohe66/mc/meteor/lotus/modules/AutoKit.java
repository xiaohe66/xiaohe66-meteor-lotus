package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.bo.StorageItem;
import com.xiaohe66.mc.meteor.lotus.bo.StoragePos;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import com.xiaohe66.mc.meteor.lotus.util.ShulkerBoxReader;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 自动Kit - 基于展示框标识的Kit装配系统
 *
 * @author He
 * @since 2025.06.24 19:32
 */
public class AutoKit extends WalkModule {

    private final Setting<Integer> scanRange = sgGeneral.add(new IntSetting.Builder()
            .name("扫描范围")
            .description("检测展示框和箱子的范围")
            .defaultValue(50)
            .min(1)
            .sliderMax(100)
            .build());

    private final Setting<Integer> qty = sgGeneral.add(new IntSetting.Builder()
            .name("操作数")
            .description("每次操作的物品数量")
            .min(1)
            .sliderMax(9)
            .defaultValue(9)
            .build());

    private final Setting<Item> emptyKitItem = sgGeneral.add(new ItemSetting.Builder()
            .name("空盒标识")
            .description("空盒箱子展示框上的物品")
            .defaultValue(Items.WHITE_SHULKER_BOX)
            .build());

    private final Setting<Item> finishedKitItem = sgGeneral.add(new ItemSetting.Builder()
            .name("成品标识")
            .description("成品kit箱子展示框上的物品")
            .defaultValue(Items.LIGHT_BLUE_SHULKER_BOX)
            .build());

    private final Setting<Item> miscKitItem = sgGeneral.add(new ItemSetting.Builder()
            .name("杂盒标识")
            .description("杂盒箱子展示框上的物品")
            .defaultValue(Items.BLUE_SHULKER_BOX)
            .build());

    private final Setting<Boolean> initBtn = sgGeneral.add(new BoolSetting.Builder()
            .name("初始化")
            .description("使用前需要先初始化，保存所有箱子位置并读取主手盒子作为模板")
            .defaultValue(false)
            .onChanged(this::init)
            .build());

    // ==================== 运行时数据 ====================

    private final Map<ItemBo, StoragePos> itemPosMap = new LinkedHashMap<>();
    private StoragePos emptyKitPos;
    private StoragePos finishedKitPos;
    private StoragePos miscKitPos;

    private final List<ItemBo> templateItemList = new ArrayList<>(27);
    /**
     * 拿之前的 index
     */
    private int takeItemIndex = 0;

    /**
     * 放之前的 index 放物品的下标, 表示处理到模板的第几格
     */
    private int putItemIndex = 0;
    private ItemBo currentItem;
    private StoragePos curOperationPos;
    private int putKitIndex = -1;

    public AutoKit() {
        super("自动Kit", "自动装配Kit, 需要在<简易仓库>中使用。由资深猎人<dadou>友情赞助开发。");

        addStep(Steps.NEXT, this::nextStep);
        addStep(Steps.PUT_KIT, this::putKit);
        addStep(Steps.PUT_ITEM, this::putItem);
        addStep(Steps.TAKE_ITEM, this::takeItem);
        addStep(Steps.PLACE_EMPTY_KIT, this::placeEmptyKit);
        addStep(Steps.TAKE_EMPTY_KIT, this::takeEmptyKit);
        addStep(Steps.PLACE_KIT, this::placeKit);
        addStep(Steps.TAKE_KIT, this::takeKit);
        addStep(Steps.BREAK_KIT, this::breakKit);
    }

    /**
     * 初始化方法 - 扫描展示框保存位置，读取主手盒子作为模板
     */
    private void init(Boolean enabled) {
        if (!Boolean.TRUE.equals(enabled)) {
            return;
        }

        // 重置按钮状态
        initBtn.set(false);

        if (mc.player == null || mc.world == null) {
            warning("玩家或世界未加载");
            return;
        }

        clear();

        // 1. 扫描展示框识别所有存储位置
        boolean scanPositionsSuccess = scanPositions();
        if (!scanPositionsSuccess) {
            clear();
            return;
        }

        // 2. 读取主手盒子作为模板
        boolean readTemplateSuccess = readTemplateFromMainHand();
        if (!readTemplateSuccess) {
            clear();
            return;
        }

        // 检查是否所有模板物品都有对应位置
        boolean missItem = false;
        for (ItemBo itemBo : templateItemList) {
            if (!itemPosMap.containsKey(itemBo)) {
                warning("未找到物品<" + itemBo.getName() + ">的存储位置");
                missItem = true;
            }
        }

        if (missItem) {
            clear();
            return;
        }

        // 输出检测到的物品
        String itemNames = itemPosMap.keySet().stream()
                .map(ItemBo::getName)
                .collect(Collectors.joining(","));

        info("成功识别: " + itemNames);
        info("初始化完毕！");
    }

    @Override
    public void onActivate() {
        super.onActivate();

        // 检查是否已初始化
        if (itemPosMap.isEmpty() || emptyKitPos == null || finishedKitPos == null) {
            warning("请先进行初始化");
            toggle();
            return;
        }

        // 清空运行时数据
        takeItemIndex = 0;
        putItemIndex = 0;
        currentItem = null;
        curOperationPos = null;

        nextStep();
    }

    /**
     * 决策下一步操作
     */
    private void nextStep() {
        // 检查背包中是否有装满的潜影盒需要存放
        ItemStack nextKit = nextPlayerStack(itemStack -> HeItemUtils.isShulkerBox(itemStack.getItem()));
        if (!nextKit.isEmpty()) {
            putKitIndex = getCurPlayerSlot();
            ShulkerBoxReader reader = new ShulkerBoxReader(nextKit);
            if (reader.matchesTemplate(templateItemList)) {
                curOperationPos = finishedKitPos;
                gotoTargetIfNeed(curOperationPos.getBtnPos(), 0, Steps.PUT_KIT, "存放成品kit");

            } else if (reader.isEmpty()) {
                // 记录拿空盒放置的时候，不要走 next 方法, 否则会在这里循环
                curOperationPos = emptyKitPos;
                gotoTargetIfNeed(curOperationPos.getBtnPos(), 0, Steps.PUT_KIT, "存放空盒");

            } else {
                // 单一物品放到对应箱子中
                if (reader.isSameItem()) {
                    ItemBo firstItemBo = reader.getFirstItemBo();
                    if (itemPosMap.containsKey(firstItemBo)) {
                        curOperationPos = itemPosMap.get(firstItemBo);
                        gotoTargetIfNeed(curOperationPos.getBtnPos(), 0, Steps.PUT_KIT, "存放满盒");
                        return;
                    }
                }
                curOperationPos = miscKitPos;
                gotoTargetIfNeed(curOperationPos.getBtnPos(), 0, Steps.PUT_KIT, "存放杂盒");
            }
            return;
        }

        // 检查地上是否有kit
        List<ItemEntity> shulkerEntities = mc.world.getEntitiesByClass(
                ItemEntity.class,
                mc.player.getBoundingBox().expand(5),
                e -> HeItemUtils.isShulkerBox(e.getStack().getItem()));

        if (!shulkerEntities.isEmpty()) {
            // 捡kit
            BlockPos targetPos = shulkerEntities.getFirst().getBlockPos();
            if (targetPos.getY() != mc.player.getBlockY()) {
                BlockPos testPos = new BlockPos(targetPos.getX(), mc.player.getBlockY(), targetPos.getZ());
                BlockPos canStandPos = HeBlockUtils.getCanStandPos(testPos, 1);
                if (canStandPos != null) {
                    targetPos = canStandPos;
                }
            }
            gotoTargetIfNeed(targetPos, 0, Steps.NEXT, "捡kit");
            return;
        }

        // 放身上的物品
        step = Steps.PUT_ITEM;
    }

    /**
     * 存放kit到对应箱子
     */
    private void putKit() {
        if (notInOperationRange(curOperationPos)) {
            gotoBtnPos(curOperationPos, "<存kit>距离不够，尝试移动", Steps.PUT_KIT);
            return;
        }

        // 找到背包中的潜影盒
        ItemStack kitItemStack = getItemStack(putKitIndex);
        if (!HeItemUtils.isShulkerBox(kitItemStack.getItem())) {
            // 是放kit的状态, 但是身上却没有kit
            warning("放kit, 但身上没有...");
            step = Steps.NEXT;
            return;
        }

        openChest(curOperationPos.getPutPos(), inventory -> {
            // TODO : 这个是找箱子的, 还是找玩家背包的？
            FindItemResult emptyResult = InvUtils.findEmpty();
            if (emptyResult.found()) {
                // 放入
                InvUtils.shiftClick().slot(putKitIndex);
                delayCloseNext(Steps.NEXT);

            } else {
                // 满了, 中断操作
                breakStep("<存kit>箱满，无法存放");
            }
        });
    }

    /**
     * 拿空盒
     */
    private void takeEmptyKit() {
        if (notInOperationRange(emptyKitPos)) {
            gotoBtnPos(emptyKitPos, "<拿空盒>距离不够，尝试移动", Steps.TAKE_EMPTY_KIT);
            return;
        }

        openChest(emptyKitPos.getTakePos(), inventory -> {
            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemStack itemStack = inventory.getStack(slot);
                if (!HeItemUtils.isShulkerBox(itemStack.getItem())) {
                    continue;
                }

                ShulkerBoxReader reader = new ShulkerBoxReader(itemStack);
                if (reader.isEmpty()) {
                    info("拿取空盒");
                    InvUtils.shiftClick().slotId(slot);
                    delayCloseNext(Steps.PLACE_EMPTY_KIT);
                    return;
                }
            }

            breakStep("<空盒箱>缺少空潜影盒");
        });
    }

    /**
     * 放置空盒
     * 1. 判断是否存在kit, 若存在则 next
     * 2. 判断身上是否存在空kit, 若不存在则去拿空kit
     * 3. 判断距离放空kit是否够, 不够需要移动过去
     * 4. 放置空kit, 设置next
     */
    private void placeEmptyKit() {
        BlockState finishedKiBlockState = mc.world.getBlockState(finishedKitPos.getKitPos());

        // 1. 判断是否存在kit, 若存在则 next
        if (finishedKiBlockState.getBlock() instanceof ShulkerBoxBlock) {
            // kit已存在
            step = Steps.PUT_ITEM;
            return;
        }

        // 检查位置是否被其他方块占用
        if (!finishedKiBlockState.isAir()) {
            breakStep("<位置>被占用: " + Names.get(finishedKitPos.getItem().getItem()));
            return;
        }

        // 2. 判断身上是否存在空kit, 若不存在则去拿空kit
        FindItemResult findItemResult = HeInvUtils.findShulkerBox(Items.AIR);
        if (!findItemResult.found()) {
            // 身上没有空盒，去拿空盒
            step = Steps.TAKE_EMPTY_KIT;
            return;
        }

        // 空盒需要在快捷栏上才能放置
        if (!findItemResult.isMainHand()) {
            // 将空盒切换到主手
            HeInvUtils.swap(findItemResult.slot(), getMainSlot());
            setDelay();
            return;
        }

        // 3. 判断距离放空kit是否够, 不够需要移动过去
        if (notInOperationRange(finishedKitPos)) {
            gotoTarget(finishedKitPos.getBtnPos(), 0, Steps.PLACE_EMPTY_KIT);
            return;
        }

        // 4. 放置空kit, 设置next
        info("放置空盒");
        HeBlockUtils.place(finishedKitPos.getKitPos(), getMainSlot(), true, Direction.DOWN);

        // 重置放置索引，准备开始装配
        putItemIndex = 0;
        // currentTakeIndex = 0;
        // setCurrent(0);
        delayNext(Steps.PUT_ITEM);
    }

    /**
     * 从箱子拿取对应物品的潜影盒
     */
    private void takeKit() {
        if (notInOperationRange(curOperationPos)) {
            gotoBtnPos(curOperationPos, "<拿kit>距离不够，尝试移动", Steps.TAKE_KIT);
            return;
        }

        openChest(curOperationPos.getTakePos(), inventory -> {
            ItemStack nextScreenStack = nextScreenStack(itemStack -> {
                if (HeItemUtils.isShulkerBox(itemStack.getItem())) {
                    ShulkerBoxReader reader = new ShulkerBoxReader(itemStack);
                    return reader.hasItem(currentItem);
                }
                return false;
            });

            if (nextScreenStack.isEmpty()) {
                breakStep("<" + currentItem.getName() + ">库存不足");
                return;
            }

            info("拿取kit: " + currentItem.getName());
            InvUtils.shiftClick().slotId(getCurScreenSlot());

            delayCloseNext(Steps.PLACE_KIT);
        });
    }

    /**
     * 放置物品kit到世界
     * 1. 检查地上是否有kit, 有则 take_item
     * 2. 检查身上是否有物品kit, 没有则 take_kit
     * 3. 检查距离是否够, 不够则移动
     * 4. 放置, take_item
     */
    private void placeKit() {
        BlockState finishedKitBlockState = mc.world.getBlockState(curOperationPos.getKitPos());
        if (!finishedKitBlockState.isAir()) {
            // 检查是否是潜影盒
            if (finishedKitBlockState.getBlock() instanceof ShulkerBoxBlock) {
                step = Steps.TAKE_ITEM;

            } else {
                breakStep("<kit位置>被占用");
            }
            return;
        }
        // 检查身上是否存在kit
        ItemStack nextItemStack = nextPlayerStack(itemStack -> HeItemUtils.isShulkerBox(itemStack.getItem()));
        if (nextItemStack.isEmpty()) {
            // 身上没有, 去拿kit
            step = Steps.TAKE_KIT;
            return;
        }

        // 判断距离是否够, 不够则需要移动过去
        if (notInOperationRange(curOperationPos)) {
            gotoBtnPos(curOperationPos, "去拿kit", Steps.PLACE_KIT);
            return;
        }

        // 切主手
        if (swapToMainHand(getCurPlayerSlot())) {
            return;
        }

        // info("放置盒子:" + Names.get(currentItem));
        info("放置盒子:" + Names.get(curOperationPos.getItem().getItem()));
        // info("放置盒子:" + Names.get(templateItemList.get(takeItemIndex)));
        HeBlockUtils.place(curOperationPos.getKitPos(), getMainSlot(), true, Direction.DOWN);

        delayNext(Steps.TAKE_ITEM);
    }

    /**
     * 从kit中拿取物品
     */
    private void takeItem() {
        // 检查是否需要停止拿取（已达到操作数限制）
        // TODO : 和完成后的重复检查了, 考虑优化
        if (takeItemIndex >= templateItemList.size() || takeItemIndex - putItemIndex >= qty.get()) {
            step = Steps.PUT_ITEM;
            return;
        }

        // TODO : 检查身上空格？

        // 检查是否存在kit
        BlockState finishedKitBlockState = mc.world.getBlockState(curOperationPos.getKitPos());
        if (finishedKitBlockState.isAir()) {
            // 不存在, 需要放盒子
            step = Steps.PLACE_KIT;
            return;
        }

        // 检查是否是潜影盒
        if (!(finishedKitBlockState.getBlock() instanceof ShulkerBoxBlock)) {
            breakStep("<kit位置>被占用");
            return;
        }

        // 判断距离是否够, 不够则需要移动过去
        if (notInOperationRange(curOperationPos)) {
            gotoTarget(curOperationPos.getBtnPos(), 0, Steps.TAKE_ITEM);
            return;
        }

        openKit(curOperationPos.getKitPos(), screenHandler -> {
            // 只拿满组物品
            ItemStack nextItemStack = nextScreenStack(itemStack -> currentItem.isSameItem(itemStack) && itemStack.getCount() == currentItem.getItem().getMaxCount());
            if (nextItemStack.isEmpty()) {
                // 盒子里没有了，需要挖掉并重新拿取
                info("kit已空，挖掉重放: " + currentItem.getName());
                delayCloseNext(Steps.BREAK_KIT);
                return;
            }

            info("拿取物品: " + currentItem.getName());
            InvUtils.shiftClick().slotId(getCurScreenSlot());

            takeItemIndex++;

            // 模板的物品都拿完了, 去放物品
            if (takeItemIndex >= templateItemList.size() || takeItemIndex - putItemIndex >= qty.get()) {
                delayCloseNext(Steps.PUT_ITEM);
            }
            // TODO : 当相同的物品不连续时, 当前逻辑会分多次去拿, 可以考虑优化成一次拿取
            else if (templateItemList.get(takeItemIndex).equals(currentItem)) {
                // 不用关闭kit, 接着拿
                setDelay();

            } else {
                // 下一个物品
                setTakeItem(takeItemIndex);
            }
        });
    }

    /**
     * 将物品放入空盒
     * 1. 检查身上是否存在需要被存放的物品, 若不存在则设置 next 并跳过当前
     * 2. 当有多个要存放的物品时, 取最近的那个
     * 3. 判断这个物品存放位置是否存在kit, 若不存在则需要去拿空盒, 然后走放置kit流程
     * 4. 判断距离是否够, 不够则需要移动过去
     * 5. 打开kit, 判断是否满了, 若满了则需要打挖点(点击按钮), 然后去拿空盒, 再走放置kit流程
     * 6. 一次存放身上的物品到kit中, 直到放完
     * 7. 设置 next
     */
    private void putItem() {
        // 获取当前需要放入的物品
        ItemBo putItem = templateItemList.get(putItemIndex);

        // 1. 检查身上是否存在需要被存放的物品, 若不存在则设置 next 并跳过当前
        ItemStack nextItemStack = nextPlayerStack(itemStack -> putItem.isSameItem(itemStack) && itemStack.getCount() == itemStack.getMaxCount());
        if (nextItemStack.isEmpty()) {
            // 身上没有, 去拿物品
            closeScreen();
            // 若因为特殊原因导致身上多了一些物品, 在没有拿的时候就放了一些物品了, 就会导致 takeItemIndex 小于 putItemIndex 的情况
            if (putItemIndex > takeItemIndex) {
                takeItemIndex = putItemIndex;
            }
            setTakeItem(putItem);
            return;
        }

        // 2. 检查空盒位置是否存在kit
        BlockState finishedKitBlockState = mc.world.getBlockState(finishedKitPos.getKitPos());
        if (finishedKitBlockState.isAir()) {
            // 不存在, 需要去拿空盒, 然后走放置kit流程
            curOperationPos = finishedKitPos;
            closeScreen();
            step = Steps.PLACE_EMPTY_KIT;
            return;
        }

        // 检查是否是潜影盒
        if (!(finishedKitBlockState.getBlock() instanceof ShulkerBoxBlock)) {
            closeScreen();
            breakStep("<成品位置>被占用");
            return;
        }

        // 4. 判断距离是否够, 不够则需要移动过去
        if (notInOperationRange(finishedKitPos)) {
            closeScreen();
            gotoBtnPos(finishedKitPos, "<放" + putItem.getName() + ">距离不够, 尝试移动", Steps.PUT_ITEM);
            return;
        }

        // 5. 打开kit, 判断是否满了, 若满了则需要打挖点(点击按钮), 然后去拿空盒, 再走放置kit流程
        openKit(finishedKitPos.getKitPos(), screenHandler -> {
            // 检查kit是否已满
            if (hasScreenFull()) {
                setBreakFullKit();
                return;
            }

            // 对应格子是空的时候才放
            ItemStack itemStack = screenHandler.getSlot(putItemIndex).getStack();
            if (itemStack.isEmpty()) {
                InvUtils.shiftClick().slot(getCurPlayerSlot());
            }

            putItemIndex++;

            // 7. 设置 next
            if (putItemIndex >= templateItemList.size()) {
                // 所有物品已放置完成, 需要挖掉
                setBreakFullKit();
                // } else if (putItemIndex >= takeItemIndex) {
                // 拿的物品已经放好了, 需要去拿新的
                // delayCloseNext(Steps.TAKE_ITEM);
            } else if (!itemStack.isEmpty()) {
                // 还有物品需要放置，继续放置。不需要检查物品类型, 因为都是放到同一个kit里
                setDelay();
            }
        });
    }

    /**
     * 挖掉潜影盒
     * <p>
     * 1. 挖掉之后去捡东西(不考虑路径优先, 这样简单点)
     * 2. 捡东西后, 去 next
     */
    private void breakKit() {
        // 检查距离
        if (notInOperationRange(curOperationPos)) {
            gotoBtnPos(curOperationPos, "<挖盒子>距离不够, 尝试移动", Steps.BREAK_KIT);
            return;
        }

        // 计算点击位置
        Vec3d hitPos = Vec3d.ofCenter(curOperationPos.getBtnPos()).add(0, -0.5, 0);

        // 创建 BlockHitResult
        BlockHitResult hitResult = new BlockHitResult(hitPos, Direction.UP, curOperationPos.getBtnPos(), false);

        // 创建并发送交互数据包
        PlayerInteractBlockC2SPacket packet = new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0);

        // 点击按钮推掉盒子
        mc.getNetworkHandler().sendPacket(packet);

        step = Steps.NEXT;

        setDelay(40);
    }

    /**
     * 扫描展示框识别所有存储位置（用于初始化）
     */
    private boolean scanPositions() {
        List<ItemFrameEntity> itemFrames = mc.world.getEntitiesByClass(
                ItemFrameEntity.class,
                mc.player.getBoundingBox().expand(scanRange.get()),
                frame -> !frame.getHeldItemStack().isEmpty());

        int playerY = mc.player.getBlockPos().getY();
        Vec3d playerPos = mc.player.getEntityPos();

        for (ItemFrameEntity frame : itemFrames) {
            BlockPos attachedBlockPos = frame.getAttachedBlockPos();

            // 范围限制：只需要玩家相同或高4格范围内的
            if (attachedBlockPos.getY() < playerY || attachedBlockPos.getY() > playerY + 4) {
                continue;
            }

            // 距离限制
            double distance = attachedBlockPos.toCenterPos().distanceTo(playerPos);
            if (distance > scanRange.get()) {
                continue;
            }

            ItemStack frameHeldItemStack = frame.getHeldItemStack();
            ItemBo itemBo = new ItemBo(frameHeldItemStack);
            Item item = itemBo.getItem();

            StoragePos pos = checkAndBuildStoragePos(frame, StorageItem.valueOf(itemBo));
            if (pos == null) {
                continue;
            }

            // 识别空盒位置
            if (item == emptyKitItem.get()) {
                emptyKitPos = keepNearer(pos, emptyKitPos, playerPos);
            }
            // 识别成品位置
            else if (item == finishedKitItem.get()) {
                finishedKitPos = keepNearer(pos, finishedKitPos, playerPos);
            }
            // 识别杂盒位置
            else if (item == miscKitItem.get()) {
                miscKitPos = keepNearer(pos, miscKitPos, playerPos);
            }
            // 普通物品位置（非潜影盒）
            else if (!HeItemUtils.isShulkerBox(item)) {
                StoragePos oldPos = itemPosMap.get(itemBo);
                StoragePos nearerPos = keepNearer(pos, oldPos, playerPos);
                if (nearerPos == pos) {
                    itemPosMap.put(itemBo, pos);
                }
            }
        }

        // 验证必要位置
        if (emptyKitPos == null) {
            warning("未检测到<空盒位置>，请在对应展示框放置" + Names.get(emptyKitItem.get()));
            return false;
        }
        if (finishedKitPos == null) {
            warning("未检测到<成品位置>，请在对应展示框放置" + Names.get(finishedKitItem.get()));
            return false;
        }
        if (miscKitPos == null) {
            warning("未检测到<杂盒位置>，请在对应展示框放置" + Names.get(miscKitItem.get()));
            return false;
        }
        if (itemPosMap.isEmpty()) {
            warning("未检测到<物品>位置>");
            return false;
        }

        return true;
    }

    /**
     * 从主手读取模板
     */
    private boolean readTemplateFromMainHand() {
        ItemStack mainHandStack = mc.player.getMainHandStack();
        // 检查主手是否持有潜影盒
        if (!HeItemUtils.isShulkerBox(mainHandStack.getItem())) {
            warning("主手未持有潜影盒，请手持模板盒子后重新初始化");
            return false;
        }
        ShulkerBoxReader reader = new ShulkerBoxReader(mainHandStack);
        if (reader.isEmpty()) {
            warning("主手潜影盒为空，请放入模板物品后重新初始化");
            return false;
        }

        // 读取盒子中的物品作为模板（按原始顺序）
        templateItemList.clear();
        for (ItemStack itemStack : reader) {
            if (!itemStack.isEmpty()) {
                templateItemList.add(new ItemBo(itemStack));
            }
        }

        if (templateItemList.isEmpty()) {
            warning("未能从主手盒子读取到模板物品");
            return false;
        }

        return true;
    }

    /**
     * 保留距离玩家更近的位置
     *
     * @return 如果 newPos 更近则返回 newPos，否则返回 oldPos
     */
    private StoragePos keepNearer(StoragePos newPos, StoragePos oldPos, Vec3d playerPos) {
        if (oldPos == null) {
            return newPos;
        }
        double newDist = newPos.getBtnPos().getSquaredDistance(playerPos);
        double oldDist = oldPos.getBtnPos().getSquaredDistance(playerPos);
        return newDist < oldDist ? newPos : oldPos;
    }

    /**
     * 检查并构建位置信息
     */
    // ==================== 辅助方法 ====================
    private void setTakeItem(int index) {
        currentItem = templateItemList.get(index);
        curOperationPos = itemPosMap.get(currentItem);
        delayCloseNext(Steps.TAKE_ITEM);
    }

    private void setTakeItem(ItemBo putItem) {
        currentItem = putItem;
        curOperationPos = itemPosMap.get(putItem);
        delayCloseNext(Steps.TAKE_ITEM);
    }

    private void setBreakFullKit() {
        info("kit已满，挖掉存放");
        takeItemIndex = 0;
        putItemIndex = 0;
        curOperationPos = finishedKitPos;
        delayCloseNext(Steps.BREAK_KIT);
    }

    private void clear() {
        itemPosMap.clear();
        emptyKitPos = null;
        finishedKitPos = null;
        miscKitPos = null;
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();

        takeItemIndex = 0;
        putItemIndex = 0;
        currentItem = null;
        curOperationPos = null;

        HeInvUtils.closeCurScreen();
    }
}