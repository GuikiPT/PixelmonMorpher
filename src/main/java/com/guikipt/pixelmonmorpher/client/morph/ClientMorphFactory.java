package com.guikipt.pixelmonmorpher.client.morph;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.guikipt.pixelmonmorpher.morph.MorphData;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonFactory;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * Creates and caches Pixelmon entities for morphed players on the client.
 */
public class ClientMorphFactory {
    private static final Map<UUID, PixelmonEntity> ENTITY_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> LAST_IDLE_STATE = new ConcurrentHashMap<>();
    private static final AtomicBoolean WALK_ANIMATION_REFLECTION_FAILED = new AtomicBoolean(false);


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

                // Disable rotation smoothing to prevent jitter
                entity.setYBodyRot(player.getYRot());
                entity.yBodyRotO = player.yRotO;

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
            // Detect motion state with tolerance for network/interpolation jitter
            double playerDx = player.getX() - player.xOld;
            double playerDz = player.getZ() - player.zOld;
            double horizontalDeltaSqr = playerDx * playerDx + playerDz * playerDz;
            double velocityDeltaSqr = player.getDeltaMovement().x * player.getDeltaMovement().x
                + player.getDeltaMovement().z * player.getDeltaMovement().z;
            boolean hasMovementInput = Math.abs(player.xxa) > 0.01F || Math.abs(player.zza) > 0.01F;
            boolean hasRelevantHorizontalMotion = hasMovementInput || horizontalDeltaSqr > 0.0016D || velocityDeltaSqr > 0.0016D;
            boolean isFlying = (player.getAbilities().flying || player.isFallFlying()) && !player.onGround();

            boolean isLocalPlayer = Minecraft.getInstance().player == player;
            boolean groundedNoInput = player.onGround() && !hasMovementInput && !player.isSprinting() && !player.isSwimming();
            boolean isIdle = (isLocalPlayer && groundedNoInput)
                || (!hasRelevantHorizontalMotion && !player.isSprinting() && !player.isSwimming() && !isFlying);

            boolean wasIdle = LAST_IDLE_STATE.getOrDefault(player.getUUID(), false);

            // Set current position
            entity.setPos(player.getX(), player.getY(), player.getZ());

            // Handle position history BASED on movement state
            if (isIdle) {
                // Idle: force old position = current position (zero movement delta)
                entity.xOld = entity.getX();
                entity.yOld = entity.getY();
                entity.zOld = entity.getZ();
            } else {
                // Moving: copy player's position history for accurate movement delta
                entity.xOld = player.xOld;
                entity.yOld = player.yOld;
                entity.zOld = player.zOld;
            }

            // Copy rotations with smooth body rotation
            entity.yRotO = player.yRotO;
            entity.xRotO = player.xRotO;
            
            // Smoothly interpolate body rotation towards player rotation
            float targetBodyRot = player.getYRot();
            float currentBodyRot = entity.yBodyRot;
            float bodyRotDiff = net.minecraft.util.Mth.wrapDegrees(targetBodyRot - currentBodyRot);
            float smoothedBodyRot = currentBodyRot + bodyRotDiff * 0.3f; // 30% interpolation for smoothness
            
            entity.yBodyRotO = entity.yBodyRot; // Keep previous body rotation for interpolation
            entity.setYRot(player.getYRot());
            entity.setXRot(0); // Don't tilt up/down
            
            // Keep head rotation matching body - don't follow player's look direction
            entity.yHeadRotO = entity.yBodyRotO;
            entity.setYHeadRot(smoothedBodyRot);
            entity.setYBodyRot(smoothedBodyRot);

            // Copy pose for swim/crouch animations
            entity.setPose(Objects.requireNonNull(player.getPose()));

            // Handle movement state - Set state BEFORE tick() so animation system sees correct state
            if (isIdle) {
                // Idle: clear all movement state so entity plays IDLE animation
                entity.setDeltaMovement(0, 0, 0);
                entity.setXxa(0);
                entity.setYya(0);
                entity.setZza(0);
                entity.setSpeed(0);
                entity.setSprinting(false);
                entity.setSwimming(false);

                // Transition to idle: force-stop any lingering movement controller state
                if (!wasIdle) {
                    entity.stopInPlace();
                }
                
                // Reset walk animation distance (tells animation system we're not walking)
                entity.walkDist = 0;
                entity.walkDistO = 0;
            } else {
                // Moving: copy movement state so entity plays WALK/SWIM/FLY animation
                entity.setDeltaMovement(Objects.requireNonNull(player.getDeltaMovement()));
                entity.setXxa(player.xxa);
                entity.setYya(player.yya);
                entity.setZza(player.zza);
                entity.setSpeed(player.getSpeed());
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
            entity.setFlying(isFlying);
            if (!isFlying) {
                entity.setHoverTicks(0);
            }

            // Update tick count
            entity.tickCount = player.tickCount;

            // Always tick to keep animations running (idle, walk, swim, fly animations)
            try {
                entity.tick();
            } catch (Exception e) {
                // Silently handle errors
            }

            // Re-lock position after tick to prevent drift
            entity.setPos(player.getX(), player.getY(), player.getZ());
            
            // Re-enforce idle state after tick (in case tick modified animation state)
            if (isIdle) {
                entity.xOld = entity.getX();
                entity.yOld = entity.getY();
                entity.zOld = entity.getZ();
                entity.walkDist = 0;
                entity.walkDistO = 0;
                entity.setDeltaMovement(0, 0, 0);
                entity.setXxa(0);
                entity.setYya(0);
                entity.setZza(0);
                entity.setSpeed(0);
                entity.setSprinting(false);
                entity.setSwimming(false);
                try {
                    // Use reflection to access private walkAnimation field in LivingEntity.
                    // This is necessary because Minecraft doesn't provide a public API to control
                    // animation state, and we need to ensure the animation transitions to idle properly.
                    var walkAnimationField = net.minecraft.world.entity.LivingEntity.class.getDeclaredField("walkAnimation");
                    walkAnimationField.setAccessible(true);  // Required to access private field
                    Object walkAnimation = walkAnimationField.get(entity);
                    var setSpeedMethod = walkAnimation.getClass().getMethod("setSpeed", float.class);
                    setSpeedMethod.invoke(walkAnimation, 0.0F);
                } catch (Exception e) {
                    // Catch all exceptions since this is a best-effort operation that may fail
                    // due to obfuscation, mapping differences, or Minecraft version changes.
                    // Log the error only once to avoid spam.
                    if (WALK_ANIMATION_REFLECTION_FAILED.compareAndSet(false, true)) {
                        com.guikipt.pixelmonmorpher.PixelmonMorpher.LOGGER.warn(
                            "Failed to reset walkAnimation via reflection (this may cause animation issues): {}",
                            e.getMessage()
                        );
                    }
                }
            }

            LAST_IDLE_STATE.put(player.getUUID(), isIdle);
        }
    }

    /**
     * Clear the entity cache for a player.
     */
    public static void clearCache(UUID playerId) {
        PixelmonEntity entity = ENTITY_CACHE.remove(playerId);
        LAST_IDLE_STATE.remove(playerId);
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
        LAST_IDLE_STATE.clear();
    }
}
