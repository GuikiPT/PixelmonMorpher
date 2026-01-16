package com.guikipt.pixelmonmorpher.client.morph;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.guikipt.pixelmonmorpher.morph.MorphData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Client-side cache of morph data for rendering.
 */
public class ClientMorphCache {
    private static final Map<UUID, MorphData> CACHE = new ConcurrentHashMap<>();

    public static MorphData get(AbstractClientPlayer player) {
        return CACHE.get(player.getUUID());
    }

    public static void setMorph(UUID playerId, MorphData data) {
        MorphData oldData = CACHE.get(playerId);

        // Clear entity cache if morph is changing
        if (oldData != null && oldData.isMorphed()) {
            ClientMorphFactory.clearCache(playerId);
        }

        if (data == null || !data.isMorphed()) {
            CACHE.remove(playerId);
            ClientMorphFactory.clearCache(playerId);
        } else {
            CACHE.put(playerId, data);
        }

        // CRITICAL: Force dimension refresh on the client
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Player player = mc.level.getPlayerByUUID(playerId);
            if (player != null) {
                player.refreshDimensions();
            }
        }
    }

    public static boolean isMorphed(AbstractClientPlayer player) {
        MorphData data = get(player);
        return data != null && data.isMorphed();
    }
}
