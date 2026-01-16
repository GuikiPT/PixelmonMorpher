package com.guikipt.pixelmonmorpher.command;

import com.guikipt.pixelmonmorpher.client.screen.MorphMenuScreen;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Client-side command to open the morph menu GUI
 * Usage: /pokemorph (with no arguments opens GUI)
 */
public class MorphCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("pokemorph")
                .executes(MorphCommand::openMorphMenu)
        );
    }

    private static int openMorphMenu(CommandContext<CommandSourceStack> context) {
        // This runs on the client side
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new MorphMenuScreen()));
        return 1;
    }
}
