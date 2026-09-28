package com.xiaohe66.mc.meteor.lotus.modules.villager;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.village.VillagerProfession;

import java.util.List;
import java.util.Set;

/**
 * @author He
 * @since 2025.06.11 21:49
 */
public enum VillagerType {

    盔甲匠(Items.BLAST_FURNACE, VillagerProfession.ARMORER,
            Set.of(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS),
            List.of(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS)),

    屠夫(Items.SMOKER, VillagerProfession.BUTCHER,
            Set.of(Items.RABBIT_STEW, Items.COOKED_CHICKEN, Items.COOKED_PORKCHOP),
            List.of(Items.COOKED_PORKCHOP)),

    制图师(Items.CARTOGRAPHY_TABLE, VillagerProfession.CARTOGRAPHER,
            Set.of(Items.MAP, Items.ITEM_FRAME,
                    Items.WHITE_BANNER,
                    Items.ORANGE_BANNER,
                    Items.YELLOW_BANNER,
                    Items.LIGHT_GRAY_BANNER,
                    Items.RED_BANNER,
                    Items.BROWN_BANNER,
                    Items.MAGENTA_BANNER,
                    Items.LIGHT_BLUE_BANNER,
                    Items.LIME_BANNER,
                    Items.PINK_BANNER,
                    Items.GRAY_BANNER,
                    Items.CYAN_BANNER,
                    Items.PURPLE_BANNER,
                    Items.BLUE_BANNER,
                    Items.GREEN_BANNER,
                    Items.BLACK_BANNER),
            List.of(Items.MAP)),

    牧师(Items.BREWING_STAND, VillagerProfession.CLERIC,
            Set.of(Items.REDSTONE, Items.LAPIS_LAZULI, Items.GLOWSTONE, Items.ENDER_PEARL, Items.EXPERIENCE_BOTTLE),
            List.of(Items.EXPERIENCE_BOTTLE, Items.ENDER_PEARL)),

    农民(Items.COMPOSTER, VillagerProfession.FARMER,
            Set.of(Items.BREAD, Items.PUMPKIN_PIE, Items.APPLE, Items.COOKIE, Items.GOLDEN_CARROT),
            List.of(Items.GOLDEN_CARROT)),

    /*渔夫(Items.BARREL, VillagerProfession.FISHERMAN,
            Set.of(Items.CAMPFIRE),
            List.of(Items.CAMPFIRE)),

    制箭师(Items.FLETCHING_TABLE, VillagerProfession.FLETCHER,
            Set.of(Items.ARROW, Items.SPECTRAL_ARROW, Items.TIPPED_ARROW, Items.BOW, Items.CROSSBOW),
            List.of(Items.ARROW)),

    皮匠(Items.CAULDRON, VillagerProfession.LEATHERWORKER,
            Set.of(Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS, Items.LEATHER_HORSE_ARMOR),
            List.of(Items.LEATHER_CHESTPLATE)),*/

    图书管理员(Items.LECTERN, VillagerProfession.LIBRARIAN,
            Set.of(Items.BOOKSHELF, Items.LANTERN, Items.GLASS, Items.COMPASS, Items.NAME_TAG),
            List.of(Items.GLASS)),

    石匠(Items.STONECUTTER, VillagerProfession.MASON,
            Set.of(Items.BRICK, Items.CHISELED_STONE_BRICKS, Items.DRIPSTONE_BLOCK, Items.POLISHED_ANDESITE, Items.POLISHED_DIORITE, Items.POLISHED_GRANITE,
                    Items.TERRACOTTA,

                    Items.WHITE_TERRACOTTA,
                    Items.ORANGE_TERRACOTTA,
                    Items.YELLOW_TERRACOTTA,
                    Items.LIGHT_GRAY_TERRACOTTA,
                    Items.RED_TERRACOTTA,
                    Items.BROWN_TERRACOTTA,
                    Items.MAGENTA_TERRACOTTA,
                    Items.LIGHT_BLUE_TERRACOTTA,
                    Items.LIME_TERRACOTTA,
                    Items.PINK_TERRACOTTA,
                    Items.GRAY_TERRACOTTA,
                    Items.CYAN_TERRACOTTA,
                    Items.PURPLE_TERRACOTTA,
                    Items.BLUE_TERRACOTTA,
                    Items.GREEN_TERRACOTTA,
                    Items.BLACK_TERRACOTTA,

                    Items.WHITE_GLAZED_TERRACOTTA,
                    Items.ORANGE_GLAZED_TERRACOTTA,
                    Items.YELLOW_GLAZED_TERRACOTTA,
                    Items.LIGHT_GRAY_GLAZED_TERRACOTTA,
                    Items.RED_GLAZED_TERRACOTTA,
                    Items.BROWN_GLAZED_TERRACOTTA,
                    Items.MAGENTA_GLAZED_TERRACOTTA,
                    Items.LIGHT_BLUE_GLAZED_TERRACOTTA,
                    Items.LIME_GLAZED_TERRACOTTA,
                    Items.PINK_GLAZED_TERRACOTTA,
                    Items.GRAY_GLAZED_TERRACOTTA,
                    Items.CYAN_GLAZED_TERRACOTTA,
                    Items.PURPLE_GLAZED_TERRACOTTA,
                    Items.BLUE_GLAZED_TERRACOTTA,
                    Items.GREEN_GLAZED_TERRACOTTA,
                    Items.BLACK_GLAZED_TERRACOTTA,

                    Items.QUARTZ_PILLAR, Items.QUARTZ_BLOCK),
            List.of()),

