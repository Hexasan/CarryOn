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

package tschipp.carryon.platform;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.PlayPayloadHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamMemberEncoder;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import tschipp.carryon.CarryOnFabricClientMod;
import tschipp.carryon.config.BuiltConfig;
import tschipp.carryon.config.fabric.ConfigLoaderImpl;
import tschipp.carryon.networking.PacketBase;
import tschipp.carryon.platform.services.IPlatformHelper;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public void registerConfig(BuiltConfig cfg) {
        ConfigLoaderImpl.registerConfig(cfg);
    }

    @Override
    public <T extends PacketBase, B extends FriendlyByteBuf> void  registerServerboundPacket(CustomPacketPayload.Type<T> type, Class<T> clazz, StreamCodec<B, T> codec, BiConsumer<T, Player> handler, Object... args)
    {
        PayloadTypeRegistry.playC2S().register(type, (StreamCodec<RegistryFriendlyByteBuf, T>)codec);

        ServerPlayNetworking.registerGlobalReceiver(type, (T packet, ServerPlayNetworking.Context context) -> {
            context.server().execute(() -> {
                handler.accept(packet, context.player());
            });
        });
    }

    @Override
    public <T extends PacketBase, B extends FriendlyByteBuf> void  registerClientboundPacket(CustomPacketPayload.Type<T> type, Class<T> clazz, StreamCodec<B, T> codec, BiConsumer<T, Player> handler, Object... args)
    {
        PayloadTypeRegistry.playS2C().register(type, (StreamCodec<RegistryFriendlyByteBuf, T>)codec);

        CarryOnFabricClientMod.registerClientboundPacket(type, handler);
    }

    @Override
    public void sendPacketToServer(ResourceLocation id, PacketBase packet)
    {
        CarryOnFabricClientMod.sendPacketToServer(packet);
    }

    @Override
    public void sendPacketToPlayer(ResourceLocation id, PacketBase packet, ServerPlayer player)
    {
        ServerPlayNetworking.send(player, packet);
    }
}
