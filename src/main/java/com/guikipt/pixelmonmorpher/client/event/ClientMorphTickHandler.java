package com.guikipt.pixelmonmorpher.client.event;

import com.guikipt.pixelmonmorpher.PixelmonMorpher;
import com.guikipt.pixelmonmorpher.client.morph.ClientMorphCache;
import com.guikipt.pixelmonmorpher.client.morph.ClientMorphFactory;
import com.guikipt.pixelmonmorpher.morph.MorphData;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.util.Objects;

/**
 * Handles client tick updates for morphed player entities.
 * This runs once per tick (not per frame) to properly update animations.
 */
@EventBusSubscriber(modid = PixelmonMorpher.MODID, value = Dist.CLIENT)
public class ClientMorphTickHandler {

    private static int lastTickCount = -1;

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        // Store non-null references for null safety
        var level = Objects.requireNonNull(mc.level);
        var player = Objects.requireNonNull(mc.player);

        // Only update once per game tick (not per render frame)
        int currentTickCount = player.tickCount;
        if (currentTickCount == lastTickCount) {
            return;
        }
        lastTickCount = currentTickCount;

        // Update all morphed players once per tick
        for (var otherPlayer : level.players()) {
            if (!(otherPlayer instanceof AbstractClientPlayer clientPlayer)) {
                continue;
            }

            MorphData morphData = ClientMorphCache.get(clientPlayer);
            if (morphData == null || !morphData.isMorphed()) {
                continue;
            }

            // Get or create the entity
            PixelmonEntity entity = ClientMorphFactory.getOrCreateEntity(clientPlayer, morphData);
            if (entity == null) {
                continue;
            }

            // Update entity once per tick (not per render frame)
            ClientMorphFactory.updateEntityTick(clientPlayer, entity);
        }
    }
}
