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

/**
 * Registers payloads and helper senders for morph sync.
 */
@EventBusSubscriber(modid = PixelmonMorpher.MODID)
public class NetworkHandler {
    public static final ResourceLocation MORPH_SYNC_ID = ResourceLocation.fromNamespaceAndPath(PixelmonMorpher.MODID, "morph_sync");

    public static final StreamCodec<FriendlyByteBuf, MorphDataSyncPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, packet) -> packet.encode(packet, buf),
            MorphDataSyncPacket::decode
        );

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PixelmonMorpher.MODID);
        registrar.playToClient(MorphDataSyncPacket.TYPE, STREAM_CODEC, MorphDataSyncPacket::handle);
        registrar.playToServer(MorphRequestPacket.TYPE, MorphRequestPacket.CODEC, MorphRequestPacket::handle);
    }

    public static void sendToServer(CustomPacketPayload message) {
        PacketDistributor.sendToServer(message);
    }

    public static void sendToAll(MorphDataSyncPacket message) {
        PacketDistributor.sendToAllPlayers(message);
    }

    public static void sendToPlayer(MorphDataSyncPacket message, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, message);
    }
}
