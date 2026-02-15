package com.guikipt.pixelmonmorpher;

import org.slf4j.Logger;

import com.guikipt.pixelmonmorpher.command.PokeMorphCommand;
import com.guikipt.pixelmonmorpher.command.PokeUnmorphCommand;
import com.guikipt.pixelmonmorpher.item.SynchroMachineItem;
import com.guikipt.pixelmonmorpher.morph.PlayerMorphAttachment;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Objects;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(PixelmonMorpher.MODID)
public class PixelmonMorpher {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "pixelmonmorpher";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Items which will all be registered under the "pixelmonmorpher" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "pixelmonmorpher" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Objects.requireNonNull(Registries.CREATIVE_MODE_TAB), MODID);

    // Synchro Machine - The main item for morphing into Pokémon
    public static final DeferredItem<Item> SYNCHRO_MACHINE = ITEMS.register("synchro_machine",
            () -> new SynchroMachineItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.UNCOMMON)));

    // Creates a creative tab with the id "pixelmonmorpher:example_tab" for the Synchro Machine and other items
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Objects.requireNonNull(Component.translatable("itemGroup.pixelmonmorpher")))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> SYNCHRO_MACHINE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(Objects.requireNonNull(SYNCHRO_MACHINE.get()));
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public PixelmonMorpher(IEventBus modEventBus, ModContainer modContainer) {
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // Register the attachment types for player morph data
        PlayerMorphAttachment.ATTACHMENT_TYPES.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }



    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Register commands
        PokeMorphCommand.register(event.getServer().getCommands().getDispatcher());
        PokeUnmorphCommand.register(event.getServer().getCommands().getDispatcher());
    }
}
