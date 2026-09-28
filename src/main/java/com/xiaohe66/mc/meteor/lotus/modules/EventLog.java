package com.xiaohe66.mc.meteor.lotus.modules;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xiaohe66.mc.meteor.lotus.event.LogEvent;
import com.xiaohe66.mc.meteor.lotus.util.Const;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author He
 * @since 2025.05.18 11:03
 */
public class EventLog extends Module {

    private static final Logger log = LoggerFactory.getLogger(EventLog.class);

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private Cache<String, LogEvent> logCache = CacheBuilder.newBuilder()
            .maximumSize(256)
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> logPath = sgGeneral.add(new StringSetting.Builder()
            .name("日志路径")
            .description("日志路径")
            .defaultValue("D:\\mc\\xiaohe66-meteor-lotus\\event_log.txt")
            .onChanged(this::checkPath)
            .build());

    public EventLog() {
        super(Const.CATEGORY, "事件日志", "记录各种功能的事件, 保存日志到电脑中, 可用于分析");
    }

    @Override
    public void onActivate() {
        checkPath(logPath.get());
    }

    @EventHandler
    private void onLogEvent(LogEvent logEvent) {
        if (!isActive()) {
            return;
        }

        String formatKey = logEvent.formatKey();

        LogEvent existLogEvent = logCache.getIfPresent(formatKey);
        if (existLogEvent == null) {
            String time = LocalDateTime.now().format(dateTimeFormatter);
            String pos = logEvent.getPos() != null ? logEvent.getPos().toShortString() : "";

            String msgContext = String.format("[%s] [%s] - %s", pos, logEvent.getModuleName(), logEvent.getMsg());
            ChatUtils.sendMsg(Formatting.GRAY, msgContext);

            String logContext = String.format("%s [%s] [%s] - %s\n", time, pos, logEvent.getModuleName(), logEvent.getMsg());
            mc.world.playSoundFromEntity(mc.player, mc.player, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.AMBIENT, 3.0F, 1.0F);

            executorService.execute(() -> saveLog(logContext));
        }

        logCache.put(formatKey, logEvent);
    }

    private void saveLog(String logContext) {

        File file = new File(logPath.get());

        if (!checkPath(file)) {
            return;
        }

        try {
            FileUtils.writeStringToFile(file, logContext, StandardCharsets.UTF_8, true);

        } catch (IOException e) {
            log.error("记录日志失败, path : {}, log : {}", logPath.get(), logContext, e);
            error("记录日志失败 : " + e.getMessage());
        }
    }

    private boolean checkPath(String path) {
        File file = new File(path);
        return checkPath(file);
    }

    private boolean checkPath(File file) {

        try {
            FileUtils.createParentDirectories(file);

            if (!file.exists()) {
                boolean ignore = file.createNewFile();
            }

            return true;

        } catch (IOException e) {
            log.error("文件路径无效 : {}", logPath, e);
            error("文件路径无效");
        }

        return false;
    }
}
