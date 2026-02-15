package com.guikipt.pixelmonmorpher.client.morph;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.guikipt.pixelmonmorpher.morph.MorphData;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonFactory;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;

import net.minecraft.world.entity.player.Player;

/**
 * Creates and caches Pixelmon entities for morphed players on the client.
 */
public class ClientMorphFactory {
    private static final Map<UUID, PixelmonEntity> ENTITY_CACHE = new ConcurrentHashMap<>();


    /**
     * Get or create a Pixelmon entity for the given player's morph.
     */
    public static PixelmonEntity getOrCreateEntity(Player player, MorphData morphData) {
        if (morphData == null || !morphData.isMorphed()) {
            ENTITY_CACHE.remove(player.getUUID());
            return null;
        }

        return ENTITY_CACHE.computeIfAbsent(player.getUUID(), uuid -> createEntity(player, morphData));
    }

    private static PixelmonEntity createEntity(Player player, MorphData morphData) {
        try {
            // Get species from registry
            Species species = PixelmonSpecies.fromNameOrDex(morphData.getSpeciesName()).orElse(null);
            if (species == null) {
                return null;
            }

            // Create Pokemon
            Pokemon pokemon = PokemonFactory.create(species);
            pokemon.setShiny(morphData.isShiny());

            // Debug: Log the form name we're trying to apply
            String formName = morphData.getFormName();
            com.guikipt.pixelmonmorpher.PixelmonMorpher.LOGGER.info("Client creating entity: species={}, formName='{}', isShiny={}",
                morphData.getSpeciesName(), formName, morphData.isShiny());

            // Apply the form if specified
            if (formName != null && !formName.isEmpty() && !formName.equalsIgnoreCase("base")) {
                try {
                    var form = species.getForm(formName);
                    if (form != null) {
                        pokemon.setForm(form);
                        com.guikipt.pixelmonmorpher.PixelmonMorpher.LOGGER.info("Client applied form: {}", formName);
                    } else {
                        com.guikipt.pixelmonmorpher.PixelmonMorpher.LOGGER.warn("Client failed to find form: {}", formName);
                    }
                } catch (Exception e) {
                    com.guikipt.pixelmonmorpher.PixelmonMorpher.LOGGER.error("Client error applying form: {}", formName, e);
                    // Form not found, use base form
                }
            }

            // Create entity directly without spawning into world
            PixelmonEntity entity = pokemon.getOrCreatePixelmon(player);
            if (entity != null) {
                // CRITICAL: Disable AI so entity doesn't have free will
                entity.setNoAi(true);
                entity.stopInPlace();

                // Don't add to world - we just want to render it
                // Position the entity at the player's location
                entity.setPos(player.getX(), player.getY(), player.getZ());

                // Set rotation values
                entity.setYRot(player.getYRot());
                entity.yRotO = player.yRotO;
                entity.setXRot(player.getXRot());
                entity.xRotO = player.xRotO;
                entity.setYHeadRot(player.getYHeadRot());
                entity.yHeadRotO = player.yHeadRotO;
                entity.yBodyRot = player.yBodyRot;
                entity.yBodyRotO = player.yBodyRot;

                // Initialize entity for rendering
                entity.tickCount = player.tickCount;
            }

            return entity;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Update the entity once per client tick to advance animations.
     * Called from ClientTickEvent.Post, NOT from render events.
     */
    public static void updateEntityTick(Player player, PixelmonEntity entity) {
        if (entity != null) {
            // Copy previous tick position from player -> entity (gives real per-tick deltas)
            entity.xOld = player.xOld;
            entity.yOld = player.yOld;
            entity.zOld = player.zOld;

            // Set current position
            entity.setPos(player.getX(), player.getY(), player.getZ());

            // Copy rotations (old + current)
            entity.yRotO = player.yRotO;
            entity.xRotO = player.xRotO;
            entity.yHeadRotO = player.yHeadRotO;
            entity.yBodyRotO = player.yBodyRotO;

            entity.setYRot(player.getYRot());
            entity.setXRot(player.getXRot());
            entity.setYHeadRot(player.getYHeadRot());
            entity.yBodyRot = player.yBodyRot;

            // Copy pose for swim/crouch animations
            entity.setPose(Objects.requireNonNull(player.getPose()));

            // Detect idle state based on actual movement delta
            double dx = entity.getX() - entity.xOld;
            double dz = entity.getZ() - entity.zOld;
            double horizontalMovement = Math.sqrt(dx * dx + dz * dz);
            boolean isIdle = horizontalMovement < 0.01 && player.onGround() && !player.isSprinting() && !player.isSwimming();

            // Handle movement state
            if (isIdle) {
                // True idle: force no delta and clear movement
                entity.setOldPosAndRot();
                entity.setDeltaMovement(0, 0, 0);
                entity.setXxa(0);
                entity.setYya(0);
                entity.setZza(0);
                entity.setSpeed(0);
                entity.setSprinting(false);
                entity.setSwimming(false);
                entity.stopInPlace();
            } else {
                // Moving: copy movement state
                entity.setDeltaMovement(Objects.requireNonNull(player.getDeltaMovement()));
                entity.setSprinting(player.isSprinting());
                entity.setSwimming(player.isSwimming());
            }

            // Always set these states
            entity.setOnGround(player.onGround());
            entity.setShiftKeyDown(player.isShiftKeyDown());

            // Set collision flags
            entity.horizontalCollision = player.horizontalCollision;
            entity.verticalCollision = player.verticalCollision;
            entity.verticalCollisionBelow = player.verticalCollisionBelow;

            // PIXELMON-SPECIFIC: Handle flying state
            boolean isFlying = player.getAbilities().flying || player.isFallFlying();
            entity.setFlying(isFlying);
            if (!isFlying) {
                entity.setHoverTicks(0);
            }

            // Update tick count
            entity.tickCount = player.tickCount;

            // Tick the entity to advance animations
            try {
                entity.tick();
            } catch (Exception e) {
                // Silently handle errors
            }

            // Re-lock position after tick to prevent drift
            entity.setPos(player.getX(), player.getY(), player.getZ());
        }
    }

    /**
     * Clear the entity cache for a player.
     */
    public static void clearCache(UUID playerId) {
        PixelmonEntity entity = ENTITY_CACHE.remove(playerId);
        if (entity != null && !entity.isRemoved()) {
            entity.discard();
        }
    }

    /**
     * Clear all cached entities.
     */
    public static void clearAll() {
        ENTITY_CACHE.values().forEach(entity -> {
            if (!entity.isRemoved()) {
                entity.discard();
            }
        });
        ENTITY_CACHE.clear();
    }
}
