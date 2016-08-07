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

import java.awt.Point;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.getspout.spoutapi.SpoutManager;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.bukkit.portcullis.PortcullisMover.Status.*;
import static org.pepsoft.bukkit.portcullis.Directions.*;

/**
 *
 * @author pepijn
 */
public class PortcullisMover implements Runnable {
    public PortcullisMover(PortcullisPlugin plugin, Portcullis portcullis, Set<Integer> wallMaterials) {
        this.plugin = plugin;
        this.portcullis = portcullis;
        this.wallMaterials = wallMaterials;
    }

    public Portcullis getPortcullis() {
        return portcullis;
    }

    void setPortcullis(Portcullis portcullis) {
        if (! portcullis.equals(this.portcullis)) {
            throw new IllegalArgumentException();
        }
        this.portcullis = portcullis;
    }

    public void hoist() {
        if (status == HOISTING) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("[PorteCoulissante] Portcullis already hoisting; ignoring request");
            }
            return;
        } else if (logger.isLoggable(Level.FINE)) {
            logger.fine("[PorteCoulissante] Hoisting portcullis");
        }
        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        if (status == DROPPING) {
            scheduler.cancelTask(taskId);
        }
        int hoistingDelay = plugin.getHoistingDelay();
        taskId = scheduler.scheduleSyncRepeatingTask(plugin, this, hoistingDelay / 2, hoistingDelay);

        if (plugin.isSoundEffects() && (status != HOISTING)) {
            World world = plugin.getServer().getWorld(portcullis.getWorldName());
            if (world != null) {
                SpoutManager.getSoundManager().playGlobalCustomSoundEffect(plugin, plugin.getStartSoundURL(), false, new Location(world, portcullis.getX() + portcullis.getDirection().getModX() * portcullis.getWidth() / 2, portcullis.getY(), portcullis.getZ() + portcullis.getDirection().getModZ() * portcullis.getWidth() / 2), plugin.getSoundEffectDistance(), plugin.getSoundEffectVolume());
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("[PorteCoulissante] Playing start sound effect");
                }
            }
        }
        
        status = HOISTING;
    }

    public void drop() {
        if (status == DROPPING) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("[PorteCoulissante] Portcullis already dropping; ignoring request");
            }
            return;
        } else if (logger.isLoggable(Level.FINE)) {
            logger.fine("[PorteCoulissante] Dropping portcullis");
        }
        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        if (status == HOISTING) {
            scheduler.cancelTask(taskId);
        }
        int droppingDelay = plugin.getDroppingDelay();
        taskId = scheduler.scheduleSyncRepeatingTask(plugin, this, droppingDelay, droppingDelay);

        if (plugin.isSoundEffects() && (status != DROPPING)) {
            World world = plugin.getServer().getWorld(portcullis.getWorldName());
            if (world != null) {
                SpoutManager.getSoundManager().playGlobalCustomSoundEffect(plugin, plugin.getStartSoundURL(), false, new Location(world, portcullis.getX() + portcullis.getDirection().getModX() * portcullis.getWidth() / 2, portcullis.getY(), portcullis.getZ() + portcullis.getDirection().getModZ() * portcullis.getWidth() / 2), plugin.getSoundEffectDistance(), plugin.getSoundEffectVolume());
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("[PorteCoulissante] Playing start sound effect");
                }
            }
        }
        downSoundPlayedInPreviousMove = false;
        
        status = DROPPING;
    }

    @Override
    public void run() {
        try {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "[PorteCoulissante] PortCullisMover.run() (thread: " + Thread.currentThread() + ")", new Throwable());
            }
            if ((status == HOISTING) && (! movePortcullisUp(portcullis))) {
                plugin.getServer().getScheduler().cancelTask(taskId);
                taskId = 0;
                status = IDLE;
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("[PorteCoulissante] Portcullis hoisted!");
                }
            } else if ((status == DROPPING) && (! movePortcullisDown(portcullis))) {
                plugin.getServer().getScheduler().cancelTask(taskId);
                taskId = 0;
                status = IDLE;
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("[PorteCoulissante] Portcullis dropped!");
                }
            }
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "[PorteCoulissante] Exception thrown while moving portcullis!", t);
        }
    }

    private boolean movePortcullisUp(Portcullis portcullis) {
        World world = plugin.getServer().getWorld(portcullis.getWorldName());
        if (world == null) {
            // The world is gone!!!
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("[PorteCoulissante] World not loaded; cancelling the hoist!");
            }
            return false;
        }
        int x = portcullis.getX();
        int z = portcullis.getZ();
        int y = portcullis.getY();
        int width = portcullis.getWidth();
        int height = portcullis.getHeight();
        BlockFace direction = portcullis.getDirection();

        // Check whether the relevant chunks (the portcullis might straddle two
        // chunks) are loaded. In theory someone might build a huge portcullis
        // which straddles multiple chunks, but they're on their own... ;-)
        Set<Point> chunkCoords = getChunkCoords(x, z, direction, width);
        if (! areChunksLoaded(world, chunkCoords)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("[PorteCoulissante] Some or all chunks not loaded; cancelling the hoist!");
            }
            return false;
        }
        
        // Check whether the portcullis is still intact
        if (! isPortcullisWhole(world)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("[PorteCoulissante] Portcullis no longer intact; cancelling the hoist!");
            }
            return false;
        }

        // Check whether there is room above the portcullis
        if (y + height >= world.getMaxHeight()) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("[PorteCoulissante] World ceiling reached; no more room; destroying portcullis!");
            }
            explodePortcullis(world);
            return false;
        }
        
        BlockFace actualDirection = actual(direction);
        int dx = actualDirection.getModX(), dz = actualDirection.getModZ();
        if (! plugin.isAllowFloating()) {
            // Check that the portcullis would not be floating, if that is not
            // allowed
            boolean solidBlockFound = false;
            for (int yy = y + 1; yy <= y + height; yy++) {
                int blockID = world.getBlockTypeIdAt(x - dx, yy, z - dz);
                if (wallMaterials.contains(blockID) || SUPPORTING_MATERIALS.contains(blockID)) {
                    solidBlockFound = true;
                    break;
                }
                blockID = world.getBlockTypeIdAt(x + width * dx, yy, z + width * dz);
                if (wallMaterials.contains(blockID) || SUPPORTING_MATERIALS.contains(blockID)) {
                    solidBlockFound = true;
                    break;
                }
            }
            if (! solidBlockFound) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("[PorteCoulissante] Portcullis would be floating, which is not allowed; cancelling the hoist!");
                }
                return false;
            }
        }
        
        for (int i = 0; i < width; i++) {
            Block block = world.getBlockAt(x + i * dx, y + height, z + i * dz);
            if (! AIR_MATERIALS.contains(block.getTypeId())) {
                // No room to move up, we're done here
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("[PorteCoulissante] Not enough room above portcullis (block of type " + block.getType() + " found @ " + (x + i * dx) + ", " + (y + height) + ", " + (z + i * dz) + ")");
                }
                return false;
            }
        }

        // There is room. Move the portcullis up one block
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("[PorteCoulissante] Moving portcullis up one row.");
        }
        int portcullisType = portcullis.getType();
        byte portcullisData = portcullis.getData();
        for (int i = 0; i < width; i++) {
            // Set the block above the portcullis to "fence"
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("[PorteCoulissante] Setting block @ " + (x + i * dx) + ", " + (y + height) + ", " + (z + i * dz) + " to type " + portcullisType + ", data " + portcullisData + ".");
            }
            Block block = world.getBlockAt(x + i * dx, y + height, z + i * dz);
            block.setTypeIdAndData(portcullisType, portcullisData, true);
            // Set the block below to "air"
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("[PorteCoulissante] Setting block @ " + (x + i * dx) + ", " + y + ", " + (z + i * dz) + " to \"air\".");
            }
            block = world.getBlockAt(x + i * dx, y, z + i * dz);
            block.setTypeIdAndData(BLK_AIR, (byte) 0, true);
        }

        // Move any entities and items on top of the portcullis up (but only if
        // enabled)
        if (plugin.isEntityMovingEnabled()) {
            moveEntitiesUp(world, chunkCoords, portcullis);
        }

        if (plugin.isSoundEffects()) {
            SpoutManager.getSoundManager().playGlobalCustomSoundEffect(plugin, plugin.getUpSoundURL(), false, new Location(world, x + dx * width / 2, y + 1, z + dz * width / 2), plugin.getSoundEffectDistance(), plugin.getSoundEffectVolume());
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("[PorteCoulissante] Playing up sound effect");
            }
        }
        
        portcullis.setY(y + 1);
        return true;
    }

    private boolean movePortcullisDown(Portcullis portcullis) {
        World world = plugin.getServer().getWorld(portcullis.getWorldName());
        if (world == null) {
            // The world is gone!!!
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("[PorteCoulissante] World not loaded; cancelling the move!");
            }
            return false;
        }
        int x = portcullis.getX();
        int z = portcullis.getZ();
        int y = portcullis.getY();
        int width = portcullis.getWidth();
        int height = portcullis.getHeight();
        BlockFace direction = portcullis.getDirection();

        // Check whether the relevant chunks (the portcullis might straddle two
        // chunks) are loaded. In theory someone might build a huge portcullis
        // which straddles multiple chunks, but they're on their own... ;-)
        if (! areChunksLoaded(world, getChunkCoords(x, z, direction, width))) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("[PorteCoulissante] Some or all chunks not loaded; cancelling the drop!");
            }
            return false;
        }

        // Check whether the portcullis is still intact
        if (! isPortcullisWhole(world)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("[PorteCoulissante] Portcullis no longer intact; cancelling the drop!");
            }
            return false;
        }
        
        // Check whether there is room below the portcullis
        if (y <= 0) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("[PorteCoulissante] World floor reached; no more room.");
            }
            return false;
        }
        BlockFace actualDirection = actual(direction);
        int dx = actualDirection.getModX(), dz = actualDirection.getModZ();
        for (int i = 0; i < width; i++) {
            Block block = world.getBlockAt(x + i * dx, y - 1, z + i * dz);
            if (! AIR_MATERIALS.contains(block.getTypeId())) {
                // No room to move down, we're done here
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("[PorteCoulissante] Not enough room below portcullis (block of type " + block.getType() + " found @ " + (x + i * dx) + ", " + (y - 1) + ", " + (z + i * dz) + ")");
                }
                return false;
            }
        }

        // There is room. Move the portcullis down one block
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("[PorteCoulissante] Moving portcullis down one row.");
        }
        int portcullisType = portcullis.getType();
        byte portcullisData = portcullis.getData();
        y--;
        for (int i = 0; i < width; i++) {
            // Set the block above the portcullis to "air"
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("[PorteCoulissante] Setting block @ " + (x + i * dx) + ", " + (y + height) + ", " + (z + i * dz) + " to \"air\".");
            }
            Block block = world.getBlockAt(x + i * dx, y + height, z + i * dz);
            block.setTypeIdAndData(BLK_AIR, (byte) 0, true);
            // Set the block below to "fence"
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("[PorteCoulissante] Setting block @ " + (x + i * dx) + ", " + y + ", " + (z + i * dz) + " to type " + portcullisType + ", data " + portcullisData + ".");
            }
            block = world.getBlockAt(x + i * dx, y, z + i * dz);
            block.setTypeIdAndData(portcullisType, portcullisData, true);
        }

        portcullis.setY(y);

        if (plugin.isSoundEffects()) {
            if (! downSoundPlayedInPreviousMove) {
                SpoutManager.getSoundManager().playGlobalCustomSoundEffect(plugin, plugin.getDownSoundURL(), false, new Location(world, x + dx * width / 2, y - 1, z + dz * width / 2), plugin.getSoundEffectDistance(), plugin.getSoundEffectVolume());
                downSoundPlayedInPreviousMove = true;
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("[PorteCoulissante] Playing down sound effect");
                }
            } else {
                downSoundPlayedInPreviousMove = false;
            }
        }

        return true;
    }

    private void moveEntitiesUp(World world, Set<Point> chunkCoords, Portcullis portcullis) {
        for (Point chunkCoord: chunkCoords) {
            Chunk chunk = world.getChunkAt(chunkCoord.x, chunkCoord.y);
            for (Entity entity: chunk.getEntities()) {
                Location location = entity.getLocation();
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("[PorteCoulissante] Considering entity " + entity + "@" + entity.getEntityId() + ": " + location.getX() + ", " + location.getY() + ", " + location.getZ());
                }
                if (isOnPortcullis(location, portcullis)) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("[PorteCoulissante] Entity is on portcullis; moving it up");
                    }
                    location.setY(location.getY() + 1);
                    entity.teleport(location);
                }
            }
        }
    }

    private boolean isOnPortcullis(Location location, Portcullis portcullis) {
        int x = portcullis.getX(), y = portcullis.getY(), z = portcullis.getZ(), width = portcullis.getWidth(), height = portcullis.getHeight();
        BlockFace actualDirection = actual(portcullis.getDirection());
        int x2 = x + actualDirection.getModX() * width;
        int z2 = z + actualDirection.getModZ() * width;
        if (x > x2) {
            int tmp = x;
            x = x2;
            x2 = tmp;
        }
        if (z > z2) {
            int tmp = z;
            z = z2;
            z2 = tmp;
        }
        int locX = location.getBlockX(), locY = location.getBlockY(), locZ = location.getBlockZ();
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("[PorteCoulissante] Portcullis coordinates: " + x + " -> " + x2 + ", " + z + " -> " + z2 + ", " + (y + height));
            logger.fine("[PorteCoulissante] Location: " + locX + ", " + locZ + ", " + locY);
        }
        return (locX >= x) && (locX <= x2) && (locZ >= z) && (locZ <= z2) && (locY == (y + height));
    }

    private boolean areChunksLoaded(World world, Set<Point> chunkCoords) {
        for (Point point: chunkCoords) {
            if (! world.isChunkLoaded(point.x, point.y)) {
                return false;
            }
        }
        return true;
    }

    private Set<Point> getChunkCoords(int x, int z, BlockFace direction, int width) {
        Set<Point> chunkCoords = new HashSet<Point>();
        int firstChunkX = x >> 4;
        int firstChunkZ = z >> 4;
        chunkCoords.add(new Point(firstChunkX, firstChunkZ));
        BlockFace actualDirection = actual(direction);
        int secondChunkX = (x + actualDirection.getModX() * (width - 1)) >> 4;
        int secondChunkZ = (z + actualDirection.getModZ() * (width - 1)) >> 4;
        if ((secondChunkX != firstChunkX) || (secondChunkZ != firstChunkZ)) {
            chunkCoords.add(new Point(secondChunkX, secondChunkZ));
        }
        return chunkCoords;
    }
    
    private boolean isPortcullisWhole(World world) {
        int portcullisX = portcullis.getX();
        int portcullisY1 = portcullis.getY(), portcullisY2 = portcullisY1 + portcullis.getHeight();
        int portcullisZ = portcullis.getZ();
        int portcullisWidth = portcullis.getWidth();
        BlockFace actualPortcullisDirection = actual(portcullis.getDirection());
        int dx = actualPortcullisDirection.getModX(), dz = actualPortcullisDirection.getModZ();
        int portcullisType = portcullis.getType();
        byte portcullisData = portcullis.getData();
        for (int y = portcullisY1; y < portcullisY2; y++) {
            int x = portcullisX;
            int z = portcullisZ;
            for (int i = 0; i < portcullisWidth; i++) {
                Block block = world.getBlockAt(x, y, z);
                if ((block.getTypeId() != portcullisType) || (block.getData() != portcullisData)) {
                    return false;
                }
                x += dx;
                z += dz;
            }
        }
        return true;
    }
    
    private void explodePortcullis(World world) {
        int portcullisX = portcullis.getX();
        int portcullisY1 = portcullis.getY(), portcullisY2 = portcullisY1 + portcullis.getHeight();
        int portcullisZ = portcullis.getZ();
        BlockFace actualPortcullisDirection = actual(portcullis.getDirection());
        int dx = actualPortcullisDirection.getModX(), dz = actualPortcullisDirection.getModZ();
        int portcullisType = portcullis.getType();
        byte portcullisData = portcullis.getData();
        ItemStack itemStack = new ItemStack(portcullisType, 1, (short) 0, portcullisData);
        for (int y = portcullisY1; y < portcullisY2; y++) {
            int x = portcullisX;
            int z = portcullisZ;
            for (int i = 0; i < portcullis.getWidth(); i++) {
                Block block = world.getBlockAt(x, y, z);
                block.setTypeIdAndData(BLK_AIR, (byte) 0, true);
                world.dropItemNaturally(block.getLocation(), itemStack);
                x += dx;
                z += dz;
            }
        }
    }

    private final PortcullisPlugin plugin;
    private final Set<Integer> wallMaterials;
    private Portcullis portcullis;
    private boolean downSoundPlayedInPreviousMove;
    private int taskId;
    private Status status = Status.IDLE;

    private static final Logger logger = PortcullisPlugin.logger;
    private static final Set<Integer> AIR_MATERIALS = new HashSet<Integer>(Arrays.asList(
        BLK_AIR, BLK_WATER, BLK_STATIONARY_WATER, BLK_LAVA, BLK_STATIONARY_LAVA,
        BLK_SUGAR_CANE, BLK_SNOW, BLK_DANDELION, BLK_ROSE, BLK_BROWN_MUSHROOM, BLK_RED_MUSHROOM,
        BLK_FIRE, BLK_WHEAT, BLK_TALL_GRASS, BLK_DEAD_SHRUBS, BLK_COBWEB, BLK_PUMPKIN_STEM,
        BLK_MELON_STEM, BLK_VINES, BLK_LILY_PAD, BLK_NETHER_WART, BLK_CARROTS, BLK_POTATOES,
        BLK_DOUBLE_PLANT));
    private static final Set<Integer> SUPPORTING_MATERIALS = new HashSet<Integer>(Arrays.asList(
        BLK_FENCE, BLK_FENCE_GATE, BLK_IRON_BARS, BLK_NETHER_BRICK_FENCE, BLK_WOODEN_SLAB,
        BLK_SLAB, BLK_WOODEN_STAIRS, BLK_COBBLESTONE_STAIRS, BLK_NETHER_BRICK_STAIRS,
        BLK_SANDSTONE_STAIRS, BLK_SPRUCE_WOOD_STAIRS, BLK_BIRCH_WOOD_STAIRS, BLK_JUNGLE_WOOD_STAIRS));
    
    static enum Status {IDLE, HOISTING, DROPPING}
}