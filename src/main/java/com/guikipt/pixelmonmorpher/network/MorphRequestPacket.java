package com.guikipt.pixelmonmorpher.network;

import com.guikipt.pixelmonmorpher.morph.MorphData;
import com.guikipt.pixelmonmorpher.morph.PlayerMorphAttachment;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonFactory;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.guikipt.pixelmonmorpher.PixelmonMorpher.MODID;

/**
 * Packet sent from client to server requesting a morph transformation
 */
public record MorphRequestPacket(
    String speciesName,
    String formName,
    boolean isShiny,
    String palette,
    float size,
    Gender gender,
    int level
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MorphRequestPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "morph_request"));

    public static final StreamCodec<ByteBuf, MorphRequestPacket> CODEC = StreamCodec.of(
        MorphRequestPacket::encode,
        MorphRequestPacket::decode
    );

    private static void encode(ByteBuf buf, MorphRequestPacket msg) {
        FriendlyByteBuf friendly = new FriendlyByteBuf(buf);
        friendly.writeUtf(msg.speciesName);
        friendly.writeUtf(msg.formName);
        friendly.writeBoolean(msg.isShiny);
        friendly.writeUtf(msg.palette);
        friendly.writeFloat(msg.size);
        friendly.writeEnum(msg.gender);
        friendly.writeInt(msg.level);
    }

    private static MorphRequestPacket decode(ByteBuf buf) {
        FriendlyByteBuf friendly = new FriendlyByteBuf(buf);
        String speciesName = friendly.readUtf();
        String formName = friendly.readUtf();
        boolean isShiny = friendly.readBoolean();
        String palette = friendly.readUtf();
        float size = friendly.readFloat();
        Gender gender = friendly.readEnum(Gender.class);
        int level = friendly.readInt();

        return new MorphRequestPacket(speciesName, formName, isShiny, palette, size, gender, level);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Handle the packet on the server side
     */
    public static void handle(MorphRequestPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.flow().isServerbound() && ctx.player() instanceof ServerPlayer player) {
                // Get species from registry
                Species species = PixelmonSpecies.fromNameOrDex(msg.speciesName).orElse(null);
                if (species == null) {
                    player.sendSystemMessage(Component.literal("§cUnknown Pokémon: " + msg.speciesName));
                    return;
                }

                // Create Pokemon to get proper form and palette
                Pokemon pokemon = PokemonFactory.create(species);
                pokemon.setShiny(msg.isShiny);
                pokemon.setGender(msg.gender);
                pokemon.setLevel(msg.level);

                // Handle form if specified
                if (msg.formName != null && !msg.formName.isEmpty() && !msg.formName.equalsIgnoreCase("base")) {
                    try {
                        var form = species.getForm(msg.formName);
                        if (form != null) {
                            pokemon.setForm(form);
                        }
                    } catch (Exception e) {
                        // Keep base form
                    }
                }

                // Get dimensions from the Pokémon entity
                float width = 0.6f;  // Default player width
                float height = 1.8f; // Default player height

                try {
                    var entity = pokemon.getOrSpawnPixelmon(null);
                    if (entity != null) {
                        width = entity.getBbWidth();
                        height = entity.getBbHeight();

                        // Clamp dimensions to reasonable values
                        width = Math.max(0.3f, Math.min(3.0f, width));
                        height = Math.max(0.5f, Math.min(3.0f, height));
                    }
                } catch (Exception e) {
                    // Use defaults if entity creation fails
                }

                // Apply size multiplier
                width *= msg.size;
                height *= msg.size;

                // Create morph data
                MorphData morphData = new MorphData(
                    pokemon.getSpecies().getName(),
                    0, // Form index (handled by form system)
                    pokemon.isShiny(),
                    msg.palette != null && !msg.palette.equals("none") ? msg.palette : pokemon.getPalette().getName(),
                    msg.size,
                    width,
                    height
                );

                // Apply the morph
                PlayerMorphAttachment.setMorphData(player, morphData);

                // CRITICAL: Force dimensions update immediately
                player.refreshDimensions();

                // Sync to all clients
                NetworkHandler.sendToAll(new MorphDataSyncPacket(player.getUUID(), morphData));

                // Build display name
                String displayName = pokemon.getSpecies().getName();
                if (msg.isShiny) {
                    displayName = "Shiny " + displayName;
                }
                if (msg.formName != null && !msg.formName.isEmpty() && !msg.formName.equalsIgnoreCase("base")) {
                    displayName += " (" + msg.formName + ")";
                }

                // Send success message
                player.sendSystemMessage(Component.literal("§aYou have morphed into " + displayName + "!"));
            }
        });
    }
}
