package com.guikipt.pixelmonmorpher.event;

import com.guikipt.pixelmonmorpher.PixelmonMorpher;
import com.guikipt.pixelmonmorpher.morph.PlayerMorphAttachment;
import com.guikipt.pixelmonmorpher.network.MorphDataSyncPacket;
import com.guikipt.pixelmonmorpher.network.NetworkHandler;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Syncs morph data to clients when players join or start tracking another player.
 */
@EventBusSubscriber(modid = PixelmonMorpher.MODID)
public class MorphSyncHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var data = PlayerMorphAttachment.getMorphData(player);
        NetworkHandler.sendToPlayer(new MorphDataSyncPacket(player.getUUID(), data), player);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer watcher)) {
            return;
        }
        if (!(event.getTarget() instanceof ServerPlayer target)) {
            return;
        }
        var data = PlayerMorphAttachment.getMorphData(target);
        NetworkHandler.sendToPlayer(new MorphDataSyncPacket(target.getUUID(), data), watcher);
    }
}
