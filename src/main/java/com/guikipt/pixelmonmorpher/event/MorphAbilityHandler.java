package com.guikipt.pixelmonmorpher.event;

import com.guikipt.pixelmonmorpher.PixelmonMorpher;
import com.guikipt.pixelmonmorpher.morph.MorphData;
import com.guikipt.pixelmonmorpher.morph.PlayerMorphAttachment;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonFactory;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Handles special abilities for morphed players:
 * - Flight ability for flying Pokémon (even in survival)
 * - No fall damage for flying Pokémon
 * - Water breathing for water Pokémon
 */
@EventBusSubscriber(modid = PixelmonMorpher.MODID)
public class MorphAbilityHandler {

    @SuppressWarnings("deprecation")
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        // Only handle server-side for ServerPlayer
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Get morph data
        MorphData morphData = PlayerMorphAttachment.getMorphData(serverPlayer);
        if (!morphData.isMorphed()) {
            // Not morphed - remove flight if in survival
            if (!serverPlayer.isCreative() && !serverPlayer.isSpectator()) {
                if (serverPlayer.getAbilities().mayfly && !serverPlayer.getAbilities().instabuild) {
                    serverPlayer.getAbilities().mayfly = false;
                    serverPlayer.getAbilities().flying = false;
                    serverPlayer.onUpdateAbilities();
                }
            }
            return;
        }

        // Get Pokémon species to check abilities
        Species species = PixelmonSpecies.fromNameOrDex(morphData.getSpeciesName()).orElse(null);
        if (species == null) {
            return;
        }

        // Create a temporary Pokemon to check its properties
        Pokemon pokemon = PokemonFactory.create(species);
        pokemon.setShiny(morphData.isShiny());

        // Check if Pokémon can fly
        boolean canFly = canPokemonFly(pokemon);

        // Always log when ability state changes
        boolean currentCanFly = serverPlayer.getAbilities().mayfly;
        if (canFly != currentCanFly && !serverPlayer.isCreative() && !serverPlayer.isSpectator()) {
            PixelmonMorpher.LOGGER.info("Flight state changing for {}: canFly={}, currentMayfly={}, species={}",
                serverPlayer.getName().getString(), canFly, currentCanFly, morphData.getSpeciesName());
        }

        // Debug logging
        if (serverPlayer.tickCount % 100 == 0) { // Log every 5 seconds
            PixelmonMorpher.LOGGER.debug("Morph check for {}: species={}, canFly={}, isCreative={}, mayfly={}",
                serverPlayer.getName().getString(), morphData.getSpeciesName(), canFly,
                serverPlayer.isCreative(), serverPlayer.getAbilities().mayfly);
        }

        // Grant flight ability in survival if Pokémon can fly
        if (canFly && !serverPlayer.isCreative() && !serverPlayer.isSpectator()) {
            if (!serverPlayer.getAbilities().mayfly) {
                serverPlayer.getAbilities().mayfly = true;
                serverPlayer.onUpdateAbilities();
                PixelmonMorpher.LOGGER.info("Granted flight ability to {} (morphed as {})",
                    serverPlayer.getName().getString(), morphData.getSpeciesName());
            }
        } else if (!canFly && !serverPlayer.isCreative() && !serverPlayer.isSpectator()) {
            // Remove flight if Pokémon can't fly and player isn't in creative
            if (serverPlayer.getAbilities().mayfly && !serverPlayer.getAbilities().instabuild) {
                serverPlayer.getAbilities().mayfly = false;
                serverPlayer.getAbilities().flying = false;
                serverPlayer.onUpdateAbilities();
                PixelmonMorpher.LOGGER.info("Removed flight ability from {} (morphed as non-flying Pokémon)",
                    serverPlayer.getName().getString());
            }
        }

        // Handle water breathing for water Pokémon
        boolean isWaterType = isWaterPokemon(pokemon);
        if (isWaterType && serverPlayer.isUnderWater()) {
            // Set air supply to max to prevent drowning
            serverPlayer.setAirSupply(serverPlayer.getMaxAirSupply());
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Get morph data
        MorphData morphData = PlayerMorphAttachment.getMorphData(player);
        if (!morphData.isMorphed()) {
            return;
        }

        // Get Pokémon species
        Species species = PixelmonSpecies.fromNameOrDex(morphData.getSpeciesName()).orElse(null);
        if (species == null) {
            return;
        }

        // Create a temporary Pokemon to check if it can fly
        Pokemon pokemon = PokemonFactory.create(species);
        pokemon.setShiny(morphData.isShiny());

        // Cancel fall damage for flying Pokémon
        if (canPokemonFly(pokemon)) {
            event.setCanceled(true);
        }
    }

    /**
     * Check if a Pokémon can fly based on its properties
     * Uses Pixelmon's built-in flying detection
     */
    private static boolean canPokemonFly(Pokemon pokemon) {
        try {
            // Check if the Pokémon entity can fly/levitate using Pixelmon's API
            var entity = pokemon.getOrCreatePixelmon(null);
            if (entity != null) {
                // Check if entity has flying or hovering capability
                boolean canFly = entity.canFly() || entity.isHovering();

                String speciesName = pokemon.getSpecies().getName();
                PixelmonMorpher.LOGGER.debug("Checking flight for {}: canFly={}, isHovering={}",
                    speciesName, entity.canFly(), entity.isHovering());

                return canFly;
            }

            return false;
        } catch (Exception e) {
            PixelmonMorpher.LOGGER.error("Error checking if Pokemon can fly", e);
            return false;
        }
    }

    /**
     * Check if a Pokémon is a water type using Pixelmon's API
     */
    private static boolean isWaterPokemon(Pokemon pokemon) {
        try {
            // Create entity to check types
            var entity = pokemon.getOrCreatePixelmon(null);
            if (entity != null) {
                // Get type from entity's form
                var form = entity.getForm();
                if (form != null) {
                    var types = form.getTypes();
                    if (!types.isEmpty()) {
                        // Use the Holder's key to get type name instead of toString() to avoid StackOverflowError
                        var type1Holder = types.get(0);
                        var type2Holder = types.size() > 1 ? types.get(1) : null;

                        // Safely get type names from holder keys
                        String type1Name = null;
                        if (type1Holder.getKey() != null) {
                            type1Name = type1Holder.getKey().location().getPath().toLowerCase();
                        }

                        String type2Name = null;
                        if (type2Holder != null && type2Holder.getKey() != null) {
                            type2Name = type2Holder.getKey().location().getPath().toLowerCase();
                        }

                        boolean isWater = type1Name != null && type1Name.contains("water");
                        if (!isWater && type2Name != null) {
                            isWater = type2Name.contains("water");
                        }

                        // PixelmonMorpher.LOGGER.debug("Checking water type for {}: type1={}, type2={}, isWater={}",
                        //     pokemon.getSpecies().getName(), type1Name,
                        //     type2Name != null ? type2Name : "none", isWater);

                        return isWater;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            PixelmonMorpher.LOGGER.error("Error checking if Pokemon is water type", e);
            return false;
        }
    }
}
