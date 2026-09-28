package com.xiaohe66.mc.meteor.lotus.modules.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static net.minecraft.enchantment.Enchantments.*;

/**
 * @author He
 * @since 2025.06.13 21:34
 */
public enum AutoEnchantmentType {

    /**
     * 自定义配置
     */
    自定义(),

    /**
     * FIRE_PROTECTION = 火焰保护
     * PROJECTILE_PROTECTION = 弹射物保护
     * <p>
     * <p>
     * FROST_WALKER = 冰霜行者
     * BINDING_CURSE = 绑定诅咒
     * SOUL_SPEED = 灵魂疾行
     * SWIFT_SNEAK = 迅捷潜行
     * SMITE = 亡灵杀手
     * BANE_OF_ARTHROPODS = 节肢杀手
     * POWER = 力量
     * PUNCH = 冲击
     * FLAME = 火矢
     * INFINITY = 无限
     * LUCK_OF_THE_SEA = 海之眷顾
     * LURE = 饵钓
     * LOYALTY = 忠诚
     * IMPALING = 穿刺
     * RIPTIDE = 激流
     * CHANNELING = 引雷
     * MULTISHOT = 多重射击
     * QUICK_CHARGE = 快速装填
     * PIERCING = 穿透
     * DENSITY = 密度
     * BREACH = 贯穿
     * WIND_BURST = 风爆
     * VANISHING_CURSE = 消失诅咒
     * <p>
     * RESPIRATION = 水下呼吸
     * AQUA_AFFINITY = 水下速掘
     * DEPTH_STRIDER = 深海探索者
     * <p>
     * PROTECTION = 保护
     * FEATHER_FALLING = 摔落缓冲
     * BLAST_PROTECTION = 爆炸保护
     * THORNS = 荆棘
     * <p>
     * FORTUNE = 时运
     * SILK_TOUCH = 精准采集
     * EFFICIENCY = 效率
     * <p>
     * SHARPNESS = 锋利
     * KNOCKBACK = 击退
     * FIRE_ASPECT = 火焰附加
     * LOOTING = 抢夺
     * SWEEPING_EDGE = 横扫之刃
     * <p>
     * UNBREAKING = 耐久
     * MENDING = 经验修补
     */
    精准工具(EFFICIENCY, UNBREAKING, MENDING, SILK_TOUCH),
    时运工具(FORTUNE, UNBREAKING, MENDING, EFFICIENCY),
    生电剑(SWEEPING_EDGE, LOOTING, UNBREAKING, SHARPNESS, MENDING),
    击退剑(SWEEPING_EDGE, LOOTING, SHARPNESS, FIRE_ASPECT, UNBREAKING, KNOCKBACK, MENDING),
    头盔(RESPIRATION, PROTECTION, AQUA_AFFINITY, UNBREAKING, MENDING),
    胸甲(PROTECTION, UNBREAKING, MENDING),
    荆棘胸甲(THORNS, UNBREAKING, MENDING, PROTECTION),
    裤子(PROTECTION, UNBREAKING, MENDING),
    爆炸裤子(BLAST_PROTECTION, UNBREAKING, MENDING),
    鞋子(DEPTH_STRIDER, PROTECTION, UNBREAKING, FEATHER_FALLING, MENDING),
    爆炸鞋子(BLAST_PROTECTION, DEPTH_STRIDER, UNBREAKING, FEATHER_FALLING, MENDING),
    鞘翅(UNBREAKING, MENDING),
    三叉戟(IMPALING, CHANNELING, LOYALTY, UNBREAKING, MENDING),
    无限弓(POWER, INFINITY, UNBREAKING);

    private final Set<RegistryKey<Enchantment>> enchantments;

    @SafeVarargs
    AutoEnchantmentType(RegistryKey<Enchantment>... enchantments) {
        if (enchantments != null && enchantments.length > 0) {
            // 过滤掉 null 值
            this.enchantments = Arrays.stream(enchantments)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        } else {
            this.enchantments = Collections.emptySet();
        }
    }

    /**
     * 获取该预设包含的所有附魔
     */
    public Set<RegistryKey<Enchantment>> getAll() {
        return enchantments;
    }
}
