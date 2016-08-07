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
package org.pepsoft.bukkit.portcullis;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import static org.bukkit.block.BlockFace.*;
import org.bukkit.event.block.BlockRedstoneEvent;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.bukkit.portcullis.Directions.*;

/**
 *
 * @author pepijn
 */
public class PortcullisBlockListener implements Listener {
    public PortcullisBlockListener(PortcullisPlugin plugin) {
        this.plugin = plugin;
        wallMaterials.addAll(plugin.getAdditionalWallMaterials());
    }

    @EventHandler(priority= EventPriority.MONITOR)
    public void onBlockRedstoneChange(BlockRedstoneEvent event) {
        try {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "[PorteCoulissante] PortcullisBlockListener.onBlockRedstoneChange() (thread: " + Thread.currentThread() + ")", new Throwable());
            }
            Block block = event.getBlock();
            Location location = block.getLocation();
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("[PorteCoulissante] Redstone event on block @ " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ", type: " + block.getType() + "; " + event.getOldCurrent() + " -> " + event.getNewCurrent());
                if (logger.isLoggable(Level.FINEST)) {
                    logger.finest("[PorteCoulissante] Type according to World.getBlockAt(): " + block.getWorld().getBlockAt(location).getType());
                    logger.finest("[PorteCoulissante] Type according to World.getBlockTypeIdAt(): " + Material.getMaterial(block.getWorld().getBlockTypeIdAt(location)));
                }
            }
            if (! ((event.getOldCurrent() == 0) || (event.getNewCurrent() == 0))) {
                // Not a power on or off event
                return;
            }
            if (! CONDUCTIVE.contains(block.getTypeId())) {
                logger.fine("[PorteCoulissante] Block @ " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ", type: " + block.getType() + " not conductive; ignoring");
                return;
            }
            boolean powerOn = event.getOldCurrent() == 0;
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("[PorteCoulissante] Block powered " + (powerOn ? "on" : "off"));
            }
            for (BlockFace direction: CARDINAL_DIRECTIONS) {
                Portcullis portCullis = findPortcullisInDirection(block, direction);
                if (portCullis != null) {
                    portCullis = normalisePortcullis(portCullis);
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("[PorteCoulissante] Portcullis found! (x: " + portCullis.getX() + ", z: " + portCullis.getZ() + ", y: " + portCullis.getY() + ", width: " + portCullis.getWidth() + ", height: " + portCullis.getHeight() + ", direction: " + portCullis.getDirection() + ")");
                        if (logger.isLoggable(Level.FINEST)) {
                            logger.finest("[PorteCoulissante] According to Bukkit cache:");
                            World world = block.getWorld();
                            for (int y = portCullis.getY() + portCullis.getHeight() + 4; y >= portCullis.getY() - 5; y--) {
                                StringBuilder sb = new StringBuilder("[PorteCoulissante] ");
                                sb.append(y);
                                for (int i = -5; i <= portCullis.getWidth() + 4; i++) {
                                    sb.append('|');
                                    sb.append(world.getBlockAt(portCullis.getX() + i * portCullis.getDirection().getModX(), y, portCullis.getZ() + i * portCullis.getDirection().getModZ()).getType().name().substring(0, 2));
                                }
                                logger.finest(sb.toString());
                            }
                            logger.finest("[PorteCoulissante] According to Minecraft:");
                            for (int y = portCullis.getY() + portCullis.getHeight() + 4; y >= portCullis.getY() - 5; y--) {
                                StringBuilder sb = new StringBuilder("[PorteCoulissante] ");
                                sb.append(y);
                                for (int i = -5; i <= portCullis.getWidth() + 4; i++) {
                                    sb.append('|');
                                    sb.append(Material.getMaterial(world.getBlockTypeIdAt(portCullis.getX() + i * portCullis.getDirection().getModX(), y, portCullis.getZ() + i * portCullis.getDirection().getModZ())).name().substring(0, 2));
                                }
                                logger.finest(sb.toString());
                            }
                        }
                    }
                    if (powerOn) {
                        hoistPortcullis(portCullis);
                    } else {
                        dropPortcullis(portCullis);
                    }
                }
            }
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "[PorteCoulissante] Exception thrown while handling redstone event!", t);
        }
    }

    private Portcullis findPortcullisInDirection(Block block, BlockFace direction) {
        BlockFace actualDirection = actual(direction);
        Block powerBlock = block.getRelative(actualDirection);
        int powerBlockType = powerBlock.getTypeId();
        if (isPotentialPowerBlock(powerBlockType)) {
            byte powerBlockData = powerBlock.getData();
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("[PorteCoulissante] Potential power block found (type: " + powerBlockType + ", data: " + powerBlockData + ")");
            }
            Block firstPortcullisBlock = powerBlock.getRelative(actualDirection);
            if (isPotentialPortcullisBlock(firstPortcullisBlock)) {
                int portcullisType = firstPortcullisBlock.getTypeId();
                byte portcullisData = firstPortcullisBlock.getData();
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("[PorteCoulissante] Potential portcullis block found (type: " + portcullisType + ", data: " + portcullisData + ")");
                }
                if ((portcullisType == powerBlockType) && (portcullisData == powerBlockData)) {
                    // The portcullis can't be made of the same blocks as its frame
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("[PorteCoulissante] Potential portcullis block is same type as wall; aborting");
                    }
                    return null;
                }
                Block lastPortCullisBlock = firstPortcullisBlock.getRelative(actualDirection);
                if (isPortcullisBlock(portcullisType, portcullisData, lastPortCullisBlock)) {
                    int width = 2;
                    Block nextBlock = lastPortCullisBlock.getRelative(actualDirection);
                    while (isPortcullisBlock(portcullisType, portcullisData, nextBlock)) {
                        width++;
                        lastPortCullisBlock = nextBlock;
                        nextBlock = lastPortCullisBlock.getRelative(actualDirection);
                    }
                    // At least two fences found in a row. Now search up and down
                    int highestY = firstPortcullisBlock.getLocation().getBlockY();
                    Block nextBlockUp = firstPortcullisBlock.getRelative(UP);
                    while (isPortcullisBlock(portcullisType, portcullisData, nextBlockUp)) {
                        highestY++;
                        nextBlockUp = nextBlockUp.getRelative(UP);
                    }
                    int lowestY = firstPortcullisBlock.getLocation().getBlockY();
                    Block nextBlockDown = firstPortcullisBlock.getRelative(DOWN);
                    while (isPortcullisBlock(portcullisType, portcullisData, nextBlockDown)) {
                        lowestY--;
                        nextBlockDown = nextBlockDown.getRelative(DOWN);
                    }
                    int height = highestY - lowestY + 1;
                    if (height >= 2) {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("[PorteCoulissante] Found potential portcullis of width " + width + " and height " + height);
                        }
                        int x = firstPortcullisBlock.getX();
                        int y = lowestY;
                        int z = firstPortcullisBlock.getZ();
                        World world = firstPortcullisBlock.getWorld();
                        // Check the integrity of the portcullis
                        for (int i = -1; i <= width; i++) {
                            for (int dy = -1; dy <= height; dy++) {
                                if ((((i == -1) || (i == width)) && (dy != -1) && (dy != height))
                                        || (((dy == -1) || (dy == height)) && (i != -1) && (i != width))) {
                                    // This is one of the blocks to the sides or above or below of the portcullis
                                    Block frameBlock = world.getBlockAt(x + i * actualDirection.getModX(), y + dy, z + i * actualDirection.getModZ());
                                    if (isPortcullisBlock(portcullisType, portcullisData, frameBlock)) {
                                        if (logger.isLoggable(Level.FINE)) {
                                            logger.fine("[PorteCoulissante] Block of same type as potential portcullis found in frame; aborting");
                                        }
                                        return null;
                                    }
                                } else if ((i >= 0) && (i < width) && (dy >= 0) && (dy < height)) {
                                    // This is a portcullis block
                                    Block portcullisBlock = world.getBlockAt(x + i * actualDirection.getModX(), y + dy, z + i * actualDirection.getModZ());
                                    if (! isPortcullisBlock(portcullisType, portcullisData, portcullisBlock)) {
                                        if (logger.isLoggable(Level.FINE)) {
                                            logger.fine("[PorteCoulissante] Block of wrong type (" + portcullisBlock.getTypeId() + ") found inside potential portcullis; aborting");
                                        }
                                        return null;
                                    }
                                }
                            }
                        }
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("[PorteCoulissante] Portcullis found! Location: " + x + ", " + y + ", " + z + ", width: " + width + ", height: " + height + ", direction: " + direction + ", type: " + portcullisType + ", data: " + portcullisData);
                        }
                        return new Portcullis(world.getName(), x, z, y, width, height, direction, portcullisType, portcullisData);
                    }
                }
            }
        }
        return null;
    }

    private boolean isPotentialPowerBlock(int wallType) {
        return plugin.isAllPowerBlocksAllowed() ? wallMaterials.contains(wallType) : plugin.getPowerBlocks().contains(wallType);
    }
    
    private boolean isPotentialPortcullisBlock(Block block) {
        return plugin.getPortcullisMaterials().contains(block.getTypeId());
    }

    private boolean isPortcullisBlock(int portcullisType, byte portcullisData, Block block) {
        return (block.getTypeId() == portcullisType) && (block.getData() == portcullisData);
    }
    
    private Portcullis normalisePortcullis(Portcullis portcullis) {
        if (portcullis.getDirection() == WEST) {
            return new Portcullis(portcullis.getWorldName(), portcullis.getX() - portcullis.getWidth() + 1, portcullis.getZ(), portcullis.getY(), portcullis.getWidth(), portcullis.getHeight(), EAST, portcullis.getType(), portcullis.getData());
        } else if (portcullis.getDirection() == NORTH) {
            return new Portcullis(portcullis.getWorldName(), portcullis.getX(), portcullis.getZ() - portcullis.getWidth() + 1, portcullis.getY(), portcullis.getWidth(), portcullis.getHeight(), SOUTH, portcullis.getType(), portcullis.getData());
        } else {
            return portcullis;
        }
    }

    private void hoistPortcullis(final Portcullis portcullis) {
        // Check whether the portcullis is already known
        for (PortcullisMover mover: portcullisMovers) {
            if (mover.getPortcullis().equals(portcullis)) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("[PorteCoulissante] Reusing existing portcullis mover");
                }
                // Set the portcullis, because the one cached by the portcullis
                // mover may be made from a different material
                mover.setPortcullis(portcullis);
                mover.hoist();
                return;
            }
        }
        // It isn't
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("[PorteCoulissante] Creating new portcullis mover");
        }
        PortcullisMover mover = new PortcullisMover(plugin, portcullis, wallMaterials);
        portcullisMovers.add(mover);
        mover.hoist();
    }

    private void dropPortcullis(final Portcullis portcullis) {
        // Check whether the portcullis is already known
        for (PortcullisMover mover: portcullisMovers) {
            if (mover.getPortcullis().equals(portcullis)) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("[PorteCoulissante] Reusing existing portcullis mover");
                }
                // Set the portcullis, because the one cached by the portcullis
                // mover may be made from a different material
                mover.setPortcullis(portcullis);
                mover.drop();
                return;
            }
        }
        // It isn't
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("[PorteCoulissante] Creating new portcullis mover");
        }
        PortcullisMover mover = new PortcullisMover(plugin, portcullis, wallMaterials);
        portcullisMovers.add(mover);
        mover.drop();
    }

    private final PortcullisPlugin plugin;
    private final Set<PortcullisMover> portcullisMovers = new HashSet<PortcullisMover>();
    private final Set<Integer> wallMaterials = new HashSet<Integer>(Arrays.asList(
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
    
    private static final BlockFace[] CARDINAL_DIRECTIONS = {NORTH, EAST, SOUTH, WEST};
    private static final Set<Integer> CONDUCTIVE = new HashSet<Integer>(Arrays.asList(
        BLK_REDSTONE_WIRE, BLK_REDSTONE_TORCH_ON, BLK_REDSTONE_TORCH_OFF, BLK_REDSTONE_REPEATER_ON,
        BLK_REDSTONE_REPEATER_OFF, BLK_STONE_BUTTON, BLK_LEVER, BLK_STONE_PRESSURE_PLATE,
        BLK_WOODEN_PRESSURE_PLATE, BLK_TRIPWIRE_HOOK, BLK_WOODEN_BUTTON, BLK_TRAPPED_CHEST,
        BLK_REDSTONE_COMPARATOR_OFF, BLK_REDSTONE_COMPARATOR_ON, BLK_WEIGHTED_PRESSURE_PLATE_LIGHT,
        BLK_WEIGHTED_PRESSURE_PLATE_HEAVY));
    private static final Logger logger = PortcullisPlugin.logger;
}