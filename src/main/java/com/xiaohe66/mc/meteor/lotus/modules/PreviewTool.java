package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.event.HandledScreenRenderEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseScrollEvent;
import com.xiaohe66.mc.meteor.lotus.event.ScreenRenderEvent;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import com.xiaohe66.mc.meteor.lotus.util.ShulkerBoxReader;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author He
 * @since 2025.07.22 23:07
 */
public class PreviewTool extends BaseModule {

    private static final int SLOT_SIZE = 18;
    private static final int MARGIN = 2;

    private final SettingGroup iconGroup = settings.createGroup("盒子图标");

    public final Setting<Boolean> kitIcon = iconGroup.add(new BoolSetting.Builder()
            .name("盒子图标")
            .description("显示盒子数量最多的物品(以组为单位)")
            .defaultValue(true)
            .build());

    private final Setting<Double> iconScale = iconGroup.add(new DoubleSetting.Builder()
            .name("图标比例")
            .description("盒子图标比例")
            .defaultValue(0.4)
            .sliderRange(0.1, 1)
            .build());

    private final Setting<Integer> iconOffsetX = iconGroup.add(new IntSetting.Builder()
            .name("图标偏移量X")
            .description("偏移量")
            .defaultValue(7)
            .sliderRange(-20, 20)
            .build());

    private final Setting<Integer> iconOffsetY = iconGroup.add(new IntSetting.Builder()
            .name("图标偏移量Y")
            .description("偏移量")
            .defaultValue(7)
            .sliderRange(-20, 20)
            .build());

    private final SettingGroup kitSpreadGroup = settings.createGroup("盒子平铺");

    public final Setting<Boolean> kitSpread = kitSpreadGroup.add(new BoolSetting.Builder()
            .name("盒子平铺")
            .description("将kit在界面中平铺显示")
            .defaultValue(true)
            .build());

    private final Setting<Integer> top = kitSpreadGroup.add(new IntSetting.Builder()
            .name("上边距")
            .description("上边距")
            .defaultValue(6)
            .min(0)
            .sliderMax(100)
            .build());

    private final Setting<Integer> left = kitSpreadGroup.add(new IntSetting.Builder()
            .name("左边距")
            .description("左边距")
            .defaultValue(6)
            .min(0)
            .sliderMax(500)
            .build());

    private final Setting<Integer> marge = kitSpreadGroup.add(new IntSetting.Builder()
            .name("间距")
            .description("间距")
            .defaultValue(4)
            .min(0)
            .sliderMax(10)
            .build());

    private final Setting<Double> scale = kitSpreadGroup.add(new DoubleSetting.Builder()
            .name("缩放")
            .description("平铺区域的缩放比例")
            .defaultValue(1.0)
            .sliderRange(0.5, 1.5)
            .build());

    private final Setting<Boolean> compact = kitSpreadGroup.add(new BoolSetting.Builder()
            .name("紧凑模式")
            .description("将相同物品合并显示，节省空间")
            .defaultValue(true)
            .build());

    private final Setting<Integer> backgroundAlpha = kitSpreadGroup.add(new IntSetting.Builder()
            .name("背景透明度")
            .description("盒子背景的不透明度")
            .defaultValue(30)
            .min(0)
            .sliderMax(255)
            .build());

    /*private final Setting<Boolean> bothSides = kitSpreadGroup.add(new BoolSetting.Builder()
            .name("自动换列")
            .description("当屏幕下方放不下时，在右侧继续排列")
            .defaultValue(true)
            .build());*/

    private int scrollOffset = 0;
    private int lastSyncId = 0;

    public PreviewTool() {
        super("预览器", "盒子小图标预览、盒子内容平铺");
    }

