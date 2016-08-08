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

import java.util.Set;
import org.bukkit.block.BlockFace;

/**
 *
 * @author pepijn
 */
public class Bridge {
    /**
     * Create a new Bridge
     *
     * @param worldName The name of the world this bridge is in.
     * @param x The X coordinate of its north east corner
     * @param z The Z coordinate of its north east corner
     * @param y The height of the bridge
     * @param width The width of the bridge (its size in the north-south direction)
     * @param height The height of the bridge (its size in the east-west direction)
     */
    public Bridge(String worldName, int x, int z, int y, int width, int height, Set<BlockFace> blockedDirections, int type, byte material) {
        this.worldName = worldName;
        this.x = x;
        this.z = z;
        this.width = width;
        this.height = height;
        this.y = y;
        this.blockedDirections = blockedDirections;
        this.type = type;
        this.material = material;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public Set<BlockFace> getBlockedDirections() {
        return blockedDirections;
    }

    public int getType() {
        return type;
    }

    public byte getMaterial() {
        return material;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setZ(int z) {
        this.z = z;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (! (obj instanceof Bridge)) {
            return false;
        }
        final Bridge other = (Bridge) obj;
        if (this.x != other.x) {
            return false;
        }
        if (this.z != other.z) {
            return false;
        }
        if (this.y != other.y) {
            return false;
        }
        if (this.width != other.width) {
            return false;
        }
        if (this.height != other.height) {
            return false;
        }
        if ((this.worldName == null) ? (other.worldName != null) : !this.worldName.equals(other.worldName)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + (this.worldName != null ? this.worldName.hashCode() : 0);
        hash = 29 * hash + this.x;
        hash = 29 * hash + this.z;
        hash = 29 * hash + this.y;
        hash = 29 * hash + this.width;
        hash = 29 * hash + this.height;
        return hash;
    }

    private final String worldName;
    private int x, z;
    private final int y, width, height, type;
    private final Set<BlockFace> blockedDirections;
    private final byte material;
}