    牧羊人(Items.LOOM, VillagerProfession.SHEPHERD,
            Set.of(Items.SHEARS,
                    Items.WHITE_WOOL,
                    Items.ORANGE_WOOL,
                    Items.MAGENTA_WOOL,
                    Items.LIGHT_BLUE_WOOL,
                    Items.YELLOW_WOOL,
                    Items.LIME_WOOL,
                    Items.PINK_WOOL,
                    Items.GRAY_WOOL,
                    Items.LIGHT_GRAY_WOOL,
                    Items.CYAN_WOOL,
                    Items.PURPLE_WOOL,
                    Items.BLUE_WOOL,
                    Items.BROWN_WOOL,
                    Items.GREEN_WOOL,
                    Items.RED_WOOL,
                    Items.BLACK_WOOL,
                    Items.WHITE_CARPET,
                    Items.ORANGE_CARPET,
                    Items.MAGENTA_CARPET,
                    Items.LIGHT_BLUE_CARPET,
                    Items.YELLOW_CARPET,
                    Items.LIME_CARPET,
                    Items.PINK_CARPET,
                    Items.GRAY_CARPET,
                    Items.LIGHT_GRAY_CARPET,
                    Items.CYAN_CARPET,
                    Items.PURPLE_CARPET,
                    Items.BLUE_CARPET,
                    Items.BROWN_CARPET,
                    Items.GREEN_CARPET,
                    Items.RED_CARPET,
                    Items.BLACK_CARPET,
                    Items.WHITE_BED,
                    Items.ORANGE_BED,
                    Items.MAGENTA_BED,
                    Items.LIGHT_BLUE_BED,
                    Items.YELLOW_BED,
                    Items.LIME_BED,
                    Items.PINK_BED,
                    Items.GRAY_BED,
                    Items.LIGHT_GRAY_BED,
                    Items.CYAN_BED,
                    Items.PURPLE_BED,
                    Items.BLUE_BED,
                    Items.BROWN_BED,
                    Items.GREEN_BED,
                    Items.RED_BED,
                    Items.BLACK_BED,
                    Items.WHITE_BANNER,
                    Items.ORANGE_BANNER,
                    Items.YELLOW_BANNER,
                    Items.LIGHT_GRAY_BANNER,
                    Items.RED_BANNER,
                    Items.BROWN_BANNER,
                    Items.MAGENTA_BANNER,
                    Items.LIGHT_BLUE_BANNER,
                    Items.LIME_BANNER,
                    Items.PINK_BANNER,
                    Items.GRAY_BANNER,
                    Items.CYAN_BANNER,
                    Items.PURPLE_BANNER,
                    Items.BLUE_BANNER,
                    Items.GREEN_BANNER,
                    Items.BLACK_BANNER,
                    Items.PAINTING),
            List.of(Items.WHITE_WOOL)),

    工具匠(Items.SMITHING_TABLE, VillagerProfession.TOOLSMITH,
            Set.of(Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_PICKAXE),
            List.of(Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_PICKAXE)),

    武器匠(Items.GRINDSTONE, VillagerProfession.WEAPONSMITH,
            Set.of(Items.DIAMOND_AXE, Items.DIAMOND_SWORD),
            List.of(Items.DIAMOND_AXE, Items.DIAMOND_SWORD));

    private final Item item;
    private final RegistryKey<VillagerProfession> profession;
    private final Set<Item> canVillagerItemSet;
    private final List<Item> defaultVillagerItemList;

    VillagerType(Item item, RegistryKey<VillagerProfession> profession, Set<Item> canVillagerItemSet, List<Item> defaultVillagerItemList) {
        this.item = item;
        this.profession = profession;
        this.canVillagerItemSet = canVillagerItemSet;
        this.defaultVillagerItemList = defaultVillagerItemList;
    }

    public Item getItem() {
        return item;
    }

    public RegistryKey<VillagerProfession> getProfession() {
        return profession;
    }

    public Set<Item> getCanVillagerItemSet() {
        return canVillagerItemSet;
    }

    public List<Item> getDefaultVillagerItemList() {
        return defaultVillagerItemList;
    }

    public static VillagerType fromEntry(RegistryEntry<VillagerProfession> registryEntry) {
        for (VillagerType type : values()) {
            if (registryEntry.matchesKey(type.getProfession())) {
                return type;
            }
        }
        return null;
    }
}