    @EventHandler
    private void onOpenScreen(ScreenRenderEvent event) {
        if (!kitSpread.get() || !(mc.currentScreen instanceof HandledScreen)) {
            return;
        }

        List<ShulkerBoxReader> readerList = collectReaders();
        if (readerList.isEmpty()) {
            return;
        }

        if (lastSyncId != mc.player.currentScreenHandler.syncId) {
            scrollOffset = 0;
            lastSyncId = mc.player.currentScreenHandler.syncId;
        }

        DrawContext drawContext = event.getDrawContext();
        drawContext.createNewRootLayer();
        Matrix3x2fStack matrices = drawContext.getMatrices();
        matrices.pushMatrix();

        float scaleValue = scale.get().floatValue();
        matrices.scale(scaleValue, scaleValue);

        float scaledOffset = scrollOffset / scaleValue;

        int startX = left.get();
        int currentY = top.get() + (int) scaledOffset;
        // boolean rightSide = false;

        // int maxHeight = (int) (drawContext.getScaledWindowHeight() / scaleValue);
        // int maxWidth = (int) (drawContext.getScaledWindowWidth() / scaleValue);

        ItemStack tooltipStack = ItemStack.EMPTY;

        for (ShulkerBoxReader reader : readerList) {
            List<ItemStack> stacks = compact.get() ? reader.getCondensed() : new ArrayList<>(reader.getStacks());

            // 非紧凑模式下补齐27格
            if (!compact.get()) {
                while (stacks.size() < 27) {
                    stacks.add(ItemStack.EMPTY);
                }
            }

            int itemCount = compact.get()
                    ? (int) stacks.stream().filter(s -> !s.isEmpty()).count()
                    : stacks.size();

            int rows = Math.max(1, (itemCount - 1) / 9 + 1);
            int cols = MathHelper.clamp(itemCount, 1, 9);
            int boxWidth = cols * SLOT_SIZE + MARGIN * 2;
            int boxHeight = rows * SLOT_SIZE + MARGIN * 2;

            // 自动换列
            /*if (bothSides.get() && currentY + boxHeight > maxHeight && !rightSide) {
                rightSide = true;
                currentY = top.get() + (int) scaledOffset;
            }

            if (rightSide) {
                startX = maxWidth - boxWidth - left.get();
            }*/

            // 绘制背景
            int color = reader.getColor();
            int bgAlpha = backgroundAlpha.get();
            int bgColor = (bgAlpha << 24);
            drawContext.fill(startX, currentY, startX + boxWidth, currentY + boxHeight, bgColor);
            drawContext.fill(startX, currentY - 1, startX + boxWidth, currentY, color);

            // 绘制物品
            int count = 0;
            for (ItemStack stack : stacks) {
                if (compact.get() && stack.isEmpty()) {
                    continue;
                }

                int col = count % 9;
                int row = count / 9;
                int x = startX + MARGIN + col * SLOT_SIZE;
                int y = currentY + MARGIN + row * SLOT_SIZE;

                drawStack(drawContext, event.getTextRenderer(), stack, x, y);

                if (isMouseOverSlot(event.getMouseX(), event.getMouseY(), x, y, scaleValue)) {
                    tooltipStack = stack;
                }
                count++;
            }

            // 绘制盒子堆叠数量
            if (reader.getOriginItemStackCount() > 1) {
                String text = "x" + reader.getOriginItemStackCount();
                int textX = startX + boxWidth + MARGIN;
                int textY = currentY + boxHeight - mc.textRenderer.fontHeight - MARGIN;
                drawContext.drawText(event.getTextRenderer(), text, textX, textY, Color.GREEN.getRGB(), true);
            }

            currentY += boxHeight + marge.get();
        }

        // 绘制 Tooltip
        if (!tooltipStack.isEmpty()) {
            float invScale = 1f / scaleValue;
            matrices.pushMatrix();
            matrices.scale(invScale, invScale);
            drawContext.drawItemTooltip(event.getTextRenderer(), tooltipStack, event.getMouseX(), event.getMouseY());
            matrices.popMatrix();
        }

        matrices.popMatrix();
    }

    @EventHandler
    private void onMouseScroll(MouseScrollEvent event) {
        if (!kitSpread.get() || !(mc.currentScreen instanceof HandledScreen)) {
            return;
        }

        List<ShulkerBoxReader> readerList = collectReaders();
        if (readerList.isEmpty()) return;

        float totalHeight = 0;
        for (ShulkerBoxReader reader : readerList) {
            List<ItemStack> stacks = compact.get() ? reader.getCondensed() : reader.getStacks();
            int itemCount = compact.get()
                    ? (int) stacks.stream().filter(s -> !s.isEmpty()).count()
                    : 27;
            int rows = Math.max(1, (itemCount - 1) / 9 + 1);
            totalHeight += rows * SLOT_SIZE + MARGIN * 2 + marge.get();
        }
        totalHeight += marge.get();

        float scaleValue = scale.get().floatValue();
        float maxScroll = Math.min(-totalHeight + mc.getWindow().getScaledHeight() / scaleValue, 0);
        this.scrollOffset = (int) MathHelper.clamp(scrollOffset + Math.ceil(event.getVerticalAmount()) * 15, maxScroll, 0);
    }

