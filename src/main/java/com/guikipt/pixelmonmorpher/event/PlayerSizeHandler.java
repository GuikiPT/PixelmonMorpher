package com.guikipt.pixelmonmorpher.event;

import com.guikipt.pixelmonmorpher.PixelmonMorpher;
import com.guikipt.pixelmonmorpher.morph.MorphData;
import com.guikipt.pixelmonmorpher.morph.PlayerMorphAttachment;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;

/**
 * Handles dynamic player size changes based on morph.
 * Changes player hitbox, eye height, and POV to match the Pokémon.
 */
@EventBusSubscriber(modid = PixelmonMorpher.MODID)
public class PlayerSizeHandler {

    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
            return;
        }

        // Get morph data
        MorphData morphData = PlayerMorphAttachment.getMorphData(player);
        if (!morphData.isMorphed()) {
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

        // Set the new dimensions (eye height is calculated automatically as 90% of height)
        if (newDimensions != null) {
            event.setNewSize(newDimensions);
        }
    }
}
