/*
 * GNU Lesser General Public License v3
 * Copyright (C) 2024 Tschipp
 * mrtschipp@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package tschipp.carryon.compat;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import tschipp.carryon.Constants;
import tschipp.carryon.platform.Services;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ArchitecturyCompat {

    private static Object INVOKER_INSTANCE;
    private static Method PLACE_BLOCK;
    private static Method IS_FALSE;

    private static void setup( ) {
        try {
            Class BlockEvent = Class.forName("dev.architectury.event.events.common.BlockEvent");
            Field PLACE = BlockEvent.getField("PLACE");
            Method invoker = Class.forName("dev.architectury.event.Event").getMethod("invoker");
            INVOKER_INSTANCE = invoker.invoke(PLACE.get(BlockEvent));
            Class PlaceClass = Class.forName("dev.architectury.event.events.common.BlockEvent$Place");
            PLACE_BLOCK = PlaceClass.getMethod("placeBlock", Level.class, BlockPos.class, BlockState.class, Entity.class);
            Class EventResult = Class.forName("dev.architectury.event.EventResult");
            IS_FALSE = EventResult.getMethod("isFalse");

        } catch (Exception e) {
            Constants.LOG.warn("Error while initializing Architectury Compat: " + e);
        }
    }

    public static boolean active() {
        return Services.PLATFORM.isModLoaded("architectury");
    }

    public static boolean sendPlaceEvent(Level level, BlockPos pos, BlockState state, Player player) {
        if(!active())
            return true;

        if(INVOKER_INSTANCE == null || PLACE_BLOCK == null)
            setup();

        if(INVOKER_INSTANCE != null && PLACE_BLOCK != null && IS_FALSE != null) {
            try {
                Object eventResult = PLACE_BLOCK.invoke(INVOKER_INSTANCE, level, pos, state, player);
                boolean canceled = (boolean) IS_FALSE.invoke(eventResult);

                return !canceled;

            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return true;
    }

}
