package com.guikipt.pixelmonmorpher.morph;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Objects;
import java.util.function.Supplier;

import com.guikipt.pixelmonmorpher.PixelmonMorpher;

/**
 * Manages player morph data using NeoForge's data attachment system
 */
public class PlayerMorphAttachment {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(Objects.requireNonNull(NeoForgeRegistries.ATTACHMENT_TYPES), PixelmonMorpher.MODID);

    public static final Supplier<AttachmentType<MorphData>> MORPH_DATA = Objects.requireNonNull(ATTACHMENT_TYPES.register(
        "morph_data",
        () -> AttachmentType.serializable(MorphData::new).build()
    ));

    /**
     * Get the morph data for a player
     */
    public static MorphData getMorphData(ServerPlayer player) {
        return Objects.requireNonNull(player.getData(Objects.requireNonNull(MORPH_DATA)));
    }

    /**
     * Set morph data for a player
     */
    public static void setMorphData(ServerPlayer player, MorphData data) {
        player.setData(Objects.requireNonNull(MORPH_DATA), Objects.requireNonNull(data));
    }

    /**
     * Check if a player is currently morphed
     */
    public static boolean isMorphed(ServerPlayer player) {
        return getMorphData(player).isMorphed();
    }

    /**
     * Clear a player's morph
     */
    public static void clearMorph(ServerPlayer player) {
        MorphData data = getMorphData(player);
        data.clear();
        setMorphData(player, data);
    }
}