    private List<ShulkerBoxReader> collectReaders() {
        List<ShulkerBoxReader> list = new ArrayList<>();
        for (ItemStack boxItemStack : HeInvUtils.findAndMargeShulkerBox()) {
            ShulkerBoxReader reader = new ShulkerBoxReader(boxItemStack);
            if (!reader.isEmpty()) {
                list.add(reader);
            }
        }
        return list;
    }

    private void drawStack(DrawContext ctx, TextRenderer textRenderer, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) {
            return;
        }
        ctx.drawItem(stack, x, y);
        if (stack.getCount() > 999) {
            String text = "%.1fk".formatted(stack.getCount() / 1000f);
            ctx.drawStackOverlay(textRenderer, stack, x, y, text);
        } else {
            ctx.drawStackOverlay(textRenderer, stack, x, y);
        }
    }

    private boolean isMouseOverSlot(int mouseX, int mouseY, int slotX, int slotY, float scale) {
        int scaledX = (int) (mouseX / scale);
        int scaledY = (int) (mouseY / scale);
        return scaledX >= slotX && scaledX < slotX + SLOT_SIZE && scaledY >= slotY && scaledY < slotY + SLOT_SIZE;
    }

    @EventHandler
    private void onHandledScreenRenderEvent(HandledScreenRenderEvent event) {
        if (!kitIcon.get() || !(mc.currentScreen instanceof HandledScreen screen)) {
            return;
        }

        for (Slot slot : screen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (stack == null || !HeItemUtils.isShulkerBox(stack.getItem())) {
                continue;
            }
            drawKitIcon(event.getDrawContext(), stack, slot.x, slot.y);
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!kitIcon.get() || mc.currentScreen instanceof HandledScreen) {
            return;
        }

        // 在关闭背包后，绘制快捷栏上的盒子图标
        int scaledWidth = mc.getWindow().getScaledWidth();
        int scaledHeight = mc.getWindow().getScaledHeight();

        // 快捷栏在屏幕底部居中，计算起始位置
        int hotbarWidth = 182; // 9个槽位 * 20 + 边框
        int hotbarHeight = 22;
        int startX = (scaledWidth - hotbarWidth) / 2 + 3; // +3 是偏移量，对准槽位中心
        int startY = scaledHeight - hotbarHeight + 3; // +3 是偏移量，对准槽位中心

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack == null || !HeItemUtils.isShulkerBox(stack.getItem())) {
                continue;
            }

            int slotX = startX + i * 20;
            int slotY = startY;
            drawKitIcon(event.drawContext, stack, slotX, slotY);
        }
    }

    private void drawKitIcon(DrawContext drawContext, ItemStack kitItemStack, int originX, int originY) {
        ShulkerBoxReader reader = new ShulkerBoxReader(kitItemStack);
        ItemStack maximumItemStack = reader.getMaximumItem();
        if (maximumItemStack.isEmpty()) {
            return;
        }

        float scaleValue = iconScale.get().floatValue();
        Matrix3x2fStack matrices = drawContext.getMatrices();

        matrices.pushMatrix();
        matrices.translate(originX + iconOffsetX.get(), originY + iconOffsetY.get());
        matrices.scale(scaleValue, scaleValue);
        drawContext.drawItem(maximumItemStack, 0, 0);
        matrices.popMatrix();

        matrices.pushMatrix();
        matrices.translate(-2, 0);
        int beginY = 2;
        int maxHeight = 12;
        int height = (int) ((maximumItemStack.getCount() * 1.0 / maximumItemStack.getMaxCount() / 27) * maxHeight);
        drawContext.fill(originX + 16, originY + beginY, originX + 17, originY + beginY + maxHeight, 0xFFFFFFFF);
        drawContext.fill(originX + 16, originY + (16 - beginY - height), originX + 17, originY + (16 - beginY), 0xFF00FF00);
        matrices.popMatrix();
    }

    @Override
    public void onDeactivate() {
    }
}
