package com.github.shynixn.blockball.bukkit.dependencies.worldguard;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Copyright 2017 Shynixn
 * <p>
 * Do not remove this header!
 * <p>
 * Version 1.0
 * <p>
 * MIT License
 * <p>
 * Copyright (c) 2017
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
public final class WorldGuardConnection6 {
    private static final ArrayList<ProtectedRegion> flags = new ArrayList<>();

    /**
     * Initialize
     */
    private WorldGuardConnection6() {
        super();
    }

    /**
     * Sets entities spawning allowed at the given location
     *
     * @param location location
     */
    public synchronized static void setSpawningAllowedAt(Location location) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        final RegionManager regionManager = getWorldGuardPlugin().getRegionManager(location.getWorld());
        final ApplicableRegionSet set = regionManager.getApplicableRegions(location);
        @SuppressWarnings("unchecked") final Iterable<ProtectedRegion> regions = (Iterable<ProtectedRegion>) getMethod(set.getClass()).invoke(set);
        for (final ProtectedRegion region : regions) {
            if (region.getFlag(DefaultFlag.MOB_SPAWNING) == State.DENY) {
                region.setFlag(DefaultFlag.MOB_SPAWNING, State.ALLOW);
                flags.add(region);
            }
        }
    }

    /**
     * Rolls all spawning allowed flags back
     */
    public synchronized static void rollBack() {
        for (final ProtectedRegion region : flags.toArray(new ProtectedRegion[flags.size()])) {
            region.setFlag(DefaultFlag.MOB_SPAWNING, State.DENY);
        }
        flags.clear();
    }

    /**
     * Returns the custom method
     *
     * @param class1 class1
     * @return method
     */
    private static Method getMethod(Class<?> class1) {
        for (final Method method : class1.getDeclaredMethods()) {
            if (method.getName().equalsIgnoreCase("getRegions"))
                return method;
        }
        throw new RuntimeException("Cannot hook into WorldGuard!");
    }

    /**
     * Returns the worldGuard plugin
     *
     * @return plugin
     */
    private static WorldGuardPlugin getWorldGuardPlugin() {
        return (WorldGuardPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
    }
}
