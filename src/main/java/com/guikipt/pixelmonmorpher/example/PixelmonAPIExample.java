package com.guikipt.pixelmonmorpher.example;

// This is an EXAMPLE file showing how to use Pixelmon API
// Uncomment these imports once Pixelmon dependency is added

// import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
// import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
// import net.minecraft.server.level.ServerPlayer;

/**
 * Example class demonstrating Pixelmon API usage for a morph system
 *
 * This shows common patterns you'll need for your Pokémon morph mod:
 * - Getting a player's Pokémon
 * - Accessing Pokémon properties (species, stats, etc.)
 * - Checking Pokémon ownership
 */
public class PixelmonAPIExample {

    /**
     * Get all Pokémon from a player's party
     *
     * @param player The player to get Pokémon from
     * @return Array of Pokémon in the player's party (may contain nulls)
     */
    /*
    public static Pokemon[] getPlayerParty(ServerPlayer player) {
        var storage = StorageProxy.getPartyStorage(player);
        return storage.getAll();
    }
    */

    /**
     * Get a specific Pokémon from a player's party by slot
     *
     * @param player The player
     * @param slot The slot (0-5)
     * @return The Pokémon, or null if slot is empty
     */
    /*
    public static Pokemon getPlayerPokemonBySlot(ServerPlayer player, int slot) {
        if (slot < 0 || slot > 5) {
            return null;
        }
        var storage = StorageProxy.getPartyStorage(player);
        return storage.get(slot);
    }
    */

    /**
     * Check if a player has any Pokémon
     *
     * @param player The player to check
     * @return true if the player has at least one Pokémon
     */
    /*
    public static boolean hasAnyPokemon(ServerPlayer player) {
        var storage = StorageProxy.getPartyStorage(player);
        return storage.getAll().length > 0 && storage.get(0) != null;
    }
    */

    /**
     * Get useful Pokémon information for morphing
     *
     * Example of what you might need to implement a morph:
     * - Species name (for the morph model)
     * - Size/scale (for player hitbox adjustment)
     * - Form (for alternate forms like Alolan, Galarian, etc.)
     */
    /*
    public static class PokemonMorphData {
        private final String speciesName;
        private final int form;
        private final float size;
        private final boolean isShiny;

        public PokemonMorphData(Pokemon pokemon) {
            this.speciesName = pokemon.getSpecies().getName();
            this.form = pokemon.getForm();
            this.size = pokemon.getGrowth().getSize(); // For size-based hitbox
            this.isShiny = pokemon.isShiny();
        }

        public String getSpeciesName() { return speciesName; }
        public int getForm() { return form; }
        public float getSize() { return size; }
        public boolean isShiny() { return isShiny; }
    }
    */

    /**
     * Example: Create morph data from a Pokémon
     */
    /*
    public static PokemonMorphData getMorphData(Pokemon pokemon) {
        if (pokemon == null) {
            return null;
        }
        return new PokemonMorphData(pokemon);
    }
    */

    // COMMON PIXELMON API PATTERNS YOU'LL LIKELY NEED:

    /*
    // Get Pokémon species
    String speciesName = pokemon.getSpecies().getName();

    // Get Pokémon form (for alternate forms)
    int form = pokemon.getForm();

    // Check if shiny
    boolean isShiny = pokemon.isShiny();

    // Get Pokémon size/growth
    float size = pokemon.getGrowth().getSize();

    // Get Pokémon level
    int level = pokemon.getLevel();

    // Get Pokémon stats
    int hp = pokemon.getHealth();
    int maxHP = pokemon.getMaxHealth();

    // Get moves (for implementing Pokémon abilities in morph form)
    var moveset = pokemon.getMoveset();

    // Get ability (for special morph effects)
    var ability = pokemon.getAbility();

    // Access PC storage (if you want players to morph into PC Pokémon)
    var pcStorage = StorageProxy.getPCStorage(player);
    */
}
