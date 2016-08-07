/*
 * PorteCoulissante - a Bukkit plugin for creating working portcullises
 * Copyright 2010, 2012, 2014  Pepijn Schmitz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.pepsoft.minecraft;

/**
 *
 * @author pepijn
 */
public final class Constants {
    private Constants() {
        // Prevent instantiation
    }
    
    public static final int BLK_AIR                   =   0;
    public static final int BLK_STONE                 =   1;
    public static final int BLK_GRASS                 =   2;
    public static final int BLK_DIRT                  =   3;
    public static final int BLK_COBBLESTONE           =   4;
    public static final int BLK_WOODEN_PLANK          =   5;
    public static final int BLK_SAPLING               =   6;
    public static final int BLK_BEDROCK               =   7;
    public static final int BLK_WATER                 =   8;
    public static final int BLK_STATIONARY_WATER      =   9;
    public static final int BLK_LAVA                  =  10;
    public static final int BLK_STATIONARY_LAVA       =  11;
    public static final int BLK_SAND                  =  12;
    public static final int BLK_GRAVEL                =  13;
    public static final int BLK_GOLD_ORE              =  14;
    public static final int BLK_IRON_ORE              =  15;
    public static final int BLK_COAL                  =  16;
    public static final int BLK_WOOD                  =  17;
    public static final int BLK_LEAVES                =  18;
    public static final int BLK_SPONGE                =  19;
    public static final int BLK_GLASS                 =  20;
    public static final int BLK_LAPIS_LAZULI_ORE      =  21;
    public static final int BLK_LAPIS_LAZULI_BLOCK    =  22;
    public static final int BLK_DISPENSER             =  23;
    public static final int BLK_SANDSTONE             =  24;
    public static final int BLK_NOTE_BLOCK            =  25;
    public static final int BLK_BED                   =  26;
    public static final int BLK_POWERED_RAILS         =  27;
    public static final int BLK_DETECTOR_RAILS        =  28;
    public static final int BLK_STICKY_PISTON         =  29;
    public static final int BLK_COBWEB                =  30;
    public static final int BLK_TALL_GRASS            =  31;
    public static final int BLK_DEAD_SHRUBS           =  32;
    public static final int BLK_PISTON                =  33;
    public static final int BLK_PISTON_EXTENSION      =  34;
    public static final int BLK_WOOL                  =  35;
    
