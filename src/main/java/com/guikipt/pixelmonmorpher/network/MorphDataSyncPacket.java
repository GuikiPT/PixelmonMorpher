package com.guikipt.pixelmonmorpher.network;

import java.util.UUID;

import com.guikipt.pixelmonmorpher.morph.MorphData;
import com.guikipt.pixelmonmorpher.client.morph.ClientMorphCache;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Syncs a player's morph data to clients so rendering can mirror the server state.
 */
public record MorphDataSyncPacket(UUID playerId, MorphData morphData) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MorphDataSyncPacket> TYPE =
        new CustomPacketPayload.Type<>(NetworkHandler.MORPH_SYNC_ID);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static MorphDataSyncPacket decode(FriendlyByteBuf buf) {
        UUID playerId = buf.readUUID();
        boolean isMorphed = buf.readBoolean();
        MorphData data = new MorphData();
        if (isMorphed) {
            String species = buf.readUtf();
            String formName = buf.readUtf();
            boolean shiny = buf.readBoolean();
            String palette = buf.readUtf();
            float size = buf.readFloat();
            float width = buf.readFloat();
            float height = buf.readFloat();
            data = new MorphData(species, formName, shiny, palette, size, width, height);
        } else {
            data.setMorphed(false);
        }
        return new MorphDataSyncPacket(playerId, data);
    }

    public static void handle(MorphDataSyncPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // Update the cache first
            ClientMorphCache.setMorph(msg.playerId, msg.morphData);

            // Force an additional dimension refresh to ensure it takes effect
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level != null) {
                net.minecraft.world.entity.player.Player player = mc.level.getPlayerByUUID(msg.playerId);
                if (player != null) {
                    // Refresh dimensions on the next tick to ensure cache is fully updated
                    mc.execute(() -> player.refreshDimensions());
                }
            }
        });
    }

    public static void encode(MorphDataSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        buf.writeBoolean(msg.morphData != null && msg.morphData.isMorphed());
        if (msg.morphData != null && msg.morphData.isMorphed()) {
            buf.writeUtf(msg.morphData.getSpeciesName());
            buf.writeUtf(msg.morphData.getFormName() != null ? msg.morphData.getFormName() : "");
            buf.writeBoolean(msg.morphData.isShiny());
            buf.writeUtf(msg.morphData.getPalette());
            buf.writeFloat(msg.morphData.getSize());
            buf.writeFloat(msg.morphData.getWidth());
            buf.writeFloat(msg.morphData.getHeight());
        }
    }
}
