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

import org.bukkit.Chunk;
import java.awt.Point;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitScheduler;
import org.pepsoft.bukkit.PortcullisPlugin;

import static org.pepsoft.minecraft.Constants.*;

/**
 *
 * @author pepijn
 */
public class BridgeMover implements Runnable {
    public BridgeMover(PortcullisPlugin plugin, Bridge bridge) {
        this.plugin = plugin;
        this.bridge = bridge;
    }

    public Bridge getBridge() {
        return bridge;
    }

    public void setBridge(Bridge bridge) {
        if (! bridge.equals(this.bridge)) {
            throw new IllegalArgumentException();
        }
        this.bridge = bridge;
    }

    public void move(BlockFace direction) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("[RetractableBridge] Moving bridge " + direction);
        }
        if (direction != this.direction) {
            this.direction = direction;
            movingDelay = plugin.getMovingDelayBase();
            boosts = 0;
        } else if (boosts < plugin.getMaximumBoosts()) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("[RetractableBridge] Bridge already moving " + direction + "; boosting speed");
            }
            // If we were already moving in that direction, boost the speed
            movingDelay = movingDelay / 2;
            if (movingDelay < 1) {
                movingDelay = 1;
            }
            boosts++;
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("[RetractableBridge] Bridge already moving " + direction + " and maximum boosts reached; doing nothing");
            }
        }
        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        if (taskId != 0) {
            scheduler.cancelTask(taskId);
        }
        taskId = scheduler.scheduleSyncRepeatingTask(plugin, this, java.lang.Math.max(movingDelay / 2, 1), movingDelay);
    }

    @Override
    public void run() {
        try {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "[RetractableBridge] BridgeMover.run() (thread: " + Thread.currentThread() + ")", new Throwable());
            }
            if (! moveBridge()) {
                plugin.getServer().getScheduler().cancelTask(taskId);
                taskId = 0;
                direction = null;
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("[RetractableBridge] Bridge moved!");
                }
            }
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "[RetractableBridge] Exception thrown while moving bridge!", t);
        }
    }

    private boolean moveBridge() {
        World world = plugin.getServer().getWorld(bridge.getWorldName());
        if (world == null) {
            // The world is gone!!!
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("[RetractableBridge] World not loaded; cancelling the move!");
            }
            return false;
        }
        int x = bridge.getX();
        int z = bridge.getZ();
        int y = bridge.getY();
        int width = bridge.getWidth();
        int height = bridge.getHeight();
        Set<Point> chunkCoords = getChunkCoords(x, z, width, height);

        // Check whether the relevant chunks are loaded
        if (! areChunksLoaded(world, chunkCoords)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("[RetractableBridge] Some or all chunks not loaded; cancelling the move!");
            }
            return false;
        }
        
        // Check whether the bridge is still intact
        if (! isBridgeWhole(world)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("[RetractableBridge] Bridge no longer intact; cancelling the move!");
            }
            return false;
        }

        // Check whether there is room in the direction of the move
        int length, dx = 0, dz = 0, xMult = 0, zMult = 0;
        switch (direction) {
            case WEST:
                length = height;
                dx = -1;
                zMult = 1;
                break;
            case NORTH:
                length = width;
                dz = -1;
                xMult = 1;
                break;
            case EAST:
                length = height;
                dx = width;
                zMult = 1;
                break;
            case SOUTH:
                length = width;
                dz = height;
                xMult = 1;
                break;
            default:
                throw new AssertionError(direction);
        }
        for (int i = 0; i < length; i++) {
            int blockTypeId = world.getBlockTypeIdAt(x + dx + i * xMult, y, z + dz + i * zMult);
            if (! BridgeBlockListener.AIR_MATERIALS.contains(blockTypeId)) {
                // Blocked
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("[RetractableBridge] Not enough room besides bridge (block of type " + Material.getMaterial(blockTypeId) + " found @ " + (x + dx + i * xMult) + ", " + y + ", " + (z + dz + i * zMult) + ")");
                }
                return false;
            }
        }
        
        // Check whether the bridge would be floating, and if it is, whether
        // that is allowed
        boolean floatingOnAir = true, floatingOnWater = false;
        int ddx = (int) java.lang.Math.signum(dx), ddz = (int) java.lang.Math.signum(dz);
