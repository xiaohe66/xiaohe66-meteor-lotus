package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.placemap.PlaceMapPos;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * @author He
 * @since 2025.12.01 19:42
 */
public class AutoPlaceMap extends StepModule {

    private boolean wasRightClicking = false;
    private BlockPos blockPos1;
    private BlockPos blockPos2;
    private Direction playerDirection;
    private final List<PlaceMapPos> placePosList = new ArrayList<>();
    private int placeIndex;

    public AutoPlaceMap() {
        super("自动贴画", "开启功能后, 给2个角放置展示框后自动贴画。由<hn2>友情赞助开发");

        addStep(Steps.PLACE, this::place);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (mc.player == null || !isActive() || step == Steps.PLACE) {
                return;
            }

            boolean isRightClicking = mc.options.useKey.isPressed();

            // 检测右键点击开始
            if (isRightClicking && !wasRightClicking) {
                checkItemFramePlacement();
            }

            wasRightClicking = isRightClicking;
        });
    }

    @Override
    public void onActivate() {
        init();
    }

    private void place() {
        int startSlot = placeIndex;
        do {

            PlaceMapPos mapPos = placePosList.get(placeIndex);
            if (mapPos.isDone()) {
                nextIndex();
            } else {
                // info("do sth");
                // do sth
                doPlace(mapPos);
                return;
            }
        } while (placeIndex != startSlot);

        toggle();
    }

    private void nextIndex() {
        if (placeIndex >= placePosList.size() - 1) {
            placeIndex = 0;
        } else {
            placeIndex++;
        }
    }

    private void doPlace(PlaceMapPos placeMapPos) {
        BlockPos placePos = placeMapPos.getBlockPos();

        ItemFrameEntity itemFrame = getItemFrameAtPosition(placePos);
        if (itemFrame == null) {

            FindItemResult findFrameResult = findFrame();
            if (findFrameResult.found()) {
                if (findFrameResult.isHotbar()) {
                    if (findFrameResult.getHand() == null) {
                        // 展示框切到主手
                        // info("切展示框到主手");
                        InvUtils.swap(findFrameResult.slot(), false);
                    } else {
                        // 放置
                        info("放置:" + placeIndex);
                        BlockUtils.place(placePos, findFrameResult, 0);
                        // HeBlockUtils.place(placePos, 0, false, direction, direction.getDoubleVector());
                        nextIndex();
                    }
                } else {
                    // info("切展示框到热栏");
                    HeInvUtils.swap(findFrameResult.slot(), 6);
                }
                setDelay();
            } else {
                info("缺少<展示框>");
                toggle();
            }
            return;
        }

        ItemStack heldItemStack = itemFrame.getHeldItemStack();
        if (heldItemStack.isEmpty()) {

            FindItemResult findMapResult = findMap(placeMapPos.getName());
            if (findMapResult.found()) {
                if (findMapResult.isHotbar()) {
                    if (findMapResult.getHand() == null) {
                        // 画切到主手
                        // info("切画到主手");
                        InvUtils.swap(findMapResult.slot(), false);
                    } else {
                        // 放置
                        info("放置:" + placeMapPos.getName());
                        interactEntity(itemFrame);
                        nextIndex();
                    }
                } else {
                    // info("切画到热栏");
                    HeInvUtils.swap(findMapResult.slot(), 7);
                }
                setDelay();
            } else {
                info("缺少<地图画>:" + placeMapPos.getName());
                toggle();
            }

        } else {
            // info("done : " + placeMapPos.getName());
            placeMapPos.setDone(true);
            nextIndex();
        }
    }

    private FindItemResult findMap(String name) {
        return InvUtils.find(itemStack -> itemStack.getItem() == Items.FILLED_MAP &&
                itemStack.getCustomName() != null &&
                name.equals(itemStack.getCustomName().getString()));
    }

    private FindItemResult findFrame() {
        return InvUtils.find(itemStack -> (itemStack.getItem() == Items.ITEM_FRAME || itemStack.getItem() == Items.GLOW_ITEM_FRAME));
    }

    private void checkItemFramePlacement() {

        ClientPlayerEntity player = mc.player;

        // 检查玩家手中的物品是否是展示框
        ItemStack mainHandStack = player.getStackInHand(Hand.MAIN_HAND);
        ItemStack offHandStack = player.getStackInHand(Hand.OFF_HAND);

        boolean holdingItemFrame = mainHandStack.isOf(Items.ITEM_FRAME) ||
                offHandStack.isOf(Items.ITEM_FRAME) ||
                mainHandStack.isOf(Items.GLOW_ITEM_FRAME) ||
                offHandStack.isOf(Items.GLOW_ITEM_FRAME);

        if (!holdingItemFrame) {
            return;
        }

        // 获取玩家指向的方块
        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) mc.crosshairTarget;
        BlockPos blockPos = blockHit.getBlockPos();
        Direction side = blockHit.getSide();

        // 计算展示框将要放置的位置
        BlockPos framePos = blockPos.offset(side);

        // info("尝试放置展示框在位置: " + framePos);
        // info("附着在方块: " + blockPos + " 的面: " + side);

        if (blockPos1 == null) {
            blockPos1 = new BlockPos(framePos);
            playerDirection = mc.player.getFacing();

        } else {
            blockPos2 = new BlockPos(framePos);
            readyPlacePos();
            readyMap();
            setDelay();
            step = Steps.PLACE;
        }
    }

    private void readyMap() {
        List<String> nameList = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = getItemStack(i);
            if (itemStack.getItem() != Items.FILLED_MAP) {
                continue;
            }
            Text customName = itemStack.getCustomName();
            if (customName != null) {
                nameList.add(customName.getString());
            }
        }
        if (nameList.size() < placePosList.size()) {
            info("地图画数量不对");
            toggle();

        } else {
            nameList.sort(null);

            for (int i = 0; i < placePosList.size(); i++) {
                PlaceMapPos pos = placePosList.get(i);
                String name = nameList.get(i);
                pos.setName(name);
            }
        }
    }

    private void readyPlacePos() {
        if (playerDirection == Direction.NORTH) {
            // 北
            if (blockPos1.getZ() != blockPos2.getZ()) {
                info("不在平面上");
                toggle();
                return;
            }
            for (int y = blockPos1.getY(), y1 = blockPos2.getY(); y >= y1; y--) {
                for (int x = blockPos1.getX(), x1 = blockPos2.getX(); x <= x1; x++) {
                    placePosList.add(new PlaceMapPos(x, y, blockPos1.getZ()));
                }
            }

        } else if (playerDirection == Direction.SOUTH) {
            // 南
            if (blockPos1.getZ() != blockPos2.getZ()) {
                info("不在平面上");
                toggle();
                return;
            }
            for (int y = blockPos1.getY(), y1 = blockPos2.getY(); y >= y1; y--) {
                for (int x = blockPos1.getX(), x1 = blockPos2.getX(); x >= x1; x--) {
                    placePosList.add(new PlaceMapPos(x, y, blockPos1.getZ()));
                }
            }

        } else if (playerDirection == Direction.WEST) {
            // 西
            if (blockPos1.getX() != blockPos2.getX()) {
                info("不在平面上");
                toggle();
                return;
            }
            for (int y = blockPos1.getY(), y1 = blockPos2.getY(); y >= y1; y--) {
                for (int z = blockPos1.getZ(), z1 = blockPos2.getZ(); z >= z1; z--) {
                    placePosList.add(new PlaceMapPos(blockPos1.getX(), y, z));
                }
            }

        } else if (playerDirection == Direction.EAST) {
            // 东
            if (blockPos1.getX() != blockPos2.getX()) {
                info("不在平面上");
                toggle();
                return;
            }
            for (int y = blockPos1.getY(), y1 = blockPos2.getY(); y >= y1; y--) {
                for (int z = blockPos1.getZ(), z1 = blockPos2.getZ(); z <= z1; z++) {
                    placePosList.add(new PlaceMapPos(blockPos1.getX(), y, z));
                }
            }
        }
    }

    public ItemFrameEntity getItemFrameAtPosition(BlockPos framePos) {
        if (mc.world == null) {
            return null;
        }

        // 创建一个小的搜索区域（正好是展示框的位置）
        Box searchBox = new Box(
                framePos.getX(), framePos.getY(), framePos.getZ(),
                framePos.getX() + 1, framePos.getY() + 1, framePos.getZ() + 1
        );

        var frames = mc.world.getEntitiesByClass(
                ItemFrameEntity.class,
                searchBox,
                itemFrame -> itemFrame.getBlockPos().equals(framePos)
        );

        return frames.isEmpty() ? null : frames.get(0);
    }

    @EventHandler
    public void onRenderWorld(final Render3DEvent event) {
        if (placePosList.isEmpty()) {
            if (blockPos1 != null) {
                event.renderer.box(blockPos1, Color.ORANGE, Color.BLUE, ShapeMode.Lines, 0);
            }
        } else {
            for (PlaceMapPos pos : placePosList) {
                event.renderer.box(pos.getBlockPos(), Color.ORANGE, Color.BLUE, ShapeMode.Lines, 0);
            }
        }
    }

    private void init() {
        blockPos1 = null;
        blockPos2 = null;
        placePosList.clear();
        placeIndex = 1;
        step = Steps.NONE;
    }

    @Override
    public void onDeactivate() {
        init();
    }
}
