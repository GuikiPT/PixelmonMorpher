package com.guikipt.pixelmonmorpher.client.screen;

import com.guikipt.pixelmonmorpher.PixelmonMorpher;
import com.guikipt.pixelmonmorpher.client.morph.ClientMorphCache;
import com.guikipt.pixelmonmorpher.morph.MorphData;
import com.guikipt.pixelmonmorpher.network.MorphRequestPacket;
import com.guikipt.pixelmonmorpher.network.NetworkHandler;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonFactory;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import java.util.*;

/**
 * GUI screen for morphing into a Pokémon with customization options
 */
public class MorphMenuScreen extends Screen {
    private static final int SCREEN_WIDTH = 400;
    private static final int SCREEN_HEIGHT = 260;

    private int guiLeft;
    private int guiTop;

    // Mouse drag tracking for preview rotation
    private boolean isDragging = false;
    private double lastMouseX = 0;
    private double lastMouseY = 0;
    private float previewRotationY = 0;
    private float previewRotationX = 0;
    private float previewZoom = 1.0f; // Zoom multiplier for preview (0.5x to 2.0x)

    // Preview animation
    private PixelmonEntity cachedPreviewEntity = null;
    private int previewTickCounter = 0;

    // Button hold tracking for continuous adjustment
    private Button heldButton = null;
    private int holdTicks = 0;
    private static final int HOLD_DELAY = 10; // Ticks before repeat starts
    private static final int HOLD_REPEAT_RATE = 3; // Ticks between repeats

    // Search and selection
    private EditBox searchBox;
    private final List<Species> allSpecies;
    private List<Species> filteredSpecies;
    private int selectedSpeciesIndex = 0;
    private int scrollOffset = 0;
    private static final int VISIBLE_SPECIES = 5;

    // Pokemon customization
    private Pokemon currentPokemon;
    private List<String> availableForms;
    private int selectedFormIndex = 0;
    private List<String> availablePalettes;
    private int selectedPaletteIndex = 0;
    private boolean isShiny = false;
    private Gender selectedGender = Gender.MALE;
    private int pokemonLevel = 50;
    private float pokemonSize = 1.0f;

    // Buttons
    private Button prevSpeciesButton;
    private Button nextSpeciesButton;
    private Button prevFormButton;
    private Button nextFormButton;
    private Button prevPaletteButton;
    private Button nextPaletteButton;
    private Button shinyToggleButton;
    private Button genderToggleButton;
    private Button levelDownButton;
    private Button levelUpButton;
    private Button sizeDownButton;
    private Button sizeUpButton;
    private Button morphButton;

    public MorphMenuScreen() {
        super(Component.literal("Morph Menu"));

        // Load all species
        allSpecies = new ArrayList<>();
        allSpecies.addAll(PixelmonSpecies.getAll());
        allSpecies.sort(Comparator.comparing(Species::getName));
        filteredSpecies = new ArrayList<>(allSpecies);

        // Check if player is already morphed and load their current morph
        var player = Minecraft.getInstance().player;
        if (player != null) {
            MorphData currentMorph = ClientMorphCache.get(player);
            if (currentMorph != null && currentMorph.isMorphed()) {
                // Load current morph settings
                isShiny = currentMorph.isShiny();
                pokemonSize = currentMorph.getSize();

                // Find and select the current species
                Species morphSpecies = PixelmonSpecies.fromNameOrDex(currentMorph.getSpeciesName()).orElse(null);
                if (morphSpecies != null) {
                    for (int i = 0; i < filteredSpecies.size(); i++) {
                        if (filteredSpecies.get(i).equals(morphSpecies)) {
                            selectedSpeciesIndex = i;
                            break;
                        }
                    }
                }
            }
        }

        // Initialize with first species (or current morph if found)
        if (!filteredSpecies.isEmpty()) {
            updateSelectedPokemon();
        }
    }

