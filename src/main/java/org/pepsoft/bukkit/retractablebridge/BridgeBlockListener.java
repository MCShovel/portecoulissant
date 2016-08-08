/*
 * RetractableBridge - a Bukkit plugin for creating working retractable bridges
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
package org.pepsoft.bukkit.retractablebridge;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import static org.bukkit.block.BlockFace.*;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.pepsoft.bukkit.PortcullisPlugin;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.bukkit.retractablebridge.Directions.*;

/**
 *
 * @author pepijn
 */
public class BridgeBlockListener implements Listener {
    public BridgeBlockListener(PortcullisPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority= EventPriority.MONITOR)
    public void onBlockRedstoneChange(BlockRedstoneEvent event) {
        try {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "[RetractableBridge] BridgeBlockListener.onBlockRedstoneChange() (thread: " + Thread.currentThread() + ")", new Throwable());
            }
            Block block = event.getBlock();
            Location location = block.getLocation();
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("[RetractableBridge] Redstone event on block @ " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ", type: " + block.getType() + "; " + event.getOldCurrent() + " -> " + event.getNewCurrent());
            }
            if (! ((event.getOldCurrent() == 0) || (event.getNewCurrent() == 0))) {
                // Not a power on or off event
                return;
            }
            if (! CONDUCTIVE.contains(block.getTypeId())) {
                logger.fine("[RetractableBridge] Block @ " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ", type: " + block.getType() + " not conductive; ignoring");
                return;
            }
            boolean powerOn = event.getOldCurrent() == 0;
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("[RetractableBridge] Block powered " + (powerOn ? "on" : "off") + " @ " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ", type: " + block.getType());
            }
            for (BlockFace direction: SEARCH_DIRECTIONS) {
                Bridge bridge = findBridge(block, direction);
                if (bridge != null) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("[RetractableBridge] Bridge found! (x: " + bridge.getX() + ", z: " + bridge.getZ() + ", y: " + bridge.getY() + ", width: " + bridge.getWidth() + ", height: " + bridge.getHeight() + ", material: " + bridge.getMaterial() + ")");
                    }
                    Set<BlockFace> blockedDirections = bridge.getBlockedDirections();
                    if (powerOn) {
                        if (blockedDirections.size() == 3) {
                            if (! blockedDirections.contains(SOUTH)) {
                                move(bridge, SOUTH);
                            } else if (! blockedDirections.contains(EAST)) {
                                move(bridge, EAST);
                            } else {
                                if (logger.isLoggable(Level.FINE)) {
                                    logger.fine("[RetractableBridge] Bridge is blocked to the south and east; no reason to start moving");
                                }
                            }
                        } else {
                            if (blockedDirections.contains(NORTH) && blockedDirections.contains(SOUTH)) {
                                move(bridge, EAST);
                            } else {
                                move(bridge, SOUTH);
                            }
                        }
                    } else {
                        if (blockedDirections.size() == 3) {
                            if (! blockedDirections.contains(NORTH)) {
                                move(bridge, NORTH);
                            } else if (! blockedDirections.contains(WEST)) {
                                move(bridge, WEST);
                            } else {
                                if (logger.isLoggable(Level.FINE)) {
                                    logger.fine("[RetractableBridge] Bridge is blocked to the north and west; no reason to start moving");
                                }
                            }
                        } else {
                            if (blockedDirections.contains(NORTH) && blockedDirections.contains(SOUTH)) {
                                move(bridge, WEST);
                            } else {
                                move(bridge, NORTH);
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "[RetractableBridge] Exception thrown while handling redstone event!", t);
        }
    }

    private Bridge findBridge(Block block, BlockFace direction) {
        block = block.getRelative(direction);
        if (plugin.isAllPowerBlocksAllowed() ? (! WALL_MATERIALS.contains(block.getTypeId())) : (! plugin.getPowerBlocks().contains(block.getTypeId()))) {
            return null;
        }
        // Find a bridge above the block. A bridge is a rectangular area of
        // slabs or double slabs, parallel to the ground. It can have no holes
        // or bits sticking out, and it must be constrained on three sides, or
        // on two opposite sides.
        block = block.getRelative(UP);
        if (isPotentialBridgeBlock(block.getTypeId())) {
            int x = block.getX();
            int y = block.getY();
            int z = block.getZ();
            int width = 1, height = 1;
            int type = block.getTypeId();
            byte material = block.getData();
            Block adjacentBlock = block.getRelative(ACTUAL_WEST);
            while (isBridgeBlock(adjacentBlock, type, material)) {
                x--;
                width++;
                adjacentBlock = adjacentBlock.getRelative(ACTUAL_WEST);
            }
            adjacentBlock = block.getRelative(ACTUAL_NORTH);
            while (isBridgeBlock(adjacentBlock, type, material)) {
                z--;
                height++;
                adjacentBlock = adjacentBlock.getRelative(ACTUAL_NORTH);
            }
            adjacentBlock = block.getRelative(ACTUAL_EAST);
            while (isBridgeBlock(adjacentBlock, type, material)) {
                width++;
                adjacentBlock = adjacentBlock.getRelative(ACTUAL_EAST);
            }
            adjacentBlock = block.getRelative(ACTUAL_SOUTH);
            while (isBridgeBlock(adjacentBlock, type, material)) {
                height++;
                adjacentBlock = adjacentBlock.getRelative(ACTUAL_SOUTH);
            }
            if ((width >= 2) && (height >= 2)) {
                // Check the integrity of the bridge, and also its degrees of
                // freedom
                World world = block.getWorld();
                Set<BlockFace> blockedDirections = EnumSet.noneOf(BlockFace.class);
                for (int dz = 0; dz < height; dz++) {
                    Block edgeBlock = world.getBlockAt(x - 1, y, z + dz);
                    if (isBridgeBlock(edgeBlock, type, material)) {
                        // Bit of material sticking out
                        return null;
                    } else if (! isAir(edgeBlock)) {
                        blockedDirections.add(WEST);
                    }
                    edgeBlock = world.getBlockAt(x + width, y, z + dz);
                    if (isBridgeBlock(edgeBlock, type, material)) {
                        // Bit of material sticking out
                        return null;
                    } else if (! isAir(edgeBlock)) {
                        blockedDirections.add(EAST);
                    }
                }
                for (int dx = 0; dx < width; dx++) {
                    Block edgeBlock = world.getBlockAt(x + dx, y, z - 1);
                    if (isBridgeBlock(edgeBlock, type, material)) {
                        // Bit of material sticking out
                        return null;
                    } else if (! isAir(edgeBlock)) {
                        blockedDirections.add(NORTH);
                    }
                    for (int dz = 0; dz < height; dz++) {
                        Block bridgeBlock = world.getBlockAt(x + dx, y, z + dz);
                        if (! isBridgeBlock(bridgeBlock, type, material)) {
                            // Bit of material missing inside bridge
                            return null;
                        }
                    }
                    edgeBlock = world.getBlockAt(x + dx, y, z + height);
                    if (isBridgeBlock(edgeBlock, type, material)) {
                        // Bit of material sticking out
                        return null;
                    } else if (! isAir(edgeBlock)) {
                        blockedDirections.add(SOUTH);
                    }
                }
                // The bridge is whole and has no bits sticking out. If it is
                // blocked in three directions, or in two opposing directions,
                // it is a valid bridge!
                if ((blockedDirections.size() == 3)
                    || ((blockedDirections.size() == 2)
                        && ((blockedDirections.contains(NORTH) && blockedDirections.contains(SOUTH))
                            || (blockedDirections.contains(EAST) && blockedDirections.contains(WEST))))) {
                    return new Bridge(world.getName(), x, z, y, width, height, blockedDirections, type, material);
                }
            }
        }
        return null;
    }

    private boolean isPotentialBridgeBlock(int blockType) {
        return plugin.getBridgeMaterials().contains(blockType);
    }

    private boolean isBridgeBlock(Block block, int type, byte material) {
        return (block.getTypeId() == type) && (block.getData() == material);
    }

    private boolean isAir(Block block) {
        return AIR_MATERIALS.contains(block.getTypeId());
    }

    private void move(final Bridge bridge, BlockFace direction) {
        // Check whether the bridge is already known
        BridgeMover mover = null;
        for (BridgeMover potentialMover: bridgeMovers) {
            if (potentialMover.getBridge().equals(bridge)) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("[RetractableBridge] Using existing bridge mover");
                }
                // Set the bridge, since the one cached by the bridge mover may
                // have been made from a different material
                potentialMover.setBridge(bridge);
                mover = potentialMover;
            }
        }
        if (mover == null) {
            // It isn't
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("[RetractableBridge] Creating new bridge mover");
            }
            mover = new BridgeMover(plugin, bridge);
            bridgeMovers.add(mover);
        }
        mover.move(direction);
    }

    private final PortcullisPlugin plugin;
    private final Set<BridgeMover> bridgeMovers = new HashSet<BridgeMover>();

    static final Set<Integer> AIR_MATERIALS = new HashSet<Integer>(Arrays.asList(
        BLK_AIR, BLK_WATER, BLK_STATIONARY_WATER, BLK_LAVA, BLK_STATIONARY_LAVA,
        BLK_SUGAR_CANE, BLK_SNOW, BLK_DANDELION, BLK_ROSE, BLK_BROWN_MUSHROOM, BLK_RED_MUSHROOM,
        BLK_FIRE, BLK_WHEAT, BLK_TALL_GRASS, BLK_DEAD_SHRUBS, BLK_COBWEB, BLK_PUMPKIN_STEM,
        BLK_MELON_STEM, BLK_VINES, BLK_LILY_PAD, BLK_NETHER_WART, BLK_CARROTS, BLK_POTATOES,
        BLK_DOUBLE_PLANT));
    static final Set<Integer> WALL_MATERIALS = new HashSet<Integer>(Arrays.asList(
        BLK_SAND, BLK_GRAVEL,
        BLK_STONE, BLK_GRASS, BLK_DIRT, BLK_COBBLESTONE, BLK_WOODEN_PLANK, BLK_BEDROCK,
        BLK_GOLD_ORE, BLK_IRON_ORE, BLK_COAL, BLK_WOOD, BLK_SPONGE, BLK_GLASS, BLK_LAPIS_LAZULI_ORE,
        BLK_LAPIS_LAZULI_BLOCK, BLK_SANDSTONE, BLK_WOOL, BLK_GOLD_BLOCK, BLK_IRON_BLOCK,
        BLK_DOUBLE_SLAB, BLK_BRICK_BLOCK, BLK_BOOKSHELF, BLK_MOSSY_COBBLESTONE,
        BLK_OBSIDIAN, BLK_CHEST, BLK_DIAMOND_ORE, BLK_DIAMOND_BLOCK, BLK_CRAFTING_TABLE,
        BLK_TILLED_DIRT, BLK_FURNACE, BLK_BURNING_FURNACE, BLK_REDSTONE_ORE, BLK_GLOWING_REDSTONE_ORE,
        BLK_ICE, BLK_SNOW_BLOCK, BLK_CLAY, BLK_JUKEBOX, BLK_NETHERRACK, BLK_SOUL_SAND,
        BLK_GLOWSTONE, BLK_HIDDEN_SILVERFISH, BLK_STONE_BRICKS, BLK_MYCELIUM, BLK_NETHER_BRICK,
        BLK_END_PORTAL_FRAME, BLK_END_STONE, BLK_REDSTONE_LAMP_OFF, BLK_REDSTONE_LAMP_ON,
        BLK_WOODEN_DOUBLE_SLAB, BLK_EMERALD_BLOCK, BLK_EMERALD_ORE, BLK_ENDER_CHEST,
        BLK_TRAPPED_CHEST, BLK_REDSTONE_BLOCK, BLK_QUARTZ_ORE, BLK_QUARTZ_BLOCK,
        BLK_STAINED_CLAY, BLK_WOOD_2, BLK_HAY_BALE, BLK_HARDENED_CLAY, BLK_COAL_BLOCK,
        BLK_PACKED_ICE));

    private static final BlockFace[] SEARCH_DIRECTIONS = {UP, NORTH, EAST, SOUTH, WEST};
    private static final Set<Integer> CONDUCTIVE = new HashSet<Integer>(Arrays.asList(
        BLK_REDSTONE_WIRE, BLK_REDSTONE_TORCH_ON, BLK_REDSTONE_TORCH_OFF, BLK_REDSTONE_REPEATER_ON,
        BLK_REDSTONE_REPEATER_OFF, BLK_STONE_BUTTON, BLK_LEVER, BLK_STONE_PRESSURE_PLATE,
        BLK_WOODEN_PRESSURE_PLATE, BLK_TRIPWIRE_HOOK, BLK_WOODEN_BUTTON, BLK_TRAPPED_CHEST,
        BLK_REDSTONE_COMPARATOR_OFF, BLK_REDSTONE_COMPARATOR_ON, BLK_WEIGHTED_PRESSURE_PLATE_LIGHT,
        BLK_WEIGHTED_PRESSURE_PLATE_HEAVY));
    private static final Logger logger = PortcullisPlugin.logger;
}