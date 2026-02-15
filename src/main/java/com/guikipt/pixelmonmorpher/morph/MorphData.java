package com.guikipt.pixelmonmorpher.morph;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Stores data about a player's current morph
 */
public class MorphData implements INBTSerializable<CompoundTag> {
    private String speciesName;
    private String formName; // Changed from int to String to support all form types
    private boolean isShiny;
    private String palette; // For Pixelmon's palette system
    private float size;
    private float width;  // Pokémon width for player hitbox
    private float height; // Pokémon height for player hitbox and eye height
    private boolean isMorphed;

    public MorphData() {
        this.isMorphed = false;
    }

    public MorphData(String speciesName, String formName, boolean isShiny, String palette, float size, float width, float height) {
        this.speciesName = speciesName;
        this.formName = formName;
        this.isShiny = isShiny;
        this.palette = palette;
        this.size = size;
        this.width = width;
        this.height = height;
        this.isMorphed = true;
    }

    public String getSpeciesName() {
        return speciesName;
    }

    public String getFormName() {
        return formName;
    }

    // Deprecated: kept for backwards compatibility
    @Deprecated
    public int getForm() {
        return 0; // Return 0 for base form for backwards compat
    }

    public boolean isShiny() {
        return isShiny;
    }

    public String getPalette() {
        return palette;
    }

    public float getSize() {
        return size;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public boolean isMorphed() {
        return isMorphed;
    }

    public void setMorphed(boolean morphed) {
        isMorphed = morphed;
    }

    /**
     * Save morph data to NBT
     */
    @Override
    public CompoundTag serializeNBT(@Nonnull HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isMorphed", isMorphed);
        if (isMorphed) {
            tag.putString("speciesName", Objects.requireNonNull(speciesName));
            tag.putString("formName", formName != null ? formName : "");
            tag.putBoolean("isShiny", isShiny);
            tag.putString("palette", Objects.requireNonNull(palette));
            tag.putFloat("size", size);
            tag.putFloat("width", width);
            tag.putFloat("height", height);
        }
        return tag;
    }

    /**
     * Load morph data from NBT
     */
    @Override
    public void deserializeNBT(@Nonnull HolderLookup.Provider provider, @Nonnull CompoundTag tag) {
        this.isMorphed = tag.getBoolean("isMorphed");
        if (this.isMorphed) {
            this.speciesName = Objects.requireNonNull(tag.getString("speciesName"));
            this.formName = Objects.requireNonNull(tag.getString("formName"));
            this.isShiny = tag.getBoolean("isShiny");
            this.palette = Objects.requireNonNull(tag.getString("palette"));
            this.size = tag.getFloat("size");
            this.width = tag.getFloat("width");
            this.height = tag.getFloat("height");
        }
    }

    public void clear() {
        this.isMorphed = false;
        this.speciesName = null;
        this.formName = null;
        this.isShiny = false;
        this.palette = null;
        this.size = 1.0f;
        this.width = 0.6f;  // Default player width
        this.height = 1.8f; // Default player height
    }
}