    @Override
    protected void init() {
        super.init();

        guiLeft = (this.width - SCREEN_WIDTH) / 2;
        guiTop = (this.height - SCREEN_HEIGHT) / 2;

        // Search box - wider
        searchBox = new EditBox(this.font, guiLeft + 10, guiTop + 10, 200, 20, Component.literal("Search"));
        searchBox.setHint(Component.literal("Search Pokémon..."));
        searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(searchBox);

        // Species navigation
        prevSpeciesButton = Button.builder(Component.literal("<"), btn -> scrollSpecies(-1))
            .bounds(guiLeft + 10, guiTop + 40, 20, 20)
            .build();
        this.addRenderableWidget(prevSpeciesButton);

        nextSpeciesButton = Button.builder(Component.literal(">"), btn -> scrollSpecies(1))
            .bounds(guiLeft + 190, guiTop + 40, 20, 20)
            .build();
        this.addRenderableWidget(nextSpeciesButton);

        // Form selection
        prevFormButton = Button.builder(Component.literal("<"), btn -> changeForm(-1))
            .bounds(guiLeft + 10, guiTop + 95, 20, 20)
            .build();
        this.addRenderableWidget(prevFormButton);

        nextFormButton = Button.builder(Component.literal(">"), btn -> changeForm(1))
            .bounds(guiLeft + 190, guiTop + 95, 20, 20)
            .build();
        this.addRenderableWidget(nextFormButton);

        // Palette selection
        prevPaletteButton = Button.builder(Component.literal("<"), btn -> changePalette(-1))
            .bounds(guiLeft + 10, guiTop + 125, 20, 20)
            .build();
        this.addRenderableWidget(prevPaletteButton);

        nextPaletteButton = Button.builder(Component.literal(">"), btn -> changePalette(1))
            .bounds(guiLeft + 190, guiTop + 125, 20, 20)
            .build();
        this.addRenderableWidget(nextPaletteButton);

        // Shiny toggle
        shinyToggleButton = Button.builder(Component.literal("Shiny: No"), btn -> toggleShiny())
            .bounds(guiLeft + 10, guiTop + 155, 90, 20)
            .build();
        this.addRenderableWidget(shinyToggleButton);

        // Gender toggle
        genderToggleButton = Button.builder(Component.literal("Gender: ♂"), btn -> toggleGender())
            .bounds(guiLeft + 110, guiTop + 155, 100, 20)
            .build();
        this.addRenderableWidget(genderToggleButton);

        // Level control
        levelDownButton = Button.builder(Component.literal("-"), btn -> changeLevel(-1))
                .bounds(guiLeft + 10, guiTop + 185, 20, 20)
                .build();
        this.addRenderableWidget(levelDownButton);

        levelUpButton = Button.builder(Component.literal("+"), btn -> changeLevel(1))
                .bounds(guiLeft + 190, guiTop + 185, 20, 20)
                .build();
        this.addRenderableWidget(levelUpButton);

        // Size control
        sizeDownButton = Button.builder(Component.literal("-"), btn -> changeSize(-0.1f))
                .bounds(guiLeft + 10, guiTop + 215, 20, 20)
                .build();
        this.addRenderableWidget(sizeDownButton);

        sizeUpButton = Button.builder(Component.literal("+"), btn -> changeSize(0.1f))
                .bounds(guiLeft + 190, guiTop + 215, 20, 20)
                .build();
        this.addRenderableWidget(sizeUpButton);

        // Action buttons
        morphButton = Button.builder(Component.literal("Morph"), btn -> applyMorph())
            .bounds(guiLeft + 220, guiTop + 230, 80, 20)
            .build();
        this.addRenderableWidget(morphButton);

        Button cancelButton = Button.builder(Component.literal("Cancel"), btn -> this.onClose())
                .bounds(guiLeft + 310, guiTop + 230, 80, 20)
                .build();
        this.addRenderableWidget(cancelButton);

        updateButtonStates();
    }

    private void onSearchChanged(String search) {
        String lowerSearch = search.toLowerCase().trim();
        if (lowerSearch.isEmpty()) {
            filteredSpecies = new ArrayList<>(allSpecies);
        } else {
            // First, try to find species that start with the search term
            List<Species> startsWithMatches = allSpecies.stream()
                .filter(s -> s.getName().toLowerCase().startsWith(lowerSearch))
                .toList();

            // Then add species that contain the search term but don't start with it
            List<Species> containsMatches = allSpecies.stream()
                .filter(s -> !s.getName().toLowerCase().startsWith(lowerSearch)
                          && s.getName().toLowerCase().contains(lowerSearch))
                .toList();

            filteredSpecies = new ArrayList<>();
            filteredSpecies.addAll(startsWithMatches);
            filteredSpecies.addAll(containsMatches);
        }

        selectedSpeciesIndex = 0;
        scrollOffset = 0;
        updateSelectedPokemon();
        updateButtonStates();
    }

