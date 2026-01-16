package com.guikipt.pixelmonmorpher.client.render;

import com.guikipt.pixelmonmorpher.PixelmonMorpher;
import com.guikipt.pixelmonmorpher.client.morph.ClientMorphCache;
import com.guikipt.pixelmonmorpher.client.morph.ClientMorphFactory;
import com.guikipt.pixelmonmorpher.morph.MorphData;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

/**
 * Renders Pokémon models for morphed players.
 */
@EventBusSubscriber(modid = PixelmonMorpher.MODID, value = Dist.CLIENT)
public class MorphRenderHandler {

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) {
            return;
        }

        MorphData morphData = ClientMorphCache.get(player);
        if (morphData == null || !morphData.isMorphed()) {
            return;
        }

        // Cancel the player rendering
        event.setCanceled(true);

        try {
            // Get the Pixelmon entity for this morph (should be updated by tick handler)
            PixelmonEntity pixelmonEntity = ClientMorphFactory.getOrCreateEntity(player, morphData);
            if (pixelmonEntity == null) {
                return;
            }

            // Render the Pixelmon entity in place of the player
            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource bufferSource = event.getMultiBufferSource();
            int packedLight = event.getPackedLight();
            float partialTick = event.getPartialTick();

            EntityRenderDispatcher renderManager = Minecraft.getInstance().getEntityRenderDispatcher();

            poseStack.pushPose();

            // Apply size multiplier from morph data
            float sizeMultiplier = morphData.getSize();
            poseStack.scale(sizeMultiplier, sizeMultiplier, sizeMultiplier);

            // Use smooth rotation interpolation for rendering
            float yaw = net.minecraft.util.Mth.rotLerp(partialTick, player.yRotO, player.getYRot());
            float pitch = net.minecraft.util.Mth.lerp(partialTick, player.xRotO, player.getXRot());
            float headYaw = net.minecraft.util.Mth.rotLerp(partialTick, player.yHeadRotO, player.getYHeadRot());

            // Set interpolated rotation for smooth rendering
            pixelmonEntity.setYRot(yaw);
            pixelmonEntity.setXRot(pitch);
            pixelmonEntity.setYHeadRot(headYaw);

            // Render the Pixelmon at the player's position
            renderManager.render(pixelmonEntity, 0, 0, 0, yaw, partialTick, poseStack, bufferSource, packedLight);

            poseStack.popPose();
        } catch (Exception e) {
            PixelmonMorpher.LOGGER.error("Failed to render morphed Pokémon for player {}: {}", player.getName().getString(), e.getMessage());
        }
    }

    /**
     * Disable player shadow when morphed (Pokémon entity will render its own shadow)
     */
    @SubscribeEvent
    public static void onRenderShadow(RenderLivingEvent.Pre<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) {
            return;
        }

        MorphData morphData = ClientMorphCache.get(player);
        if (morphData == null || !morphData.isMorphed()) {
            return;
        }

        // The shadow is already handled by canceling the main render event above
        // This ensures no player shadow is rendered since we cancel the entire player render
    }
}


