package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.util.Const;
import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeRotationUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.Block;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * @author He
 * @since 2025.05.11 15:32
 */
public class HunterSupply extends Module {

    private static final Logger log = LoggerFactory.getLogger(HunterSupply.class);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("延迟")
            .description("操作延迟（刻）")
            .defaultValue(2)
            .min(0)
            .build());

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("范围")
            .description("末影箱的放置范围、盒子的搜索范围")
            .defaultValue(3)
            .min(0)
            .build());

    private final Setting<Integer> kitRange = sgGeneral.add(new IntSetting.Builder()
            .name("盒子范围")
            .description("范围")
            .defaultValue(1)
            .min(0)
            .build());

    private final Setting<Integer> elytraQty = sgGeneral.add(new IntSetting.Builder()
            .name("鞘翅数量")
            .description("鞘翅的补货量")
            .defaultValue(3)
            .range(0, 27)
            .build());

    private final Setting<Integer> elytraDamaged = sgGeneral.add(new IntSetting.Builder()
            .name("鞘翅损坏阈值")
            .description("剩余多少耐久被视为损坏")
            .defaultValue(5)
            .range(0, 20)
            .build());

    private final VoxelShape enderChestShape = Block.createCuboidShape(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
    private final VoxelShape kitShape = Block.createCuboidShape(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);

    private BlockPos enderChestPos = null;
    private BlockPos kitPos = null;

    private Stage stage = Stage.PlaceEnderChest;
    private int waitTicks;

    private int needElytraQty;
    private int takeIndex;
    private float progress;
    private boolean mineKitIng;
    private FindItemResult pickResult;

    public HunterSupply() {
        super(Const.CATEGORY, "!猎人补给", "猎人补给(未完成), 当前仅可补给鞘翅, 计划补给鞘翅、烟花、图腾、黑曜石等猎人物资");
    }

    @Override
    public void onActivate() {

        // updateNeedElytraQty();

        // waitTicks = 0;
        // stage = Stage.FindKit;
    }

    // @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!isActive()) {
            return;
        }

        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        try {
            switch (stage) {
                case FindKit -> findKit();
                case FindEnderChest -> findEnderChest();
                case PlaceEnderChest -> placeEnderChest();
                case OpenEnderChest -> openEnderChest();
                case TakeKit -> takeKit();
                case SwapKit -> swapKitToMainHand();
                case PlaceKit -> placeKit();
                case OpenKit -> openKit();
                case TakeItems -> takeItems();
                case PickUpKit -> pickUpKit();
                case MineKit -> mineKit();
                case Complete -> complete();
            }

        } catch (Exception e) {
            log.error("发生未知错误, state : {}", this.stage, e);
            error("发生未知错误: " + e.getMessage());
            toggle();
        }
    }

    private void findEnderChest() {
        int rangeValue = range.get();

        BlockPos bestPos = null;
        double distance = 100;

        List<BlockPos> blockPosArrayList = HeBlockUtils.listPosInSphere(rangeValue, mc.player.getBlockPos());
        for (BlockPos blockPos : blockPosArrayList) {
            if (!mc.world.isAir(blockPos.up())) {
                continue;
            }
            if (mc.world.getBlockState(blockPos).getBlock() instanceof EnderChestBlock) {
                this.enderChestPos = blockPos;
                this.stage = Stage.OpenEnderChest;
                return;
            }
            if (!BlockUtils.canPlace(blockPos, true)) {
                continue;
            }

            if (bestPos == null || MathHelper.sqrt((float) mc.player.squaredDistanceTo(blockPos.toCenterPos())) < distance) {
                distance = MathHelper.sqrt((float) mc.player.squaredDistanceTo(blockPos.toCenterPos()));
                bestPos = blockPos;
            }
        }

        this.enderChestPos = bestPos;
        this.stage = Stage.PlaceEnderChest;
    }

    private void placeEnderChest() {
        if (enderChestPos == null) {
            error("未找到放置末影箱合适的位置");
            // TODO : 应该操作移动，直到找到合适的位置为止
            toggle();
            return;
        }

        if (!BlockUtils.canPlace(enderChestPos, true)) {
            this.stage = Stage.FindEnderChest;
            return;
        }

        FindItemResult enderChestResult = HeInvUtils.findAndMoveHotbar(Items.ENDER_CHEST);
        if (enderChestResult == null) {
            error("背包中没有末影箱");
            toggle();
            return;
        }

        HeRotationUtils.rotate(enderChestPos);
        BlockUtils.place(enderChestPos, enderChestResult, true, 0, true);

        stage = Stage.OpenEnderChest;
    }

    private void openEnderChest() {
        if (!(mc.world.getBlockState(enderChestPos).getBlock() instanceof EnderChestBlock)) {
            error("末影箱放置失败");
            stage = Stage.PlaceEnderChest;
            return;
        }

        // HeBlockUtils.open(enderChestPos);

        stage = Stage.TakeKit;
    }

    private void takeKit() {
        // 在末影箱中查找潜影盒
        if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler screenHandler) {
            for (int slotIndex = 0; slotIndex < 27; slotIndex++) {
                Slot slotObj = screenHandler.getSlot(slotIndex);
                ItemStack stack = slotObj.getStack();
                if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {

                    // log.info("ShulkerBox slotIndex : {}", slotIndex);

                    // 检查潜影盒内是否有鞘翅
                    if (containsElytra(stack, false)) {

                        // 移动
                        FindItemResult emptyItemResult = InvUtils.findEmpty();
                        if (!emptyItemResult.found()) {
                            error("没有空位, 待实现自动丢垃圾");
                            toggle();
                            return;
                        }

                        int emptyItemSlot = emptyItemResult.slot();
                        if (emptyItemResult.isHotbar()) {
                            // NOTE : 从箱子里拿到身上
                            InvUtils.move().fromId(slotIndex).to(emptyItemSlot);

                        } else {
                            // 不在工具栏
                            // TODO : 主手如果是稿子，那不是会影响后面的挖吗？
                            InvUtils.move().fromId(slotIndex).to(HeInvUtils.getMainSlot());
                            InvUtils.click().to(emptyItemSlot);
                        }

                        HeInvUtils.closeCurScreen();

                        // TODO : 如果身上有kit, 还需要放回去

                        if (needElytraQty > 0) {
                            this.stage = Stage.SwapKit;

                        } else {
                            complete();
                        }

                        return;
                    }
                }
            }

            error("未找到含有鞘翅的潜影盒");
            toggle();
            return;
        }

        stage = Stage.OpenEnderChest;
    }

    private void findKit() {
        int rangeValue = range.get();

        List<BlockPos> blockPosArrayList = HeBlockUtils.listPosInSphere(rangeValue, mc.player.getBlockPos());
        for (BlockPos blockPos : blockPosArrayList) {
            if (mc.world.getBlockState(blockPos).getBlock() instanceof ShulkerBoxBlock) {

                this.kitPos = blockPos;
                next(Stage.OpenKit);
                return;
            }
        }

        next(Stage.FindEnderChest);
    }

    private void swapKitToMainHand() {
        FindItemResult findElytraResult = InvUtils.find(itemstack -> containsElytra(itemstack, false));

        if (findElytraResult.found()) {
            int slot = findElytraResult.slot();

            log.info("swapKitToMainHand, source slot : {}", slot);

            HeInvUtils.swapMainHand(slot);

            next(Stage.PlaceKit);

        } else {

            next(Stage.FindKit);
        }
    }

    private void placeKit() {

        if (!HeInvUtils.isKitInMainHand()) {
            log.error("placeKit main hand is not kit : {}", HeInvUtils.getMainSlot());
            this.stage = Stage.SwapKit;
            return;
        }

        int rangeValue = kitRange.get();

        BlockPos bestPos = null;
        double distance = 100;

        List<BlockPos> blockPosArrayList = HeBlockUtils.listPosInSphere(rangeValue, mc.player.getBlockPos());
        for (BlockPos blockPos : blockPosArrayList) {
            if (!mc.world.isAir(blockPos.up())) {
                continue;
            }

            if (mc.world.getBlockState(blockPos).getBlock() instanceof ShulkerBoxBlock) {
                info("地上已存在kit");
                this.kitPos = blockPos;
                next(Stage.OpenKit);
                return;
            }
            if (!BlockUtils.canPlace(blockPos, true)) {
                continue;
            }

            if (bestPos == null || MathHelper.sqrt((float) mc.player.squaredDistanceTo(blockPos.toCenterPos())) < distance) {
                distance = MathHelper.sqrt((float) mc.player.squaredDistanceTo(blockPos.toCenterPos()));
                bestPos = blockPos;
            }
        }


        if (bestPos == null) {
            error("未找到放置kit合适的位置");
            // TODO : 应该操作移动，直到找到合适的位置为止
            toggle();
            return;
        }

        this.kitPos = bestPos;

        HeRotationUtils.rotate(kitPos);

        log.info("放置kit : {}", kitPos);

        FindItemResult findItemResult = new FindItemResult(HeInvUtils.getMainSlot(), 1);
        BlockUtils.place(bestPos, findItemResult, true, 0, true);

        next(Stage.OpenKit);
    }

    private void openKit() {
        if (!(mc.world.getBlockState(kitPos).getBlock() instanceof ShulkerBoxBlock)) {
            error("kit放置失败");

            next(Stage.PlaceKit);
            return;
        }

        // HeBlockUtils.open(kitPos);

        this.takeIndex = 0;

        updateNeedElytraQty();
        next(Stage.TakeItems);
    }

    private void takeItems() {
        if (!(mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler)) {
            log.error("打开kit失败");
            next(Stage.OpenKit);
            return;
        }

        // TODO : 当地上有kit时, 不会拿

        // 获取耐久只有2的鞘翅的命令
        // /give @p minecraft:elytra[minecraft:damage=430]

        if (takeIndex < 27 && needElytraQty > 0) {
            ItemStack stack = mc.player.currentScreenHandler.getSlot(takeIndex).getStack();
            if (stack.getItem() == Items.ELYTRA) {

                FindItemResult damagedElytraResult = findElytra(true);

                // 有损坏的鞘翅, 交换
                if (damagedElytraResult.found()) {
                    InvUtils.move().from(damagedElytraResult.slot()).toId(takeIndex);
                    InvUtils.click().to(damagedElytraResult.slot());
                }
                // 没有损坏的鞘翅
                else {
                    // 需要提前清出一个空位吗？
                    FindItemResult emptyResult = InvUtils.findEmpty();
                    if (emptyResult.found()) {
                        log.info("move : {}->{}", takeIndex, emptyResult.slot());
                        InvUtils.move().fromId(takeIndex).to(emptyResult.slot());

                    } else {
                        // TODO : 自动清理垃圾，或者交换身上的垃圾
                        error("没有空位了");
                        HeInvUtils.closeCurScreen();
                        complete();
                    }
                }

            }
            takeIndex++;
            return;
        }

        HeInvUtils.closeCurScreen();
        next(Stage.MineKit);
    }

    private void mineKit() {

        if (!mineKitIng) {
            mineKitIng = true;

            progress = 0.0f;
            pickResult = InvUtils.find(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE);
            if (!pickResult.isHotbar()) {
                error("未找到稿子");
                complete();
                return;
            }
            mine(false);
            return;

        } else {
            if (progress < 1.0f) {
                pickResult = InvUtils.find(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE);
                if (!pickResult.isHotbar()) {
                    error("未找到稿子");
                    complete();
                    return;
                }
                progress += BlockUtils.getBreakDelta(pickResult.slot(), mc.world.getBlockState(kitPos));
                if (progress < 1.0f) {
                    return;
                }
            }

            mine(true);
        }

        this.stage = Stage.PickUpKit;
    }

    private void pickUpKit() {
        // TODO : 离掉落的盒子太远时，需要走过去

        // TODO : 拿到手后需要放回末影箱
        this.stage = Stage.Complete;
    }

    private void mine(boolean done) {

        log.info("mine, done : {}", done);

        InvUtils.swap(pickResult.slot(), true);
        HeRotationUtils.rotate(kitPos);

        Direction direction = BlockUtils.getDirection(kitPos);
        if (!done) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, kitPos, direction));
        }
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, kitPos, direction));

        // mc.player.swingHand(Hand.MAIN_HAND);
        // InvUtils.swapBack();
    }

    private boolean containsElytra(ItemStack itemStack, boolean isDamaged) {
        if (itemStack.isEmpty()) {
            return false;
        }

        /*NbtElement nbtElement = itemStack.toNbt(mc.player.getRegistryManager());
        if (nbtElement == null) {
            return false;
        }

        if (nbtElement instanceof NbtCompound nbtCompound) {

            NbtList nbtList = Optional.of(nbtCompound)
                    .map(v -> v.getCompound("components"))
                    .map(v -> v.getList("minecraft:container", NbtElement.COMPOUND_TYPE))
                    .orElse(null);

            if (nbtList == null) {
                return false;
            }

            for (int i = 0; i < nbtList.size(); i++) {

                NbtCompound compound = nbtList.getCompound(i);

                if (compound.contains("item")) {
                    NbtCompound itemCompound = compound.getCompound("item");

                    if ("minecraft:elytra".equals(itemCompound.getString("id"))) {

                        String damageKey = "minecraft:damage";
                        if (itemCompound.contains(damageKey)) {
                            int damage = itemCompound.getInt(damageKey);
                            int surplusDamage = Items.ELYTRA.getDefaultStack().getMaxDamage() - damage;

                            if (surplusDamage <= elytraDamaged.get() && isDamaged) {
                                return true;
                            }

                            if (surplusDamage > elytraDamaged.get() && !isDamaged) {
                                return true;
                            }

                        } else if (!isDamaged) {
                            return true;
                        }
                    }
                }
            }
        }*/
        return false;
    }

    private FindItemResult findElytra(boolean isDamaged) {
        return InvUtils.find(itemStack -> {
            if (itemStack.getItem() == Items.ELYTRA) {
                int surplusDamage = itemStack.getMaxDamage() - itemStack.getDamage();
                return isDamaged == (surplusDamage <= elytraDamaged.get());
            }
            return false;
        });
    }

    private void updateNeedElytraQty() {
        FindItemResult notDamagedResult = findElytra(false);
        this.needElytraQty = notDamagedResult.found() ? elytraQty.get() - notDamagedResult.count() : elytraQty.get();

        log.info("updateNeedElytraQty : {}", needElytraQty);
    }

    private void complete() {
        this.enderChestPos = null;
        this.kitPos = null;
        this.mineKitIng = false;
        info("补给结束");
        toggle();
    }

    // @EventHandler
    private void onRender(Render3DEvent event) {

        if (enderChestPos != null) {
            Box box = enderChestShape.getBoundingBoxes().getFirst();
            event.renderer.box(enderChestPos.getX() + box.minX, enderChestPos.getY() + box.minY, enderChestPos.getZ() + box.minZ, enderChestPos.getX() + box.maxX, enderChestPos.getY() + box.maxY, enderChestPos.getZ() + box.maxZ, Color.BLUE, Color.BLUE, ShapeMode.Lines, 0);
        }
        if (kitPos != null) {
            Box box = kitShape.getBoundingBoxes().getFirst();
            event.renderer.box(kitPos.getX() + box.minX, kitPos.getY() + box.minY, kitPos.getZ() + box.minZ, kitPos.getX() + box.maxX, kitPos.getY() + box.maxY, kitPos.getZ() + box.maxZ, Color.GREEN, Color.GREEN, ShapeMode.Lines, 0);
        }
    }

    private void next(Stage next) {
        next(next, true);
    }

    private void next(Stage next, boolean needDelay) {
        if (needDelay) {
            this.waitTicks = delay.getDefaultValue();
        }
        this.stage = next;
    }

    private enum Stage {
        FindEnderChest,
        PlaceEnderChest,
        OpenEnderChest,
        TakeKit,
        FindKit,
        SwapKit,
        PlaceKit,
        OpenKit,
        TakeItems,
        MineKit,
        PickUpKit,
        Complete
    }
}
