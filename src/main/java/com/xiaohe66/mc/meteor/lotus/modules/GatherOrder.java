package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.util.Const;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * @author He
 * @since 2025.04.17 21:46
 */
public class GatherOrder extends Module {

    private static final Logger log = LoggerFactory.getLogger(GatherOrder.class);

    private static final String VERSION_PLACEHOLDER = "{version}";
    private static final String RANDOM_PLACEHOLDER = "{random}";
    private static final String DEFAULT_MESSAGE = RANDOM_PLACEHOLDER + " -- Lotus-" + VERSION_PLACEHOLDER;
    private static final String VERSION;

    private static final String[] POEMS = {
            "粉身碎骨浑不怕，要留清白在人间。",
            "咬定青山不放松，立根原在破岩中。",
            "出淤泥而不染，濯清涟而不妖。",
            "不要人夸好颜色，只留清气满乾坤。",
            "清风两袖朝天去，不带江南一寸棉。",
            "宁可枝头抱香死，何曾吹落北风中。",
            "海内存知己，天涯若比邻。",
            "晚来天欲雪，能饮一杯无？",
            "莫愁前路无知己，天下谁人不识君。",
            "浮云游子意，落日故人情。",
            "投我以木瓜，报之以琼琚。",
            "投我以桃，报之以李。",
            "落地为兄弟，何必骨肉亲。",
            "一生大笑能几回，斗酒相逢须醉倒。",
            "相知无远近，万里尚为邻。",
            "人生自古谁无死，留取丹心照汗青。",
            "生当作人杰，死亦为鬼雄。",
            "自古驱民在信诚，一言为重百金轻。",
            "三杯吐然诺，五岳倒为轻。",
            "酒逢知己千杯少，话不投机半句多。",
            "长风破浪会有时，直挂云帆济沧海。",
            "同是天涯沦落人，相逢何必曾相识。",
            "少壮不努力，老大徒伤悲。",
            "莫等闲，白了少年头，空悲切！",
    };

    private final Random random = new Random();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> intervalMinutes = sgGeneral.add(new IntSetting.Builder()
            .name("发送间隔")
            .description("触发后发送消息的冷却时间（分钟）")
            .min(1)
            .sliderMax(60)
            .defaultValue(5)
            .build());

    private final Setting<String> keyword = sgGeneral.add(new StringSetting.Builder()
            .name("监听关键字")
            .description("公屏消息包含此关键字时自动发送消息")
            .defaultValue("挂电集结")
            .build());

    private final Setting<String> message = sgGeneral.add(new StringSetting.Builder()
            .name("发送消息")
            .description("触发后自动发送的内容，支持{random} 和 {version} 占位符")
            .defaultValue(DEFAULT_MESSAGE)
            .build());

    private final Setting<Boolean> permanentlyDisabled = sgGeneral.add(new BoolSetting.Builder()
            .name("自动启动")
            .description("勾选后，集结令功能将自动开启")
            .defaultValue(true)
            .build());

    private long lastSendTime = 0;

    static {
        VERSION = FabricLoader.getInstance()
                .getModContainer(Const.NAME)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("");
    }

    public GatherOrder() {
        super(Const.CATEGORY, "集结令", "监听公屏聊天，出现符合关键字时自动发送自定义消息");
    }

    @Override
    public void onActivate() {
        this.lastSendTime = 0;
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (!isActive()) {
            return;
        }

        String text = event.getMessage().getString();
        String keywordValue = keyword.get();

        if (keywordValue.isBlank()) {
            return;
        }

        if (text.contains(keywordValue)) {
            long intervalMillis = intervalMinutes.get() * 60L * 1000L;
            long now = System.currentTimeMillis();

            if (now - lastSendTime < intervalMillis) {
                return;
            }

            String msg = resolveMessage();
            if (msg.isBlank()) {
                return;
            }

            // log.info("集结令触发，发送消息: {}", msg);
            ChatUtils.sendPlayerMsg(msg);
            lastSendTime = now;
        }
    }

    private String resolveMessage() {
        String raw = message.get();
        if (raw.contains(RANDOM_PLACEHOLDER)) {
            int index = random.nextInt(POEMS.length);
            raw = raw.replace(RANDOM_PLACEHOLDER, POEMS[index]);
        }
        if (raw.contains(VERSION_PLACEHOLDER)) {
            raw = raw.replace(VERSION_PLACEHOLDER, VERSION);
        }
        return raw;
    }

    public boolean isPermanentlyDisabled() {
        return permanentlyDisabled.get();
    }
}
