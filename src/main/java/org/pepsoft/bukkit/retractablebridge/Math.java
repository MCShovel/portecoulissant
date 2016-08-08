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

/**
 *
 * @author pepijn
 */
public final class Math {
    private Math() {
        // Prevent instantiation
    }
    
    public static int pow(int x, int y) {
        switch (y) {
            case 0:
                return 1;
            case 1:
                return x;
            case 2:
                return x * x;
            case 3:
                return x * x * x;
            case 4:
                return x * x * x * x;
            case 5:
                return x * x * x * x * x;
            case 6:
                return x * x * x * x * x * x;
            case 7:
                return x * x * x * x * x * x * x;
            case 8:
                return x * x * x * x * x * x * x * x;
            case 9:
                return x * x * x * x * x * x * x * x * x;
            default:
                if (y < 0) {
                    throw new IllegalArgumentException("negative powers not supported");
                } else {
                    int n = x;
                    for (int i = 1; i < y; i++) {
                        n *= x;
                    }
                    return n;
                }
        }
    }
}
