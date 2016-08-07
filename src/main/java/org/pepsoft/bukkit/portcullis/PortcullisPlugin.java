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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.getspout.spoutapi.SpoutManager;
import static org.pepsoft.minecraft.Constants.*;

/**
 *
 * @author pepijn
 */
public class PortcullisPlugin extends JavaPlugin {
    @Override
    public void onDisable() {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("[PorteCoulissante] Plugin disabled");
        }
    }

    @Override
    public void onEnable() {
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "[PorteCoulissante] PortcullisPlugin.onEnable() (thread: " + Thread.currentThread() + ")", new Throwable());
        }
        
        File configFile = new File(getDataFolder(), "config.yml");
        if (! configFile.exists()) {
            getDataFolder().mkdirs();
            try {
                InputStream in = PortcullisPlugin.class.getResourceAsStream("/default.yml");
                try {
                    FileOutputStream out = new FileOutputStream(configFile);
                    try {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    } finally {
                        out.close();
                    }
                } finally {
                    in.close();
                }
            } catch (IOException e) {
                throw new RuntimeException("I/O error creating default config.yml file", e);
            }
        }
        
        FileConfiguration config = getConfig();
        entityMovingEnabled = config.getBoolean("entityMoving");
        hoistingDelay = config.getInt("hoistingDelay");
        droppingDelay = config.getInt("droppingDelay");
        portcullisMaterials = Collections.unmodifiableSet(new HashSet<Integer>(config.getIntegerList("portcullisMaterials")));
        allowFloating = config.getBoolean("allowFloating");
        powerBlocks = Collections.unmodifiableSet(new HashSet<Integer>(config.getIntegerList("powerBlocks")));
        additionalWallMaterials = Collections.unmodifiableSet(new HashSet<Integer>(config.getIntegerList("additionalWallMaterials")));
        startSoundURL = config.getString("startSoundURL");
        upSoundURL = config.getString("upSoundURL");
        downSoundURL = config.getString("downSoundURL");
        soundEffectDistance = config.getInt("soundEffectDistance");
        soundEffectVolume = config.getInt("soundEffectVolume");
        allPowerBlocksAllowed = powerBlocks.isEmpty();
        boolean defaultSpeeds = true;
        List<String> warnings = new ArrayList<String>();
        if (hoistingDelay != DEFAULT_HOISTING_DELAY) {
            warnings.add("hoisting speed " + hoistingDelay);
            defaultSpeeds = false;
        }
        if (droppingDelay != DEFAULT_DROPPING_DELAY) {
            warnings.add("dropping speed " + droppingDelay);
            defaultSpeeds = false;
        }
        soundEffects = config.getBoolean("soundEffects", defaultSpeeds);
        if (! portcullisMaterials.equals(DEFAULT_PORTCULLIS_MATERIALS)) {
            warnings.add("portcullis materials " + portcullisMaterials);
        }
        if (allowFloating != DEFAULT_ALLOW_FLOATING) {
            warnings.add("floating not allowed");
        }
        if (! allPowerBlocksAllowed) {
            warnings.add("power blocks allowed " + powerBlocks);
        }
        if (! additionalWallMaterials.isEmpty()) {
            warnings.add("additional wall materials " + additionalWallMaterials);
        }
        if (! warnings.isEmpty()) {
            StringBuilder sb = new StringBuilder("[PorteCoulissante] Non-standard configuration items loaded from config file: ");
            boolean first = true;
            for (String warning: warnings) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(warning);
            }
            logger.info(sb.toString());
        }
        
        String debugLoggingPropertyValue = config.getString("debugLogging");
        if (debugLoggingPropertyValue != null) {
            if (debugLoggingPropertyValue.equalsIgnoreCase("extra")) {
                logger.setLevel(Level.FINEST);
                logger.info("[PorteCoulissante] Extra debug logging enabled (see log file)");
            } else if (! debugLoggingPropertyValue.equalsIgnoreCase("false")) {
                logger.setLevel(Level.FINE);
                logger.info("[PorteCoulissante] Debug logging enabled (see log file)");
            }
        }
        
        if (soundEffects) {
            // Check the validity of the URLs
            boolean urlsValid = true;
            try {
                new URL(upSoundURL);
            } catch (MalformedURLException e) {
                logger.severe("[PorteCoulissante] upSoundURL parameter is missing or invalid; disabling sound effects");
                urlsValid = false;
            }
            try {
                new URL(startSoundURL);
            } catch (MalformedURLException e) {
                logger.severe("[PorteCoulissante] startSoundURL parameter is missing or invalid; disabling sound effects");
                urlsValid = false;
            }
            try {
                new URL(downSoundURL);
            } catch (MalformedURLException e) {
                logger.severe("[PorteCoulissante] downSoundURL parameter is missing or invalid; disabling sound effects");
                urlsValid = false;
            }
            if (urlsValid) {
                try {
                    // Check whether the SpoutPlugin is available
                    SpoutManager.getSoundManager();
                    logger.info("[PorteCoulissante] Sound effects enabled");
                } catch (NoClassDefFoundError e) {
                    logger.info("[PorteCoulissante] SpoutPlugin not available; disabling sound effects");
                    soundEffects = false;
                }
            } else {
                soundEffects = false;
            }
        } else {
            logger.info("[PorteCoulissante] Sound effects disabled");
        }

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new PortcullisBlockListener(this), this);
        logger.info("[PorteCoulissante] Plugin version " + getDescription().getVersion() + " by Captain_Chaos enabled");
    }

    public boolean isEntityMovingEnabled() {
        return entityMovingEnabled;
    }

    public int getDroppingDelay() {
        return droppingDelay;
    }

    public int getHoistingDelay() {
        return hoistingDelay;
    }

    public Set<Integer> getPortcullisMaterials() {
        return portcullisMaterials;
    }

    public Set<Integer> getPowerBlocks() {
        return powerBlocks;
    }

    public boolean isAllowFloating() {
        return allowFloating;
    }

    public boolean isAllPowerBlocksAllowed() {
        return allPowerBlocksAllowed;
    }

    public boolean isSoundEffects() {
        return soundEffects;
    }

    public String getStartSoundURL() {
        return startSoundURL;
    }

    public String getUpSoundURL() {
        return upSoundURL;
    }

    public String getDownSoundURL() {
        return downSoundURL;
    }

    public int getSoundEffectDistance() {
        return soundEffectDistance;
    }

    public int getSoundEffectVolume() {
        return soundEffectVolume;
    }

    public Set<Integer> getAdditionalWallMaterials() {
        return additionalWallMaterials;
    }

    private boolean entityMovingEnabled, soundEffects;
    private int hoistingDelay, droppingDelay, soundEffectDistance, soundEffectVolume;
    private Set<Integer> portcullisMaterials, powerBlocks, additionalWallMaterials;
    private boolean allowFloating, allPowerBlocksAllowed;
    private String startSoundURL, upSoundURL, downSoundURL;

    static final Logger logger = Logger.getLogger("Minecraft.org.pepsoft.bukkit.portcullis");
    
    private static final int DEFAULT_HOISTING_DELAY = 40, DEFAULT_DROPPING_DELAY = 10;
    private static final Set<Integer> DEFAULT_PORTCULLIS_MATERIALS = new HashSet<Integer>(Arrays.asList(BLK_FENCE, BLK_IRON_BARS, BLK_NETHER_BRICK_FENCE));
    private static final boolean DEFAULT_ALLOW_FLOATING = true;
    private static final int BUFFER_SIZE = 32768;
}