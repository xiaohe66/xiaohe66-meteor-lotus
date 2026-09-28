package com.xiaohe66.mc.meteor.lotus.event;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.Objects;

/**
 * @author He
 * @since 2025.05.28 09:49
 */
public class LogEvent extends Event {

    private String moduleName;
    private RegistryKey<World> worldType;
    private Vec3i pos;
    private String key;
    private String msg;

    private transient String formatKey;

    public LogEvent() {
        super(Stage.Post);
    }

    public String formatKey() {
        if (formatKey == null) {
            formatKey = moduleName + "_" + worldType.getValue().getPath() + "_" + key + "_";
        }
        return formatKey;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public RegistryKey<World> getWorldType() {
        return worldType;
    }

    public void setWorldType(RegistryKey<World> worldType) {
        this.worldType = worldType;
    }

    public Vec3i getPos() {
        return pos;
    }

    public void setPos(Vec3i pos) {
        this.pos = pos;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogEvent logEvent = (LogEvent) o;

        if (!Objects.equals(moduleName, logEvent.moduleName)) return false;
        if (!Objects.equals(pos, logEvent.pos)) return false;
        return Objects.equals(msg, logEvent.msg);
    }

    @Override
    public int hashCode() {
        int result = moduleName != null ? moduleName.hashCode() : 0;
        result = 31 * result + (pos != null ? pos.hashCode() : 0);
        result = 31 * result + (msg != null ? msg.hashCode() : 0);
        return result;
    }
}
