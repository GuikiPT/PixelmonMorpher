package com.guikipt.pixelmonmorpher.command;

import com.guikipt.pixelmonmorpher.morph.MorphData;
import com.guikipt.pixelmonmorpher.morph.PlayerMorphAttachment;
import com.guikipt.pixelmonmorpher.network.MorphDataSyncPacket;
import com.guikipt.pixelmonmorpher.network.NetworkHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Command to unmorph a player back to normal.
 * Usage: /pokeunmorph [player]
 * If no player is specified, unmorphs the command executor.
 */
public class PokeUnmorphCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("pokeunmorph")
                .requires(source -> source.hasPermission(2)) // Require OP level 2
                .executes(context -> executeUnmorph(context, null)) // Unmorph self
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> executeUnmorph(context, EntityArgument.getPlayer(context, "player")))
                )
        );
    }

    private static int executeUnmorph(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
        // If no target specified, try to unmorph the command source
        if (targetPlayer == null) {
            if (context.getSource().getEntity() instanceof ServerPlayer player) {
                targetPlayer = player;
            } else {
                context.getSource().sendFailure(Component.literal("§cYou must specify a player to unmorph!"));
                return 0;
            }
        }

        // Check if player is currently morphed
        MorphData currentMorph = PlayerMorphAttachment.getMorphData(targetPlayer);
        if (!currentMorph.isMorphed()) {
            context.getSource().sendFailure(Component.literal("§c" + targetPlayer.getName().getString() + " is not currently morphed!"));
            return 0;
        }

        // Clear the morph
        PlayerMorphAttachment.clearMorph(targetPlayer);

        // CRITICAL: Force dimensions back to normal
        targetPlayer.refreshDimensions();

        // Sync to all clients
        MorphData emptyMorph = new MorphData();
        NetworkHandler.sendToAll(new MorphDataSyncPacket(targetPlayer.getUUID(), emptyMorph));

        // Create final variable for lambda
        final String playerName = targetPlayer.getName().getString();

        // Send success messages
        context.getSource().sendSuccess(
            () -> Component.literal("§e" + playerName + " has been unmorphed!"),
            true
        );

        targetPlayer.sendSystemMessage(Component.literal("§eYou have returned to your normal form!"));

        return 1;
    }
}