    private void scrollSpecies(int direction) {
        selectedSpeciesIndex += direction;
        if (selectedSpeciesIndex < 0) selectedSpeciesIndex = 0;
        if (selectedSpeciesIndex >= filteredSpecies.size()) selectedSpeciesIndex = filteredSpecies.size() - 1;

        // Update scroll to keep selection visible
        if (selectedSpeciesIndex < scrollOffset) {
            scrollOffset = selectedSpeciesIndex;
        } else if (selectedSpeciesIndex >= scrollOffset + VISIBLE_SPECIES) {
            scrollOffset = selectedSpeciesIndex - VISIBLE_SPECIES + 1;
        }

        updateSelectedPokemon();
        updateButtonStates();
    }

    private void updateSelectedPokemon() {
        if (filteredSpecies.isEmpty()) {
            currentPokemon = null;
            availableForms = new ArrayList<>();
            availablePalettes = new ArrayList<>();
            cachedPreviewEntity = null; // Clear cache
            return;
        }

        Species species = filteredSpecies.get(selectedSpeciesIndex);
        currentPokemon = PokemonFactory.create(species);
        currentPokemon.setShiny(isShiny);
        currentPokemon.setGender(selectedGender);
        currentPokemon.setLevel(pokemonLevel);

        // Clear cached entity when Pokémon changes
        cachedPreviewEntity = null;
        previewTickCounter = 0;

        // Get available forms
        availableForms = new ArrayList<>();
        availableForms.add("base");
        species.getForms().forEach(form -> {
            if (!form.getName().equalsIgnoreCase("base")) {
                availableForms.add(form.getName());
            }
        });
        selectedFormIndex = 0;

        // Get available palettes
        availablePalettes = new ArrayList<>();
        try {
            var palettes = currentPokemon.getPalette();
            if (palettes != null) {
                availablePalettes.add(palettes.getName());
            }
        } catch (Exception e) {
            availablePalettes.add("none");
        }
        selectedPaletteIndex = 0;
    }

    private void changeForm(int direction) {
        if (availableForms.isEmpty()) return;

        selectedFormIndex += direction;
        if (selectedFormIndex < 0) selectedFormIndex = availableForms.size() - 1;
        if (selectedFormIndex >= availableForms.size()) selectedFormIndex = 0;

        // Apply form to pokemon
        String formName = availableForms.get(selectedFormIndex);
        if (!formName.equals("base") && currentPokemon != null) {
            try {
                var form = currentPokemon.getSpecies().getForm(formName);
                if (form != null) {
                    currentPokemon.setForm(form);
                    // Clear cached entity so it updates with the new form
                    cachedPreviewEntity = null;
                    com.guikipt.pixelmonmorpher.PixelmonMorpher.LOGGER.info("Form changed to: {}", formName);
                }
            } catch (Exception e) {
                com.guikipt.pixelmonmorpher.PixelmonMorpher.LOGGER.error("Error applying form: {}", formName, e);
                // Keep current form
            }
        } else if (formName.equals("base") && currentPokemon != null) {
            // Reset to base form
            currentPokemon.setForm(currentPokemon.getSpecies().getDefaultForm());
            cachedPreviewEntity = null;
            com.guikipt.pixelmonmorpher.PixelmonMorpher.LOGGER.info("Form changed to: base");
        }
    }

    private void changePalette(int direction) {
        if (availablePalettes.isEmpty()) return;

        selectedPaletteIndex += direction;
        if (selectedPaletteIndex < 0) selectedPaletteIndex = availablePalettes.size() - 1;
        if (selectedPaletteIndex >= availablePalettes.size()) selectedPaletteIndex = 0;
    }

    private void toggleShiny() {
        isShiny = !isShiny;
        if (currentPokemon != null) {
            currentPokemon.setShiny(isShiny);
            // Recreate to ensure shiny texture is loaded
            updateSelectedPokemon();
        }
        shinyToggleButton.setMessage(Component.literal("Shiny: " + (isShiny ? "Yes" : "No")));
    }

    private void toggleGender() {
        selectedGender = (selectedGender == Gender.MALE) ? Gender.FEMALE : Gender.MALE;
        genderToggleButton.setMessage(Component.literal("Gender: " + (selectedGender == Gender.MALE ? "♂" : "♀")));

        // Recreate the Pokémon completely to apply gender-specific forms (e.g., Pikachu tail)
        if (currentPokemon != null) {
            // Store current selections
            Species currentSpecies = currentPokemon.getSpecies();
            int currentFormIndex = selectedFormIndex;

            // Recreate with new gender
            currentPokemon = PokemonFactory.create(currentSpecies);
            currentPokemon.setGender(selectedGender);
            currentPokemon.setShiny(isShiny);
            currentPokemon.setLevel(pokemonLevel);

            // Reapply form if not base
            if (currentFormIndex > 0 && currentFormIndex < availableForms.size()) {
                String formName = availableForms.get(currentFormIndex);
                if (!formName.equals("base")) {
                    try {
                        var form = currentSpecies.getForm(formName);
                        if (form != null) {
                            currentPokemon.setForm(form);
                        }
                    } catch (Exception e) {
                        // Keep base form
                    }
                }
            }

            // Clear preview cache to force recreation
            cachedPreviewEntity = null;
            previewTickCounter = 0;
        }
    }

