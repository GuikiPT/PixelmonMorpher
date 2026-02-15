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

/**
 * Handles client-side player size changes for morphed players.
 * This ensures the local player's camera height matches their Pokémon size.
 */
@EventBusSubscriber(modid = PixelmonMorpher.MODID, value = Dist.CLIENT)
public class ClientPlayerSizeHandler {

    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) {
            return;
        }

        // Get morph data from client cache
        MorphData morphData = ClientMorphCache.get(player);
        if (morphData == null || !morphData.isMorphed()) {
            return;
        }

        // Get Pokémon dimensions
        float width = morphData.getWidth();
        float height = morphData.getHeight();

        // Adjust dimensions based on pose
        Pose pose = event.getPose();
        EntityDimensions newDimensions = switch (pose) {
            case SWIMMING, FALL_FLYING, SPIN_ATTACK ->
                // For swimming/flying poses, make it flatter
                EntityDimensions.scalable(width, Math.max(0.6f, height * 0.5f));

            case CROUCHING ->
                // For crouching, reduce height by 15%
                EntityDimensions.scalable(width, height * 0.85f);

            default ->
                // Use full Pokémon dimensions
                EntityDimensions.scalable(width, height);
        };

        // Set the new dimensions (this affects camera height)
        if (newDimensions != null) {
            event.setNewSize(newDimensions);
        }
    }
}
