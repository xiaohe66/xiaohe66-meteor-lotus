package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.bo.ItemWarp;
import com.xiaohe66.mc.meteor.lotus.event.LogEvent;
import com.xiaohe66.mc.meteor.lotus.modules.entitylist.DisplaySide;
import com.xiaohe66.mc.meteor.lotus.util.Const;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.render.MeteorToast;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author He
 * @since 2025.05.03 11:36
 */
public class EntityList extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Set<EntityType<?>>> entitys = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("主世界实体")
            .description("仅在主世界显示的实体")
            .defaultValue(
                    EntityType.EXPERIENCE_ORB, //经验
                    EntityType.ZOMBIFIED_PIGLIN //僵尸猪灵
            )
            .build());

    public final Setting<Set<EntityType<?>>> netherEntitys = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("下界实体")
            .description("仅在下界显示的实体")
            .defaultValue(
                    EntityType.EXPERIENCE_ORB,//经验
                    EntityType.COW,//牛
                    EntityType.SHEEP,//羊
                    EntityType.PIG,//猪
                    EntityType.HORSE,//马
                    EntityType.ZOMBIE,//僵尸
                    EntityType.CREEPER,//苦力怕
                    EntityType.BOGGED,//沼骸
                    EntityType.HUSK,//尸壳
                    EntityType.SLIME,//尸壳
                    EntityType.VILLAGER,// 村民
                    EntityType.SPIDER,// 蜘蛛
                    EntityType.CAVE_SPIDER,// 洞穴蜘蛛
                    EntityType.DROWNED,// 溺尸
                    EntityType.ZOMBIE_VILLAGER// 村民
            )
            .build());

    public final Setting<SettingColor> entitysColor = sgGeneral.add(new ColorSetting.Builder()
            .name("实体颜色")
            .defaultValue(Color.MAGENTA)
            .build());

    public final Setting<Boolean> entityLog = sgGeneral.add(new BoolSetting.Builder()
            .name("实体日志")
            .defaultValue(false)
            .build());

    private final SettingGroup itemGroup = settings.createGroup("物品");

    public final Setting<List<Item>> items1 = itemGroup.add(new ItemListSetting.Builder()
            .name("物品1")
            .defaultValue(
                    Items.ELYTRA,

                    Items.SHULKER_BOX,
                    Items.WHITE_SHULKER_BOX,
                    Items.ORANGE_SHULKER_BOX,
                    Items.MAGENTA_SHULKER_BOX,
                    Items.LIGHT_BLUE_SHULKER_BOX,
                    Items.YELLOW_SHULKER_BOX,
                    Items.LIME_SHULKER_BOX,
                    Items.PINK_SHULKER_BOX,
                    Items.GRAY_SHULKER_BOX,
                    Items.LIGHT_GRAY_SHULKER_BOX,
                    Items.CYAN_SHULKER_BOX,
                    Items.PURPLE_SHULKER_BOX,
                    Items.BLUE_SHULKER_BOX,
                    Items.BROWN_SHULKER_BOX,
                    Items.GREEN_SHULKER_BOX,
                    Items.RED_SHULKER_BOX,
                    Items.BLACK_SHULKER_BOX,

                    Items.BUNDLE,
                    Items.WHITE_BUNDLE,
                    Items.ORANGE_BUNDLE,
                    Items.MAGENTA_BUNDLE,
                    Items.LIGHT_BLUE_BUNDLE,
                    Items.YELLOW_BUNDLE,
                    Items.LIME_BUNDLE,
                    Items.PINK_BUNDLE,
                    Items.GRAY_BUNDLE,
                    Items.LIGHT_GRAY_BUNDLE,
                    Items.CYAN_BUNDLE,
                    Items.PURPLE_BUNDLE,
                    Items.BLUE_BUNDLE,
                    Items.BROWN_BUNDLE,
                    Items.GREEN_BUNDLE,
                    Items.RED_BUNDLE,
                    Items.BLACK_BUNDLE,

                    Items.ANCIENT_DEBRIS,
                    Items.NETHERITE_SCRAP,
                    Items.NETHERITE_INGOT,
                    Items.NETHERITE_BLOCK,
                    Items.NETHERITE_SWORD,
                    Items.NETHERITE_AXE,
                    Items.NETHERITE_HOE,
                    Items.NETHERITE_PICKAXE,
                    Items.NETHERITE_SHOVEL,
                    Items.NETHERITE_HELMET,
                    Items.NETHERITE_CHESTPLATE,
                    Items.NETHERITE_LEGGINGS,
                    Items.NETHERITE_BOOTS
            )
            .build());

    public final Setting<SettingColor> items1Color = itemGroup.add(new ColorSetting.Builder()
            .name("物品1颜色")
            .defaultValue(Color.RED)
            .build());

    public final Setting<Boolean> item1Log = itemGroup.add(new BoolSetting.Builder()
            .name("物品1日志")
            .defaultValue(false)
            .build());

    public final Setting<Boolean> sendNotifications = itemGroup.add(new BoolSetting.Builder()
            .name("物品1通知")
            .defaultValue(true)
            .build());

    public final Setting<Integer> sendNotificationsCheckSeconds = itemGroup.add(new IntSetting.Builder()
            .name("通知检测时间")
            .min(1)
            .sliderMax(5)
            .defaultValue(2)
            .build());

    public final Setting<Integer> sendNotificationsIntervalSeconds = itemGroup.add(new IntSetting.Builder()
            .name("通知间隔时间")
            .min(1)
            .sliderMax(10)
            .defaultValue(5)
            .build());

    public final Setting<List<Item>> items2 = itemGroup.add(new ItemListSetting.Builder()
            .name("物品2")
            .build());

    public final Setting<SettingColor> items2Color = itemGroup.add(new ColorSetting.Builder()
            .name("物品2颜色")
            .defaultValue(Color.CYAN)
            .build());

    public final Setting<SettingColor> itemsColor = itemGroup.add(new ColorSetting.Builder()
            .name("物品默认颜色")
            .defaultValue(Color.YELLOW)
            .build());

    public final Setting<List<Item>> blackList = itemGroup.add(new ItemListSetting.Builder()
            .name("黑名单")
            .build());

    private final SettingGroup ui = settings.createGroup("界面");

    public final Setting<Integer> xOffset = ui.add(new IntSetting.Builder()
            .name("X偏移")
            .min(0)
            .sliderMax(2048)
            .defaultValue(20)
            .build());

    public final Setting<Integer> yOffset = ui.add(new IntSetting.Builder()
            .name("Y偏移")
            .min(0)
            .sliderMax(2048)
            .defaultValue(500)
            .build());

    public final Setting<DisplaySide> displaySide = ui.add(new EnumSetting.Builder<DisplaySide>()
            .name("位置")
            .description("选择实体列表显示的位置")
            .defaultValue(DisplaySide.Left)
            .build());

    public final Setting<Integer> lineHeight = ui.add(new IntSetting.Builder()
            .name("行高")
            .min(0)
            .sliderMax(100)
            .defaultValue(20)
            .build());

    public final Setting<Double> scale = ui.add(new DoubleSetting.Builder()
            .name("字体大小")
            .min(0)
            .sliderMax(6)
            .defaultValue(1)
            .build());

    private long startTime;
    private long prevTime;

    public EntityList() {
        super(Const.CATEGORY, "实体列表", "显示实体列表, 可以按重要程度分组, 如红色为重点关注");
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (Utils.isLoading()) {
            return;
        }

        if (!isActive()) {
            return;
        }

        Set<Item> items1Set = new HashSet<>(items1.get());
        Set<Item> items2Set = new HashSet<>(items2.get());
        Set<Item> blackListSet = new HashSet<>(blackList.get());

        Map<Item, ItemWarp> items1Map = new HashMap<>();
        Map<Item, ItemWarp> items2Map = new HashMap<>();
        Map<Item, ItemWarp> itemsMap = new HashMap<>();
        Map<EntityType<?>, Integer> entitysMap = new HashMap<>();

        RegistryKey<World> registryKey = mc.player.getEntityWorld().getRegistryKey();

        for (Entity entity : mc.world.getEntities()) {

            if (entity instanceof ItemEntity itemEntity) {

                ItemStack stack = itemEntity.getStack();
                Item item = stack.getItem();

                if (blackListSet.contains(item)) {
                    continue;
                }

                Map<Item, ItemWarp> map;
                if (items1Set.contains(item)) {
                    map = items1Map;
                    if (item1Log.get()) {
                        String msg = "检测到物品 : " + Names.get(item);
                        postLogEvent(entity, registryKey, entity.getEntityPos(), msg);
                    }

                } else if (items2Set.contains(item)) {
                    map = items2Map;

                } else {
                    map = itemsMap;
                }

                ItemWarp itemWarp = map.computeIfAbsent(item, k -> new ItemWarp());
                itemWarp.setItem(item);
                itemWarp.setCount(itemWarp.getCount() + stack.getCount());

                float distanceTo = mc.player.distanceTo(entity);
                if (distanceTo < itemWarp.getMinDistance()) {
                    itemWarp.setMinDistance(distanceTo);
                }

            } else {
                EntityType<?> entityType = entity.getType();

                Set<EntityType<?>> entityTypes;
                if (registryKey == ServerWorld.NETHER) {
                    entityTypes = netherEntitys.get();

                } else if (registryKey == ServerWorld.OVERWORLD) {
                    entityTypes = entitys.get();

                } else {
                    entityTypes = Collections.emptySet();
                }

                if (entityTypes.contains(entityType)) {

                    Integer qty = entitysMap.getOrDefault(entityType, 0);
                    entitysMap.put(entityType, qty + 1);

                    // 记录日志
                    if (entityLog.get()) {
                        String msg = "检测到实体 : " + Names.get(entityType);
                        postLogEvent(entity, registryKey, entity.getEntityPos(), msg);
                    }
                }
            }
        }

        int y = yOffset.get();
        int screenWidth = mc.getWindow().getWidth();

        y = draw(items1Map, y, items1Color.get(), screenWidth);
        if (sendNotifications.get()) {
            long currentTimeMillis = System.currentTimeMillis();
            if (items1Map.isEmpty()) {
                // 没有物品了, 重置
                startTime = 0;

            } else if (startTime == 0) {
                // 有物品, 记录开始时间
                startTime = currentTimeMillis;

            } else if (currentTimeMillis - startTime > sendNotificationsCheckSeconds.get() * 1000) {
                // 记录的时间够长了, 所以要通知. 间隔5秒发一次通知
                if (currentTimeMillis - prevTime > sendNotificationsIntervalSeconds.get() * 1000) {
                    info("捡东西啦");
                    // NOTE : MeteorToast 在当前版本不会显示材质, 但是在未来的版本被修复了
                    MeteorToast toast = new MeteorToast.Builder(this.title)
                            .icon(Items.CHEST)
                            .text("捡东西啦~")
                            .build();
                    mc.getToastManager().add(toast);
                    prevTime = currentTimeMillis;
                }
            }
        }
        y = draw(items2Map, y, items2Color.get(), screenWidth);
        y = draw(itemsMap, y, itemsColor.get(), screenWidth);
        draw2(entitysMap, y, entitysColor.get(), screenWidth);
    }

    private void postLogEvent(Entity entity, RegistryKey<World> worldType, Vec3d pos, String msg) {

        LogEvent logEvent = new LogEvent();
        logEvent.setModuleName(super.name);
        logEvent.setWorldType(worldType);
        logEvent.setPos(new Vec3i((int) pos.getX(), (int) pos.getY(), (int) pos.getZ()));
        logEvent.setKey(String.valueOf(entity.getId()));
        logEvent.setMsg(msg);
        // logEvent.setMsg("检测到实体 : " + entity.getDisplayName());

        MeteorClient.EVENT_BUS.post(logEvent);
    }

    private int draw(Map<Item, ItemWarp> grayMap, int y, Color color, int screenWidth) {

        if (grayMap.isEmpty()) {
            return y;
        }

        // List<ItemWarp> itemWarps = new ArrayList<>(grayMap.values());
        // itemWarps.sort((o1, o2) -> Float.compare(o1.getMinDistance(), o2.getMinDistance()));

        TextRenderer textRenderer = TextRenderer.get();

        for (ItemWarp itemWarp : grayMap.values()) {
            String name = Names.get(itemWarp.getItem());
            String text = String.format("%s x%s  (%.1f m)", name, itemWarp.getCount(), itemWarp.getMinDistance());

            textRenderer.begin(scale.get());
            // 根据配置决定X坐标
            int x;
            if (displaySide.get() == DisplaySide.Right) {
                // 计算文本宽度
                int textWidth = (int) (textRenderer.getWidth(text));
                x = screenWidth - textWidth - xOffset.get();
            } else {
                x = xOffset.get();
            }
            textRenderer.render(text, x, y, color, false);
            textRenderer.end();

            y += lineHeight.get() * scale.get();
        }

        return y;
    }

    private int draw2(Map<EntityType<?>, Integer> grayMap, int y, Color color, int screenWidth) {

        TextRenderer textRenderer = TextRenderer.get();

        for (Map.Entry<EntityType<?>, Integer> entry : grayMap.entrySet()) {

            EntityType<?> item = entry.getKey();
            Integer count = entry.getValue();

            String name = Names.get(item);
            String text = String.format("%s x%s", name, count);

            textRenderer.begin(scale.get());
            // 根据配置决定X坐标
            int x;
            if (displaySide.get() == DisplaySide.Right) {
                // 计算文本宽度
                int textWidth = (int) (textRenderer.getWidth(text));
                x = screenWidth - textWidth - xOffset.get();
            } else {
                x = xOffset.get();
            }
            textRenderer.render(text, x, y, color, true);
            textRenderer.end();

            y += lineHeight.get() * scale.get();
        }

        return y;
    }
}
