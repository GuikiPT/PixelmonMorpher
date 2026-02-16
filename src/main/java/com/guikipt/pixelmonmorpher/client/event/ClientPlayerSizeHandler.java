package com.guikipt.pixelmonmorpher.client.event;

import com.guikipt.pixelmonmorpher.PixelmonMorpher;
import com.guikipt.pixelmonmorpher.client.morph.ClientMorphCache;
import com.guikipt.pixelmonmorpher.morph.MorphData;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles client-side player size changes for morphed players.
 * This ensures the local player's camera height matches their Pokémon size.
 */
@EventBusSubscriber(modid = PixelmonMorpher.MODID, value = Dist.CLIENT)
public class ClientPlayerSizeHandler {

    // Cache to prevent recalculation on every event
    private static final Map<UUID, CachedDimensions> DIMENSION_CACHE = new ConcurrentHashMap<>();

    private static class CachedDimensions {
        final String speciesName;
        final String formName;
        final float width;
        final float height;
        final Map<Pose, EntityDimensions> dimensionsByPose = new ConcurrentHashMap<>();

        CachedDimensions(String speciesName, String formName, float width, float height) {
            this.speciesName = speciesName;
            this.formName = formName;
            this.width = width;
            this.height = height;
        }

        boolean matches(MorphData morphData) {
            if (morphData == null) return false;
            return speciesName.equals(morphData.getSpeciesName()) &&
                   (formName == null ? morphData.getFormName() == null : formName.equals(morphData.getFormName())) &&
                   width == morphData.getWidth() &&
                   height == morphData.getHeight();
        }
    }

    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) {
            return;
        }

        // Get morph data from client cache
        MorphData morphData = ClientMorphCache.get(player);
        
        UUID playerId = player.getUUID();
        
        // If not morphed, clear cache and return
        if (morphData == null || !morphData.isMorphed()) {
            DIMENSION_CACHE.remove(playerId);
            return;
        }

        // Get or create cached dimensions for this player
        CachedDimensions cached = DIMENSION_CACHE.get(playerId);
        
        // Check if cache needs update (morph changed)
        if (cached == null || !cached.matches(morphData)) {
            cached = new CachedDimensions(
                morphData.getSpeciesName(),
                morphData.getFormName(),
                morphData.getWidth(),
                morphData.getHeight()
            );
            DIMENSION_CACHE.put(playerId, cached);
        }

        // Final reference for lambda
        final CachedDimensions finalCached = cached;

        // Get dimensions for current pose (compute once per morph+pose combination)
        Pose pose = event.getPose();
        EntityDimensions newDimensions = finalCached.dimensionsByPose.computeIfAbsent(pose, p -> {
            return switch (p) {
                case SWIMMING, FALL_FLYING, SPIN_ATTACK ->
                    // For swimming/flying poses, make it flatter
                    EntityDimensions.scalable(finalCached.width, Math.max(0.6f, finalCached.height * 0.5f));

                case CROUCHING ->
                    // For crouching, reduce height by 15%
                    EntityDimensions.scalable(finalCached.width, finalCached.height * 0.85f);

                default ->
                    // Use full Pokémon dimensions
                    EntityDimensions.scalable(finalCached.width, finalCached.height);
            };
        });

        // Set the cached dimensions (this affects camera height)
        event.setNewSize(newDimensions);
    }
}
