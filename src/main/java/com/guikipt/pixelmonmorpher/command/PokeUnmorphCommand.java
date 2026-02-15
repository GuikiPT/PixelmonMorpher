package com.guikipt.pixelmonmorpher.command;

import com.guikipt.pixelmonmorpher.morph.MorphData;
import com.guikipt.pixelmonmorpher.morph.PlayerMorphAttachment;
import com.guikipt.pixelmonmorpher.network.MorphDataSyncPacket;
import com.guikipt.pixelmonmorpher.network.NetworkHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

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
                .executes(context -> executeUnmorph(Objects.requireNonNull(context), null)) // Unmorph self
                .then(Commands.argument("player", Objects.requireNonNull(EntityArgument.player()))
                    .executes(context -> executeUnmorph(Objects.requireNonNull(context), EntityArgument.getPlayer(context, "player")))
                )
        );
    }

    private static int executeUnmorph(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
        // If no target specified, try to unmorph the command source
        if (targetPlayer == null) {
            if (context.getSource().getEntity() instanceof ServerPlayer player) {
                targetPlayer = player;
            } else {
                context.getSource().sendFailure(Objects.requireNonNull(Component.literal("§cYou must specify a player to unmorph!")));
                return 0;
            }
        }

        // Check if player is currently morphed
        MorphData currentMorph = PlayerMorphAttachment.getMorphData(targetPlayer);
        if (!currentMorph.isMorphed()) {
            context.getSource().sendFailure(Objects.requireNonNull(Component.literal("§c" + targetPlayer.getName().getString() + " is not currently morphed!")));
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
            () -> Objects.requireNonNull(Component.literal("§e" + playerName + " has been unmorphed!")),
            true
        );

        targetPlayer.sendSystemMessage(Objects.requireNonNull(Component.literal("§eYou have returned to your normal form!")));

        return 1;
    }
}