    private void changeLevel(int delta) {
        pokemonLevel += delta;
        if (pokemonLevel < 1) pokemonLevel = 1;
        if (pokemonLevel > 100) pokemonLevel = 100;

        if (currentPokemon != null) {
            currentPokemon.setLevel(pokemonLevel);
        }
    }

    private void changeSize(float delta) {
        pokemonSize += delta;
        if (pokemonSize < 0.1f) pokemonSize = 0.1f;
        if (pokemonSize > 5.0f) pokemonSize = 5.0f;
    }

    private void updateButtonStates() {
        prevSpeciesButton.active = selectedSpeciesIndex > 0;
        nextSpeciesButton.active = selectedSpeciesIndex < filteredSpecies.size() - 1;

        prevFormButton.active = availableForms.size() > 1;
        nextFormButton.active = availableForms.size() > 1;

        prevPaletteButton.active = availablePalettes.size() > 1;
        nextPaletteButton.active = availablePalettes.size() > 1;

        morphButton.active = currentPokemon != null;
    }

    private void applyMorph() {
        if (currentPokemon == null) return;

        // Send morph request to server
        NetworkHandler.sendToServer(new MorphRequestPacket(
            currentPokemon.getSpecies().getName(),
            availableForms.get(selectedFormIndex),
            isShiny,
            availablePalettes.isEmpty() ? "none" : availablePalettes.get(selectedPaletteIndex),
            pokemonSize,
            selectedGender,
            pokemonLevel
        ));

        this.onClose();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Draw simple transparent overlay instead of blur
        graphics.fill(0, 0, this.width, this.height, 0x80000000);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Background
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        // Draw main panel
        graphics.fill(guiLeft, guiTop, guiLeft + SCREEN_WIDTH, guiTop + SCREEN_HEIGHT, 0xCC000000);
        graphics.fill(guiLeft + 1, guiTop + 1, guiLeft + SCREEN_WIDTH - 1, guiTop + SCREEN_HEIGHT - 1, 0xFF2B2B2B);

        // Draw Pokemon preview area - larger box
        int previewLeft = guiLeft + 220;
        int previewTop = guiTop + 10;
        int previewRight = guiLeft + 390;
        int previewBottom = guiTop + 220;
        graphics.fill(previewLeft, previewTop, previewRight, previewBottom, 0xFF1A1A1A);

        // Draw labels - aligned vertically with buttons
        graphics.drawString(this.font, "Pokémon:", guiLeft + 35, guiTop + 45, 0xFFFFFF);
        graphics.drawString(this.font, getCurrentSpeciesName(), guiLeft + 35, guiTop + 57, 0xFFFF55);

        graphics.drawString(this.font, "Form:", guiLeft + 35, guiTop + 100, 0xFFFFFF);
        if (!availableForms.isEmpty()) {
            graphics.drawString(this.font, availableForms.get(selectedFormIndex), guiLeft + 35, guiTop + 112, 0xFFFF55);
        }

        graphics.drawString(this.font, "Palette:", guiLeft + 35, guiTop + 130, 0xFFFFFF);
        if (!availablePalettes.isEmpty()) {
            graphics.drawString(this.font, availablePalettes.get(selectedPaletteIndex), guiLeft + 35, guiTop + 142, 0xFFFF55);
        }

        // Center level text vertically with button
        graphics.drawString(this.font, "Level: " + pokemonLevel, guiLeft + 35, guiTop + 190, 0xFFFFFF);

        // Center size text vertically with button
        graphics.drawString(this.font, String.format("Size: %.1fx", pokemonSize), guiLeft + 35, guiTop + 220, 0xFFFFFF);

        // Render Pokemon preview - centered in preview box
        if (currentPokemon != null) {
            int previewCenterX = (previewLeft + previewRight) / 2;
            int previewCenterY = previewTop + (previewBottom - previewTop) / 2;

            // Enable scissor test to clip rendering to preview area
            graphics.enableScissor(previewLeft, previewTop, previewRight, previewBottom);
            renderPokemonPreview(graphics, previewCenterX, previewCenterY);
            graphics.disableScissor();
        }

        // Render widgets
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private String getCurrentSpeciesName() {
        if (filteredSpecies.isEmpty()) return "None";
        return filteredSpecies.get(selectedSpeciesIndex).getName();
    }

    private void renderPokemonPreview(GuiGraphics graphics, int x, int y) {
        if (this.minecraft == null || this.minecraft.level == null || currentPokemon == null) return;

        try {
            // Get or create cached entity
            PixelmonEntity entity = currentPokemon.getOrCreatePixelmon(this.minecraft.player);

            if (entity != null) {
                // Check if we need to refresh cache (species, gender, shiny, or form changed)
                boolean needsRefresh = cachedPreviewEntity == null
                    || cachedPreviewEntity.getSpecies() != entity.getSpecies()
                    || cachedPreviewEntity.getPokemon().getGender() != currentPokemon.getGender()
                    || cachedPreviewEntity.getPokemon().isShiny() != currentPokemon.isShiny()
                    || !cachedPreviewEntity.getPokemon().getForm().equals(currentPokemon.getForm());

                if (needsRefresh) {
                    cachedPreviewEntity = entity;
                    previewTickCounter = 0;
                }

                // Tick the entity to advance animations (idle animation)
                if (cachedPreviewEntity != null) {
                    // Slow down animation by only ticking every 4 frames (was 2, still too fast)
                    previewTickCounter++;
                    if (previewTickCounter % 4 == 0) {
                        cachedPreviewEntity.tickCount = previewTickCounter / 4;

                        // Set entity to idle state for proper idle animation
                        cachedPreviewEntity.setDeltaMovement(0, 0, 0);
                        cachedPreviewEntity.setOnGround(true);
                        cachedPreviewEntity.setXxa(0);
                        cachedPreviewEntity.setYya(0);
                        cachedPreviewEntity.setZza(0);
                        cachedPreviewEntity.setSpeed(0);
                        cachedPreviewEntity.setSprinting(false);
                        cachedPreviewEntity.setSwimming(false);
                        cachedPreviewEntity.setFlying(false);

                        // Tick to advance animations
                        try {
                            cachedPreviewEntity.tick();
                        } catch (Exception e) {
                            // Some entities may fail to tick in preview context, ignore
                        }
                    }
                }

                // Calculate base scale to fit in preview area WITHOUT size multiplier
                float baseDimension = Math.max(entity.getBbWidth(), entity.getBbHeight());
                float baseScale = Math.min(40.0f / baseDimension, 40.0f);

                // Apply zoom multiplier
                float scale = baseScale * previewZoom;

                renderEntity(graphics, x, y, scale, cachedPreviewEntity != null ? cachedPreviewEntity : entity);
            } else {
                graphics.drawString(this.font, "Preview", x - 20, y - 10, 0xFFFFFF);
                graphics.drawString(this.font, "Unavailable", x - 30, y + 5, 0xFFFFFF);
            }
        } catch (Exception e) {
            // Draw fallback text if entity can't be rendered
            graphics.drawString(this.font, "Preview", x - 20, y - 10, 0xFFFFFF);
            graphics.drawString(this.font, "Unavailable", x - 30, y + 5, 0xFFFFFF);
        }
    }

    private void renderEntity(GuiGraphics graphics, int x, int y, float scale, LivingEntity entity) {
        if (this.minecraft == null) return;

        try {
            graphics.pose().pushPose();

            // Adjust Y position to move model lower and use bottom space better
            // Most Pokémon models are bottom-aligned, so we shift down by 60% to use more of the bottom space
            float yOffset = entity.getBbHeight() * scale * 0.6f;
            graphics.pose().translate(x, y + yOffset, 50);
            graphics.pose().scale(scale, scale, -scale); // Negative Z to flip correctly

            // Rotation based on mouse drag (stored in class fields)
            Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI); // Flip to face forward
            Quaternionf rotationX = new Quaternionf().rotateX(previewRotationX * ((float) Math.PI / 180F));
            rotation.mul(rotationX);
            graphics.pose().mulPose(rotation);

            Quaternionf rotationY = new Quaternionf().rotateY((180 + previewRotationY) * ((float) Math.PI / 180F)); // 180 to face forward
            graphics.pose().mulPose(rotationY);

            EntityRenderDispatcher dispatcher = this.minecraft.getEntityRenderDispatcher();
            rotationY.conjugate();
            dispatcher.overrideCameraOrientation(rotationY);
            dispatcher.setRenderShadow(false);

            // Render the entity
            var bufferSource = this.minecraft.renderBuffers().bufferSource();
            dispatcher.render(entity, 0, 0, 0, 0.0F, 1.0F, graphics.pose(), bufferSource, 15728880);

            bufferSource.endBatch();
            dispatcher.setRenderShadow(true);

            graphics.pose().popPose();
        } catch (Exception e) {
            graphics.pose().popPose();
            // Failed to render - draw error text
            graphics.drawString(this.font, "Render Error", x - 30, y, 0xFF0000);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Check if mouse is in preview area for zoom control
        int previewLeft = guiLeft + 220;
        int previewTop = guiTop + 10;
        int previewRight = guiLeft + 390;
        int previewBottom = guiTop + 220;

        if (mouseX >= previewLeft && mouseX <= previewRight && mouseY >= previewTop && mouseY <= previewBottom) {
            if (scrollY != 0) {
                // Adjust zoom
                float zoomDelta = (float) scrollY * 0.1f;
                previewZoom += zoomDelta;

                // Clamp zoom to reasonable values to prevent model from leaving preview zone
                // Min zoom: 0.3x (can see full model even if large)
                // Max zoom: 2.5x (can zoom in on details without going too far)
                previewZoom = Math.max(0.3f, Math.min(2.5f, previewZoom));

                return true;
            }
        }

        // If not in preview area, handle species scrolling
        if (scrollY != 0) {
            scrollSpecies((int) -Math.signum(scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void tick() {
        super.tick();

        // Handle button hold for continuous adjustment
        if (heldButton != null && heldButton.active) {
            holdTicks++;

            if (holdTicks > HOLD_DELAY && (holdTicks - HOLD_DELAY) % HOLD_REPEAT_RATE == 0) {
                // Trigger button action repeatedly
                heldButton.onPress();
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Track button holds for level/size adjustment
        if (button == 0) {
            // Check if clicking on level or size buttons
            if (levelDownButton.isMouseOver(mouseX, mouseY) && levelDownButton.active) {
                heldButton = levelDownButton;
                holdTicks = 0;
            } else if (levelUpButton.isMouseOver(mouseX, mouseY) && levelUpButton.active) {
                heldButton = levelUpButton;
                holdTicks = 0;
            } else if (sizeDownButton.isMouseOver(mouseX, mouseY) && sizeDownButton.active) {
                heldButton = sizeDownButton;
                holdTicks = 0;
            } else if (sizeUpButton.isMouseOver(mouseX, mouseY) && sizeUpButton.active) {
                heldButton = sizeUpButton;
                holdTicks = 0;
            }
        }

        // Check if click is in preview area
        int previewLeft = guiLeft + 220;
        int previewTop = guiTop + 10;
        int previewRight = guiLeft + 390;
        int previewBottom = guiTop + 220;

        if (mouseX >= previewLeft && mouseX <= previewRight && mouseY >= previewTop && mouseY <= previewBottom) {
            if (button == 0) { // Left click
                isDragging = true;
                lastMouseX = mouseX;
                lastMouseY = mouseY;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDragging = false;
            heldButton = null; // Stop button hold
            holdTicks = 0;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging && button == 0) {
            // Update rotation based on drag
            previewRotationY += (float) (mouseX - lastMouseX);
            previewRotationX += (float) (mouseY - lastMouseY) * 0.5f; // Reduce X rotation sensitivity

            // Clamp X rotation to prevent flipping upside down
            previewRotationX = Math.max(-80, Math.min(80, previewRotationX));

            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Tab key for cycling through filtered Pokémon
        if (keyCode == 258) { // Tab key
            if (searchBox.isFocused() && !filteredSpecies.isEmpty()) {
                // Cycle to next matching Pokémon
                scrollSpecies(1);

                // Update search box with current selection
                if (!filteredSpecies.isEmpty()) {
                    String selectedName = filteredSpecies.get(selectedSpeciesIndex).getName();
                    searchBox.setValue(selectedName);
                    searchBox.moveCursorToEnd(false);
                }
                return true;
            }
        }

        // Arrow keys for navigation when search box is focused
        if (searchBox.isFocused()) {
            if (keyCode == 265) { // Up arrow
                scrollSpecies(-1);
                return true;
            } else if (keyCode == 264) { // Down arrow
                scrollSpecies(1);
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
