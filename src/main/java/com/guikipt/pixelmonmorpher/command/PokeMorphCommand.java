package com.guikipt.pixelmonmorpher.command;

import com.guikipt.pixelmonmorpher.PixelmonMorpher;
import com.guikipt.pixelmonmorpher.morph.MorphData;
import com.guikipt.pixelmonmorpher.morph.PlayerMorphAttachment;
import com.guikipt.pixelmonmorpher.network.MorphDataSyncPacket;
import com.guikipt.pixelmonmorpher.network.NetworkHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonFactory;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to morph a player into a Pokémon.
 * Usage: /pokemorph <player> <pokemon> [shiny] [form:form_name]
 */
public class PokeMorphCommand {

    private static final SuggestionProvider<CommandSourceStack> POKEMON_SUGGESTIONS = (context, builder) -> {
        String input = builder.getRemaining().toLowerCase();
        List<String> suggestions = new ArrayList<>();

        // Get all Pokémon species
        PixelmonSpecies.getAll().forEach(species -> {
            String name = species.getName().toLowerCase();
            if (name.startsWith(input)) {
                suggestions.add(species.getName());
            }
        });

        // Limit to 20 suggestions for performance
        suggestions.stream().limit(20).forEach(builder::suggest);
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("pokemorph")
                // Admin commands require OP level 2
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("pokemon", StringArgumentType.string())
                        .suggests(POKEMON_SUGGESTIONS)
                        .executes(context -> executeMorph(context, false, null))
                        .then(Commands.literal("shiny")
                            .executes(context -> executeMorph(context, true, null))
                            .then(Commands.argument("form", StringArgumentType.string())
                                .executes(context -> executeMorph(context, true, StringArgumentType.getString(context, "form")))
                            )
                        )
                        .then(Commands.argument("form", StringArgumentType.string())
                            .executes(context -> executeMorph(context, false, StringArgumentType.getString(context, "form")))
                        )
                    )
                )
        );
    }

    private static int executeMorph(CommandContext<CommandSourceStack> context, boolean shiny, String formName) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        String pokemonName = StringArgumentType.getString(context, "pokemon");

        // Get species from registry
        Species species = PixelmonSpecies.fromNameOrDex(pokemonName).orElse(null);
        if (species == null) {
            context.getSource().sendFailure(Component.literal("§cUnknown Pokémon: " + pokemonName));
            return 0;
        }

        // Create Pokemon to get proper form and palette
        Pokemon pokemon = PokemonFactory.create(species);
        pokemon.setShiny(shiny);

        // Handle form if specified
        if (formName != null && !formName.isEmpty()) {
            // Try to set the form
            try {
                // Pixelmon handles form names through the form system
                var form = species.getForm(formName);
                if (form != null) {
                    pokemon.setForm(form);
                } else {
                    context.getSource().sendFailure(Component.literal("§cUnknown form '" + formName + "' for " + species.getName()));
                    return 0;
                }
            } catch (Exception e) {
                context.getSource().sendFailure(Component.literal("§cInvalid form: " + formName));
                return 0;
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

                PixelmonMorpher.LOGGER.info("Command morph - Entity dimensions for {}: width={}, height={}",
                    pokemon.getSpecies().getName(), width, height);

                // Clamp dimensions to reasonable values
                width = Math.max(0.3f, Math.min(3.0f, width));
                height = Math.max(0.5f, Math.min(3.0f, height));

                PixelmonMorpher.LOGGER.info("Command morph - Clamped dimensions: width={}, height={}", width, height);
            } else {
                PixelmonMorpher.LOGGER.warn("Command morph - Failed to create entity for {}", pokemon.getSpecies().getName());
            }
        } catch (Exception e) {
            PixelmonMorpher.LOGGER.error("Command morph - Error getting dimensions", e);
            // Use defaults if entity creation fails
        }

        // Get the actual form name
        String actualFormName = pokemon.getForm().getName();
        PixelmonMorpher.LOGGER.info("Command morph - Form name: '{}' (requested: '{}')", actualFormName, formName);

        // Create morph data with the actual form name
        MorphData morphData = new MorphData(
            pokemon.getSpecies().getName(),
            actualFormName, // Use the actual form name from the pokemon
            pokemon.isShiny(),
            pokemon.getPalette().getName(),
            1.0f,
            width,
            height
        );

        // Apply the morph
        PlayerMorphAttachment.setMorphData(targetPlayer, morphData);

        // CRITICAL: Force dimensions update immediately
        targetPlayer.refreshDimensions();

        // Sync to all clients
        NetworkHandler.sendToAll(new MorphDataSyncPacket(targetPlayer.getUUID(), morphData));

        // Force another dimension refresh after a small delay to ensure it takes effect
        // This is needed because sometimes the first refresh happens before the data is fully synced
        var server = targetPlayer.getServer();
        if (server != null) {
            server.execute(targetPlayer::refreshDimensions);
        }

        // Build display name
        String displayName = pokemon.getSpecies().getName();
        if (shiny) {
            displayName = "Shiny " + displayName;
        }
        if (formName != null && !formName.isEmpty()) {
            displayName += " (" + formName + ")";
        }

        // Create final variables for lambda
        final String finalDisplayName = displayName;
        final String playerName = targetPlayer.getName().getString();

        // Send success messages
        context.getSource().sendSuccess(
            () -> Component.literal("§aMorphed " + playerName + " into " + finalDisplayName + "!"),
            true
        );

        targetPlayer.sendSystemMessage(Component.literal("§aYou have been morphed into " + finalDisplayName + "!"));

        return 1;
    }
}