    public static final int BLK_DANDELION             =  37;
    public static final int BLK_ROSE                  =  38;
    public static final int BLK_BROWN_MUSHROOM        =  39;
    public static final int BLK_RED_MUSHROOM          =  40;
    public static final int BLK_GOLD_BLOCK            =  41;
    public static final int BLK_IRON_BLOCK            =  42;
    public static final int BLK_DOUBLE_SLAB           =  43;
    public static final int BLK_SLAB                  =  44;
    public static final int BLK_BRICK_BLOCK           =  45;
    public static final int BLK_TNT                   =  46;
    public static final int BLK_BOOKSHELF             =  47;
    public static final int BLK_MOSSY_COBBLESTONE     =  48;
    public static final int BLK_OBSIDIAN              =  49;
    public static final int BLK_TORCH                 =  50;
    public static final int BLK_FIRE                  =  51;
    public static final int BLK_MONSTER_SPAWNER       =  52;
    public static final int BLK_WOODEN_STAIRS         =  53;
    public static final int BLK_CHEST                 =  54;
    public static final int BLK_REDSTONE_WIRE         =  55;
    public static final int BLK_DIAMOND_ORE           =  56;
    public static final int BLK_DIAMOND_BLOCK         =  57;
    public static final int BLK_CRAFTING_TABLE        =  58;
    public static final int BLK_WHEAT                 =  59;
    public static final int BLK_TILLED_DIRT           =  60;
    public static final int BLK_FURNACE               =  61;
    public static final int BLK_BURNING_FURNACE       =  62;
    public static final int BLK_SIGN                  =  63;
    public static final int BLK_WOODEN_DOOR           =  64;
    public static final int BLK_LADDER                =  65;
    public static final int BLK_RAILS                 =  66;
    public static final int BLK_COBBLESTONE_STAIRS    =  67;
    public static final int BLK_WALL_SIGN             =  68;
    public static final int BLK_LEVER                 =  69;
    public static final int BLK_STONE_PRESSURE_PLATE  =  70;
    public static final int BLK_IRON_DOOR             =  71;
    public static final int BLK_WOODEN_PRESSURE_PLATE =  72;
    public static final int BLK_REDSTONE_ORE          =  73;
    public static final int BLK_GLOWING_REDSTONE_ORE  =  74;
    public static final int BLK_REDSTONE_TORCH_OFF    =  75;
    public static final int BLK_REDSTONE_TORCH_ON     =  76;
    public static final int BLK_STONE_BUTTON          =  77;
    public static final int BLK_SNOW                  =  78;
    public static final int BLK_ICE                   =  79;
    public static final int BLK_SNOW_BLOCK            =  80;
    public static final int BLK_CACTUS                =  81;
    public static final int BLK_CLAY                  =  82;
    public static final int BLK_SUGAR_CANE            =  83;
    public static final int BLK_JUKEBOX               =  84;
    public static final int BLK_FENCE                 =  85;
    public static final int BLK_PUMPKIN               =  86;
    public static final int BLK_NETHERRACK            =  87;
    public static final int BLK_SOUL_SAND             =  88;
    public static final int BLK_GLOWSTONE             =  89;
    public static final int BLK_PORTAL                =  90;
    public static final int BLK_JACK_O_LANTERN        =  91;
    public static final int BLK_CAKE                  =  92;
    public static final int BLK_REDSTONE_REPEATER_OFF =  93;
    public static final int BLK_REDSTONE_REPEATER_ON  =  94;
    public static final int BLK_LOCKED_CHEST          =  95;
    public static final int BLK_TRAPDOOR              =  96;
    public static final int BLK_HIDDEN_SILVERFISH     =  97;
    public static final int BLK_STONE_BRICKS          =  98;
    public static final int BLK_HUGE_BROWN_MUSHROOM   =  99;
    public static final int BLK_HUGE_RED_MUSHROOM     = 100;
    public static final int BLK_IRON_BARS             = 101;
    public static final int BLK_GLASS_PANE            = 102;
    public static final int BLK_MELON                 = 103;
    public static final int BLK_PUMPKIN_STEM          = 104;
    public static final int BLK_MELON_STEM            = 105;
    public static final int BLK_VINES                 = 106;
    public static final int BLK_FENCE_GATE            = 107;
    public static final int BLK_BRICK_STAIRS          = 108;
    public static final int BLK_STONE_BRICK_STAIRS    = 109;
    public static final int BLK_MYCELIUM              = 110;
    public static final int BLK_LILY_PAD              = 111;
    public static final int BLK_NETHER_BRICK          = 112;
    public static final int BLK_NETHER_BRICK_FENCE    = 113;
    public static final int BLK_NETHER_BRICK_STAIRS   = 114;
    public static final int BLK_NETHER_WART           = 115;
    public static final int BLK_ENCHANTMENT_TABLE     = 116;
    public static final int BLK_BREWING_STAND         = 117;
    public static final int BLK_CAULDRON              = 118;
    public static final int BLK_END_PORTAL            = 119;
    public static final int BLK_END_PORTAL_FRAME      = 120;
    public static final int BLK_END_STONE             = 121;
    public static final int BLK_DRAGON_EGG            = 122;
    public static final int BLK_REDSTONE_LAMP_OFF     = 123;
    public static final int BLK_REDSTONE_LAMP_ON      = 124;
    public static final int BLK_WOODEN_DOUBLE_SLAB    = 125;
    public static final int BLK_WOODEN_SLAB           = 126;
    public static final int BLK_COCOA_PLANT           = 127;
    public static final int BLK_SANDSTONE_STAIRS      = 128;
    public static final int BLK_EMERALD_ORE           = 129;
    public static final int BLK_ENDER_CHEST           = 130;
    public static final int BLK_TRIPWIRE_HOOK         = 131;

    public static final int BLK_EMERALD_BLOCK         = 133;
    public static final int BLK_SPRUCE_WOOD_STAIRS    = 134;
    public static final int BLK_BIRCH_WOOD_STAIRS     = 135;
    public static final int BLK_JUNGLE_WOOD_STAIRS    = 136;

    public static final int BLK_CARROTS               = 141;
    public static final int BLK_POTATOES              = 142;
    public static final int BLK_WOODEN_BUTTON         = 143;

    public static final int BLK_TRAPPED_CHEST                 = 146;
   public static final int BLK_WEIGHTED_PRESSURE_PLATE_LIGHT  = 147;
    public static final int BLK_WEIGHTED_PRESSURE_PLATE_HEAVY = 148;
    public static final int BLK_REDSTONE_COMPARATOR_OFF       = 149;
    public static final int BLK_REDSTONE_COMPARATOR_ON        = 150;

    public static final int BLK_REDSTONE_BLOCK        = 152;
    public static final int BLK_QUARTZ_ORE            = 153;

    public static final int BLK_QUARTZ_BLOCK          = 155;

    public static final int BLK_STAINED_CLAY          = 159;

    public static final int BLK_WOOD_2                = 162;

    public static final int BLK_HAY_BALE              = 170;

    public static final int BLK_HARDENED_CLAY         = 172;
    public static final int BLK_COAL_BLOCK            = 173;
    public static final int BLK_PACKED_ICE            = 174;
    public static final int BLK_DOUBLE_PLANT          = 175;
}