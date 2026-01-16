package com.guikipt.pixelmonmorpher.event;

import com.guikipt.pixelmonmorpher.PixelmonMorpher;
import com.guikipt.pixelmonmorpher.morph.MorphData;
import com.guikipt.pixelmonmorpher.morph.PlayerMorphAttachment;
import com.guikipt.pixelmonmorpher.network.MorphDataSyncPacket;
import com.guikipt.pixelmonmorpher.network.NetworkHandler;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Handles player interactions with Pixelmon entities
 */
@EventBusSubscriber(modid = PixelmonMorpher.MODID)
public class PokemonInteractionHandler {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();

        // Only handle on server side
        if (player.level().isClientSide) {
            return;
        }

        // Check if player is using a stick in main hand
        if (event.getHand() != InteractionHand.MAIN_HAND ||
            !event.getItemStack().is(Items.STICK)) {
            return;
        }

        // Check if the target entity is a Pixelmon
        if (!(event.getTarget() instanceof PixelmonEntity pixelmonEntity)) {
            return;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        Pokemon pokemon = pixelmonEntity.getPokemon();

        // Check if the Pokémon is wild (not owned by a player)
        if (pokemon.getOwnerPlayer() != null) {
            serverPlayer.sendSystemMessage(Component.literal("§cYou can only morph into wild Pokémon!"));
            return;
        }

        // Check if the Pokémon is in battle
        if (BattleRegistry.getBattle(pixelmonEntity) != null) {
            serverPlayer.sendSystemMessage(Component.literal("§cYou cannot morph into a Pokémon that's in battle!"));
            return;
        }

        // Create morph data from the Pokémon
        MorphData morphData = createMorphDataFromPokemon(pokemon);

        // Apply the morph
        PlayerMorphAttachment.setMorphData(serverPlayer, morphData);

        // CRITICAL: Force dimensions update immediately
        serverPlayer.refreshDimensions();

        // Sync to all clients so they can render the morph
        NetworkHandler.sendToAll(new MorphDataSyncPacket(serverPlayer.getUUID(), morphData));

        // Send success message
        String pokemonName = pokemon.getSpecies().getName();
        serverPlayer.sendSystemMessage(Component.literal("§aYou have morphed into " + pokemonName + "!"));

        PixelmonMorpher.LOGGER.info("Player {} morphed into {} (Form: {}, Shiny: {})",
            serverPlayer.getName().getString(),
            pokemonName,
            pokemon.getForm().getName(),
            pokemon.isShiny());

        // Cancel the event to prevent other interactions
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();

        // Only handle on server side
        if (player.level().isClientSide) {
            return;
        }

        // Check if player is using a stick and sneaking (shift+right-click)
        if (!event.getItemStack().is(Items.STICK) || !player.isShiftKeyDown()) {
            return;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        MorphData currentMorph = PlayerMorphAttachment.getMorphData(serverPlayer);

        // Only unmorph if currently morphed
        if (!currentMorph.isMorphed()) {
            return;
        }

        // Clear the morph
        PlayerMorphAttachment.clearMorph(serverPlayer);

        // CRITICAL: Force dimensions back to normal
        serverPlayer.refreshDimensions();

        // Sync to all clients
        MorphData emptyMorph = new MorphData();
        NetworkHandler.sendToAll(new MorphDataSyncPacket(serverPlayer.getUUID(), emptyMorph));

        // Send message
        serverPlayer.sendSystemMessage(Component.literal("§eYou have returned to your normal form!"));

        PixelmonMorpher.LOGGER.info("Player {} unmorphed", serverPlayer.getName().getString());

        // Cancel to prevent block interaction
        event.setCanceled(true);
    }

    /**
     * Creates MorphData from a Pokémon
     */
    private static MorphData createMorphDataFromPokemon(Pokemon pokemon) {
        String speciesName = pokemon.getSpecies().getName();
        int form = 0; // TODO: Find correct form API in Pixelmon 9.3.13
        boolean isShiny = pokemon.isShiny();
        String palette = pokemon.getPalette().getName();
        float size = 1.0f; // Default size for now

        // Get dimensions from the Pokémon entity
        PixelmonEntity entity = pokemon.getOrSpawnPixelmon(null);
        float width = 0.6f;  // Default player width
        float height = 1.8f; // Default player height

        if (entity != null) {
            // Get the actual dimensions from the Pokémon entity
            width = entity.getBbWidth();
            height = entity.getBbHeight();

            // Clamp dimensions to reasonable values for player gameplay
            // Min: 0.3 (very small Pokémon like Joltik)
            // Max: 3.0 (very large Pokémon, scaled down for gameplay)
            width = Math.max(0.3f, Math.min(3.0f, width));
            height = Math.max(0.5f, Math.min(3.0f, height));
        }

        return new MorphData(speciesName, form, isShiny, palette, size, width, height);
    }
}
