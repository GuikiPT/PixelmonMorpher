package com.guikipt.pixelmonmorpher.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

/**
 * The Synchro Machine item - used to morph into Pokémon
 */
public class SynchroMachineItem extends Item {
    
    public SynchroMachineItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(@Nonnull ItemStack stack) {
        // Make the item have an enchanted glint
        return true;
    }
}
