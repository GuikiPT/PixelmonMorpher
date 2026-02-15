package com.guikipt.pixelmonmorpher.network;

import com.guikipt.pixelmonmorpher.PixelmonMorpher;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Objects;

/**
 * Registers payloads and helper senders for morph sync.
 */
@EventBusSubscriber(modid = PixelmonMorpher.MODID)
public class NetworkHandler {
    public static final ResourceLocation MORPH_SYNC_ID = ResourceLocation.fromNamespaceAndPath(PixelmonMorpher.MODID, "morph_sync");

    public static final StreamCodec<FriendlyByteBuf, MorphDataSyncPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, packet) -> MorphDataSyncPacket.encode(packet, buf),
            MorphDataSyncPacket::decode
        );

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PixelmonMorpher.MODID);
        registrar.playToClient(
            Objects.requireNonNull(MorphDataSyncPacket.TYPE),
            Objects.requireNonNull(STREAM_CODEC),
            MorphDataSyncPacket::handle
        );
        registrar.playToServer(
            Objects.requireNonNull(MorphRequestPacket.TYPE),
            Objects.requireNonNull(MorphRequestPacket.CODEC),
            MorphRequestPacket::handle
        );
    }

    public static void sendToServer(CustomPacketPayload message) {
        PacketDistributor.sendToServer(Objects.requireNonNull(message));
    }

    public static void sendToAll(MorphDataSyncPacket message) {
        PacketDistributor.sendToAllPlayers(Objects.requireNonNull(message));
    }

    public static void sendToPlayer(MorphDataSyncPacket message, ServerPlayer player) {
        PacketDistributor.sendToPlayer(Objects.requireNonNull(player), Objects.requireNonNull(message));
    }
}