outer:  for (int xx = x + ddx; xx < (x + ddx + width); xx++) {
            for (int zz = z + ddz; zz < (z + ddz + height); zz++) {
                int blockID = world.getBlockTypeIdAt(xx, y - 1, zz);
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("[RetractableBridge] Block beneath bridge @ " + xx + "," + (y - 1) + "," + zz + ": " + Material.getMaterial(blockID));
                }
                if (BridgeBlockListener.WALL_MATERIALS.contains(blockID) || SUPPORTING_MATERIALS.contains(blockID)) {
                    floatingOnAir = false;
                    floatingOnWater = false;
                    break outer;
                } else if (FLUIDS.contains(blockID)) {
                    floatingOnAir = false;
                    floatingOnWater = true;
                }
            }
        }
        if (floatingOnAir && (! plugin.isAllowFloating())) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("[RetractableBridge] No solid block beneath bridge and floating not allowed; cancelling the move!");
            }
            return false;
        }

        // There is room. Move the bridge one block
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("[RetractableBridge] Moving bridge " + direction + " one row.");
        }
        int type = bridge.getType();
        byte data = bridge.getMaterial();
        for (int i = 0; i < length; i++) {
            // Set the block in front of the bridge to the bridge material
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("[RetractableBridge] Setting block @ " + (x + dx + i * xMult) + ", " + y + ", " + (z + dz + i * zMult) + " to bridge material.");
            }
            Block block = world.getBlockAt(x + dx + i * xMult, y, z + dz + i * zMult);
            block.setTypeIdAndData(type, data, true);
        }

        // Move any entities standing or lying on the bridge. Do it after
        // extending the leading edge but before erasing the trailing edge of
        // the bridge, so that nobody falls off
        // But only if enabled
        if (plugin.isEntityMovingEnabled()) {
            moveEntities(world, chunkCoords);
        }

        if (dx == -1) {
            dx = width - 1;
        } else if (dx == width) {
            dx = 0;
        } else if (dz == -1) {
            dz = height - 1;
        } else {
            dz = 0;
        }
        for (int i = 0; i < length; i++) {
            // Set the block behind to "air"
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("[RetractableBridge] Setting block @ " + (x + dx + i * xMult) + ", " + y + ", " + (z + dz + i * zMult) + " to \"air\".");
            }
            Block block = world.getBlockAt(x + dx + i * xMult, y, z + dz + i * zMult);
            block.setTypeIdAndData(BLK_AIR, (byte) 0, true);
        }
        // This is necessary because Bukkit changed the coordinate system used
        // by the BlockFace class in version 1.4.5:
        BlockFace actualDirection = Directions.actual(direction);
        bridge.setX(x + actualDirection.getModX());
        bridge.setZ(z + actualDirection.getModZ());

        return true;
    }

    private void moveEntities(World world, Set<Point> chunkCoords) {
        int x = bridge.getX(), y = bridge.getY(), z = bridge.getZ(), width = bridge.getWidth(), height = bridge.getHeight(), type = bridge.getType();
        byte material = bridge.getMaterial();
        for (Point chunkCoord: chunkCoords) {
            Chunk chunk = world.getChunkAt(chunkCoord.x, chunkCoord.y);
            for (Entity entity: chunk.getEntities()) {
                Location location = entity.getLocation();
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("[RetractableBridge] Considering entity " + entity + "@" + entity.getEntityId() + ": " + location.getX() + ", " + location.getY() + ", " + location.getZ());
                }
                if (isOnBridge(location, x, y, z, width, height, type, material)) {
                    if (isSpaceToMove(world, type, material, entity, location, direction)) {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("[RetractableBridge] Entity is on bridge; moving it " + direction);
                        }
                        // This is necessary because Bukkit changed the coordinate system used
                        // by the BlockFace class in version 1.4.5:
                        BlockFace actualDirection = Directions.actual(direction);
                        location.setX(location.getX() + actualDirection.getModX());
                        location.setZ(location.getZ() + actualDirection.getModZ());
                        entity.teleport(location);
                    } else {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("[RetractableBridge] Entity is on bridge, but there is no space to move it");
                        }
                    }
                }
            }
        }
    }

    private boolean isOnBridge(Location location, int x, int y, int z, int width, int height, int type, byte material) {
        // Correct for bottom half slab
        float dy = (((type == BLK_SLAB) || (type == BLK_WOODEN_SLAB)) && ((material & 0x8) == 0)) ? 0.5f : 1;
        double locX = location.getX(), locY = location.getY(), locZ = location.getZ();
        return (locX >= x) && (locX < (x + width)) && (locZ >= z) && (locZ < (z + height)) && (locY >= (y + dy)) && (locY < (y + dy + 0.25));
    }

    private boolean isSpaceToMove(World world, int type, byte material, Entity entity, Location location, BlockFace direction) {
        // This is necessary because Bukkit changed the coordinate system used
        // by the BlockFace class in version 1.4.5:
        BlockFace actualDirection = Directions.actual(direction);
        int newBlockX = location.getBlockX() + actualDirection.getModX();
        int newBlockY = location.getBlockY() + actualDirection.getModY();
        // Correct for bottom half slab
        if (((type == BLK_SLAB) || (type == BLK_WOODEN_SLAB)) && ((material & 0x8) == 0)) {
            newBlockY++;
        }
        int newBlockZ = location.getBlockZ() + actualDirection.getModZ();
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("[RetractableBridge] Block at entity's new location: " + world.getBlockAt(newBlockX, newBlockY, newBlockZ).getType());
        }
        if (! BridgeBlockListener.AIR_MATERIALS.contains(world.getBlockTypeIdAt(newBlockX, newBlockY, newBlockZ))) {
            return false;
        }
        if ((entity instanceof LivingEntity) && (((LivingEntity) entity).getEyeHeight(true) > 1.0)) {
            // For double high entities (such as players), also check the block above
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("[RetractableBridge] Block above entity's new location: " + world.getBlockAt(newBlockX, newBlockY + 1, newBlockZ).getType());
            }
            if (! BridgeBlockListener.AIR_MATERIALS.contains(world.getBlockTypeIdAt(newBlockX, newBlockY + 1, newBlockZ))) {
                return false;
            }
        }
        return true;
    }

    private boolean areChunksLoaded(World world, Set<Point> chunkCoords) {
        for (Point point: chunkCoords) {
            if (! world.isChunkLoaded(point.x, point.y)) {
                return false;
            }
        }
        return true;
    }

    private Set<Point> getChunkCoords(int x, int z, int width, int height) {
        // Also consider row in front of leading edge
        switch (direction) {
            case WEST:
                x--;
                width++;
                break;
            case NORTH:
                z--;
                height++;
                break;
            case EAST:
                width++;
                break;
            case SOUTH:
                height++;
                break;
        }
        Set<Point> chunkCoords = new HashSet<Point>();
        for (int u = x; u <= (x + width - 1); u += 16) {
            for (int v = z; v <= (z + height - 1); v += 16) {
                chunkCoords.add(new Point(u >> 4, v >> 4));
            }
            chunkCoords.add(new Point(u >> 4, (z + height - 1) >> 4));
        }
        chunkCoords.add(new Point((x + width - 1) >> 4, (z + height - 1) >> 4));
        return chunkCoords;
    }
    
    private boolean isBridgeWhole(World world) {
        int bridgeX1 = bridge.getX(), bridgeX2 = bridgeX1 + bridge.getWidth();
        int bridgeZ1 = bridge.getZ(), bridgeZ2 = bridgeZ1 + bridge.getHeight();
        int bridgeY = bridge.getY();
        int bridgeType = bridge.getType();
        byte bridgeData = bridge.getMaterial();
        for (int x = bridgeX1; x < bridgeX2; x++) {
            for (int z = bridgeZ1; z < bridgeZ2; z++) {
                Block block = world.getBlockAt(x, bridgeY, z);
                if ((block.getTypeId() != bridgeType)
                        || (block.getData() != bridgeData)) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Bridge no longer intact (block of type " + block.getType() + " found @" + x + "," + bridgeY + "," + z);
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private final PortcullisPlugin plugin;
    private Bridge bridge;
    private BlockFace direction;
    private int taskId;
    private int movingDelay;
    private int boosts;

    private static final Set<Integer> FLUIDS = new HashSet<Integer>(Arrays.asList(BLK_WATER, BLK_STATIONARY_WATER, BLK_LAVA, BLK_STATIONARY_LAVA));
    private static final Set<Integer> SUPPORTING_MATERIALS = new HashSet<Integer>(Arrays.asList(BLK_FENCE, BLK_FENCE_GATE, BLK_IRON_BARS, BLK_NETHER_BRICK_FENCE, BLK_WOODEN_SLAB, BLK_SLAB, BLK_WOODEN_STAIRS, BLK_COBBLESTONE_STAIRS, BLK_NETHER_BRICK_STAIRS, BLK_SANDSTONE_STAIRS, BLK_SPRUCE_WOOD_STAIRS, BLK_BIRCH_WOOD_STAIRS, BLK_JUNGLE_WOOD_STAIRS));
    private static final Logger logger = PortcullisPlugin.logger;
}
