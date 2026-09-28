package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.event.HeOpenScreenEvent;
import com.xiaohe66.mc.meteor.lotus.util.Const;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixin.AbstractSignEditScreenAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author He
 * @since 2025.05.04 23:43
 */
public class AutoSignPlus extends Module {

    private static final Logger log = LoggerFactory.getLogger(AutoSignPlus.class);

    private static final String PLAYER_NAME_KEY = "${name}";
    private static final String TIME_KEY_START = "$time{";
    private static final String TIME_KEY_END = "}";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> line1 = sgGeneral.add(new StringSetting.Builder()
            .name("第1行")
            .defaultValue(PLAYER_NAME_KEY)
            .build());

    private final Setting<String> line2 = sgGeneral.add(new StringSetting.Builder()
            .name("第2行")
            .defaultValue("was here,到此一游")
            .build());

    private final Setting<String> line3 = sgGeneral.add(new StringSetting.Builder()
            .name("第3行")
            .defaultValue("lotus by xiaohe66")
            .build());

    private final Setting<String> line4 = sgGeneral.add(new StringSetting.Builder()
            .name("第4行")
            .defaultValue("$time{yyyy.MM.dd HH:mm}")
            .build());

    private final Setting<Boolean> onlyEmpty = sgGeneral.add(new BoolSetting.Builder()
            .name("只填写空白牌子")
            .defaultValue(true)
            .build());

    public AutoSignPlus() {
        super(Const.CATEGORY, "自动签名", "自动写牌子。${name}:玩家ID; $time{yyyy.MM.dd HH:mm}:时间可自定义表达式");
    }


    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof UpdateSignC2SPacket packet) {

            log.info("UpdateSignC2SPacket : {}", packet);

            if (packet.isFront()) {

                String[] lineArr = packet.getText();
                updateLine(line1, lineArr, 0);
                updateLine(line2, lineArr, 1);
                updateLine(line3, lineArr, 2);
                updateLine(line4, lineArr, 3);
            }
        }
    }

    private void updateLine(Setting<String> lineSetting, String[] lineArr, int index) {
        String lineConfigText = lineSetting.get();
        String lineNewText = lineArr[index];

        if (!lineConfigText.contains(PLAYER_NAME_KEY) &&
                (!lineConfigText.startsWith(TIME_KEY_START) || !lineConfigText.endsWith(TIME_KEY_END))) {

            lineSetting.set(lineNewText);
        }
    }

    @EventHandler
    private void onHeOpenScreenEvent(HeOpenScreenEvent event) {

        log.info("onHeOpenScreenEvent : {}", event.screen);

        if (event.screen instanceof AbstractSignEditScreen screen) {

            // event.cancel();
            handleOpenScreen(event, screen);
        }
    }

    private void handleOpenScreen(HeOpenScreenEvent event, AbstractSignEditScreen screen) {

        SignBlockEntity sign = ((AbstractSignEditScreenAccessor) screen).meteor$getSign();

        boolean isAllBlank = true;
        SignText frontText = sign.getFrontText();
        Text[] originMessageArr = frontText.getMessages(false);
        for (Text text : originMessageArr) {
            String lineText = text.getString();
            if (StringUtils.isNotBlank(lineText)) {
                isAllBlank = false;
            }
        }

        // 忽略存在的
        if (onlyEmpty.get() && !isAllBlank) {
            return;
        }

        // 创建四行文本内容
        Text[] lines = new Text[]{formatText(line1.get()), formatText(line2.get()), formatText(line3.get()), formatText(line4.get())};

        // 创建 SignText 对象
        SignText signText = new SignText(lines, lines, DyeColor.BLACK, false);

        event.setSignText(signText);
    }


    private MutableText formatText(String text) {
        return Text.literal(format(text));
    }

    private String format(String text) {

        if (text.startsWith(TIME_KEY_START) && text.endsWith(TIME_KEY_END)) {

            String formatText = text.substring(PLAYER_NAME_KEY.length(), text.length() - 1);

            try {
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(formatText);
                return dateTimeFormatter.format(LocalDateTime.now());

            } catch (Exception e) {
                warning("时间表达式错误");
                return DATE_TIME_FORMATTER.format(LocalDateTime.now());
            }

        } else {

            return text.replace(PLAYER_NAME_KEY, mc.player.getName().getString());
        }
    }
}
