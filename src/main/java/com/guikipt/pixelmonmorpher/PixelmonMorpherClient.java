package com.guikipt.pixelmonmorpher;

import com.guikipt.pixelmonmorpher.command.MorphCommand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = PixelmonMorpher.MODID, dist = Dist.CLIENT)
public class PixelmonMorpherClient {
    public PixelmonMorpherClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        NeoForge.EVENT_BUS.addListener(PixelmonMorpherClient::onRegisterClientCommands);
    }

    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        MorphCommand.register(event.getDispatcher());
    }
}
