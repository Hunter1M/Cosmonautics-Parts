package dev.devce.rocketnautics.client;

import dev.devce.rocketnautics.RocketConfig;
import dev.devce.rocketnautics.RocketNautics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.function.Consumer;

/**
 * Modern, clean Sodium-style configuration interface for Cosmonautics.
 */
@EventBusSubscriber(modid = RocketNautics.MODID, value = Dist.CLIENT)
public class RocketSettingsScreen extends Screen {

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (screen instanceof PauseScreen) {
            int targetX = screen.width / 2 + 104;
            int targetY = screen.height / 4 + 48;

            for (var child : event.getListenersList()) {
                if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget) {
                    // Match the right-column button in the 3rd row (Report Bugs / Feedback row)
                    if (widget.getX() > screen.width / 2 && widget.getY() >= screen.height / 4 + 35 && widget.getY() <= screen.height / 4 + 75) {
                        targetX = widget.getX() + widget.getWidth() + 4;
                        targetY = widget.getY();
                        break;
                    }
                }
            }
            event.addListener(new QuickConfigButton(targetX, targetY, screen));
        } else if (screen instanceof OptionsScreen) {
            int targetX = screen.width / 2 + 158;
            int targetY = screen.height / 6 + 48 - 6;

            for (var child : event.getListenersList()) {
                if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget) {
                    if (widget.getX() > screen.width / 2 && widget.getY() >= screen.height / 6 + 30 && widget.getY() <= screen.height / 6 + 60) {
                        targetX = widget.getX() + widget.getWidth() + 4;
                        targetY = widget.getY();
                        break;
                    }
                }
            }
            event.addListener(new QuickConfigButton(targetX, targetY, screen));
        }
    }

    public static class QuickConfigButton extends Button {
        public QuickConfigButton(int x, int y, Screen parent) {
            super(x, y, 20, 20, Component.empty(),
                b -> Minecraft.getInstance().setScreen(new RocketSettingsScreen(parent)),
                DEFAULT_NARRATION);
            this.setTooltip(Tooltip.create(Component.literal("Cosmonautics Parts Settings")));
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!this.visible) return;
            boolean hovered = this.isHoveredOrFocused();

            int bg = hovered ? 0xFF242E3B : 0xFF141A24;
            int border = hovered ? 0xFF38BDF8 : 0xFF28313E;

            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bg);
            graphics.renderOutline(this.getX(), this.getY(), this.width, this.height, border);

            net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(
                dev.devce.rocketnautics.registry.RocketBlocks.ROCKET_THRUSTER.asItem()
            );
            graphics.renderFakeItem(stack, this.getX() + 2, this.getY() + 2);
        }
    }

    public enum Tab {
        VISUALS("Visuals", "Engine plume & debug HUD visuals"),
        CAMERA("Camera", "Camera shake & view effects"),
        PHYSICS("Physics", "Engine thrust, fuel & server logic");

        private final String title;
        private final String desc;

        Tab(String title, String desc) {
            this.title = title;
            this.desc = desc;
        }
    }

    private final Screen lastScreen;
    private Tab activeTab = Tab.VISUALS;

    // Theme Colors
    private static final int BG_COLOR = 0xFF0F141C;
    private static final int SIDEBAR_BG = 0xFF141A24;
    private static final int HEADER_BG = 0xFF0B0F15;
    private static final int BORDER_COLOR = 0xFF222B38;
    private static final int ACCENT_BLUE = 0xFF38BDF8;
    private static final int ACCENT_GREEN = 0xFF10B981;
    private static final int CARD_BG = 0xFF18202C;
    private static final int CARD_HOVER = 0xFF222D3E;
    private static final int TEXT_PRIMARY = 0xFFF1F5F9;
    private static final int TEXT_SECONDARY = 0xFF94A3B8;

    public RocketSettingsScreen(Screen lastScreen) {
        super(Component.literal("Cosmonautics Parts Settings"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int sidebarWidth = 135;

        // Add Sidebar Tab Buttons
        int tabY = 48;
        int tabHeight = 28;
        for (Tab tab : Tab.values()) {
            boolean isSelected = (tab == activeTab);
            this.addRenderableWidget(new SidebarButton(10, tabY, sidebarWidth - 20, tabHeight, tab, isSelected, b -> {
                if (activeTab != tab) {
                    activeTab = tab;
                    this.init(this.minecraft, this.width, this.height);
                }
            }));
            tabY += 32;
        }

        // Content Area Layout
        int contentX = sidebarWidth + 24;
        int contentY = 50;
        int contentWidth = Math.min(this.width - contentX - 24, 460);

        switch (activeTab) {
            case VISUALS -> initVisualsTab(contentX, contentY, contentWidth);
            case CAMERA -> initCameraTab(contentX, contentY, contentWidth);
            case PHYSICS -> initPhysicsTab(contentX, contentY, contentWidth);
        }

        // Bottom Action Buttons
        int bottomY = this.height - 34;
        this.addRenderableWidget(new ModernButton(this.width - 110, bottomY, 90, 24,
            Component.literal("Done"),
            b -> this.minecraft.setScreen(this.lastScreen),
            ACCENT_BLUE));

        this.addRenderableWidget(new ModernButton(this.width - 210, bottomY, 90, 24,
            Component.literal("Reset Tab"),
            b -> {
                resetTabDefaults();
                this.init(this.minecraft, this.width, this.height);
            },
            0xFFEF4444));

        this.addRenderableWidget(new ModernButton(10, bottomY, 115, 24,
            Component.literal("Create Menu"),
            b -> net.createmod.catnip.gui.ScreenOpener.open(
                new net.createmod.catnip.config.ui.BaseConfigScreen(this, RocketNautics.MODID)
            ),
            0xFF818CF8));
    }

    private void initVisualsTab(int x, int y, int width) {
        SettingList list = new SettingList(x, y, width);

        list.addToggle(
            "Engine Plume Merging",
            "Clusters adjacent rocket thruster exhaust plumes into unified realistic fiery streams.",
            RocketConfig.CLIENT.enablePlumeMerging.get(),
            val -> {
                RocketConfig.CLIENT.enablePlumeMerging.set(val);
                RocketConfig.CLIENT.enablePlumeMerging.save();
            }
        );

        list.addToggle(
            "Debug Flight Overlay",
            "Displays altitude, velocity, and ship telemetry HUD on screen.",
            RocketConfig.CLIENT.showDebugOverlay.get(),
            val -> {
                RocketConfig.CLIENT.showDebugOverlay.set(val);
                RocketConfig.CLIENT.showDebugOverlay.save();
            }
        );
    }

    private void initCameraTab(int x, int y, int width) {
        SettingList list = new SettingList(x, y, width);

        list.addSlider(
            "Camera Shake Intensity",
            "Controls the strength of camera vibration near active rocket thrusters and atmospheric re-entry.",
            RocketConfig.CLIENT.shakeIntensity.get(),
            0.0, 2.0, 0.05, "",
            val -> {
                RocketConfig.CLIENT.shakeIntensity.set(val);
                RocketConfig.CLIENT.shakeIntensity.save();
            }
        );

        list.addSlider(
            "Camera Shake Radius",
            "Distance in blocks from engines where vibration is felt.",
            RocketConfig.CLIENT.shakeRadius.get(),
            4.0, 32.0, 1.0, "m",
            val -> {
                RocketConfig.CLIENT.shakeRadius.set(val);
                RocketConfig.CLIENT.shakeRadius.save();
            }
        );
    }

    private void initPhysicsTab(int x, int y, int width) {
        SettingList list = new SettingList(x, y, width);

        boolean isLocal = this.minecraft.getSingleplayerServer() != null;
        if (!isLocal) {
            list.addNotice("Server Settings are managed by the remote host.");
            return;
        }

        list.addSlider(
            "Max Engine Fuel Flow",
            "Maximum fuel consumption per engine in mB/tick.",
            RocketConfig.SERVER.maxFuelConsumption.get().doubleValue(),
            10, 200, 5.0, " mB/t",
            val -> {
                RocketConfig.SERVER.maxFuelConsumption.set(val.intValue());
                RocketConfig.SERVER.maxFuelConsumption.save();
            }
        );

        list.addSlider(
            "Ignition Fuel Flow",
            "Flow threshold for full rocket engine ignition in mB/tick.",
            RocketConfig.SERVER.ignitionFlow.get().doubleValue(),
            1, 20, 1.0, " mB/t",
            val -> {
                RocketConfig.SERVER.ignitionFlow.set(val.intValue());
                RocketConfig.SERVER.ignitionFlow.save();
            }
        );

        list.addSlider(
            "Jetpack Acceleration",
            "Base propulsion power of the survival jetpack.",
            RocketConfig.SERVER.jetpackThrust.get(),
            0.05, 0.5, 0.01, "",
            val -> {
                RocketConfig.SERVER.jetpackThrust.set(val);
                RocketConfig.SERVER.jetpackThrust.save();
            }
        );

        list.addSlider(
            "Jetpack Dampener Power",
            "Fraction of main jetpack thrust applied by inertia dampeners.",
            RocketConfig.SERVER.legThrusterThrustFactor.get(),
            0.0, 1.0, 0.05, "",
            val -> {
                RocketConfig.SERVER.legThrusterThrustFactor.set(val);
                RocketConfig.SERVER.legThrusterThrustFactor.save();
            }
        );

        list.addSlider(
            "Fuel Mass Multiplier",
            "Multiplier applied to ship fluid fuel mass (1.0 = normal, 0.0 = completely weightless fuel).",
            RocketConfig.SERVER.massMultiplier.get(),
            0.0, 5.0, 0.1, "x",
            val -> {
                RocketConfig.SERVER.massMultiplier.set(val);
                RocketConfig.SERVER.massMultiplier.save();
            }
        );

        list.addToggle(
            "Fuel Mass Enabled",
            "Enable dynamic fluid fuel mass affecting ship inertia and center of mass.",
            RocketConfig.SERVER.fuelMassEnabled.get(),
            val -> {
                RocketConfig.SERVER.fuelMassEnabled.set(val);
                RocketConfig.SERVER.fuelMassEnabled.save();
            }
        );

        list.addToggle(
            "Engine Debug Logs",
            "Outputs detailed engine performance and thrust data to game logs.",
            RocketConfig.SERVER.enableEngineDebugLogging.get(),
            val -> {
                RocketConfig.SERVER.enableEngineDebugLogging.set(val);
                RocketConfig.SERVER.enableEngineDebugLogging.save();
            }
        );
    }

    private void saveClientConfigs() {
        RocketConfig.CLIENT.enablePlumeMerging.save();
        RocketConfig.CLIENT.showDebugOverlay.save();
    }

    private void resetTabDefaults() {
        switch (activeTab) {
            case VISUALS -> {
                RocketConfig.CLIENT.enablePlumeMerging.set(true);
                RocketConfig.CLIENT.showDebugOverlay.set(false);
                saveClientConfigs();
            }
            case CAMERA -> {
                RocketConfig.CLIENT.shakeIntensity.set(0.5);
                RocketConfig.CLIENT.shakeRadius.set(8.0);
                RocketConfig.CLIENT.shakeIntensity.save();
                RocketConfig.CLIENT.shakeRadius.save();
            }
            case PHYSICS -> {
                if (this.minecraft.getSingleplayerServer() != null) {
                    RocketConfig.SERVER.maxFuelConsumption.set(40);
                    RocketConfig.SERVER.ignitionFlow.set(5);
                    RocketConfig.SERVER.jetpackThrust.set(0.1);
                    RocketConfig.SERVER.legThrusterThrustFactor.set(0.8);
                    RocketConfig.SERVER.enableEngineDebugLogging.set(false);
                    RocketConfig.SERVER.maxFuelConsumption.save();
                    RocketConfig.SERVER.ignitionFlow.save();
                    RocketConfig.SERVER.jetpackThrust.save();
                    RocketConfig.SERVER.legThrusterThrustFactor.save();
                    RocketConfig.SERVER.enableEngineDebugLogging.save();
                }
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Intentionally override and fill solid dark background to prevent vanilla blur overlay
        graphics.fill(0, 0, this.width, this.height, BG_COLOR);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 1. Solid Background
        graphics.fill(0, 0, this.width, this.height, BG_COLOR);

        // 2. Sidebar Panel
        int sidebarWidth = 135;
        graphics.fill(0, 0, sidebarWidth, this.height, SIDEBAR_BG);
        graphics.fill(sidebarWidth, 0, sidebarWidth + 1, this.height, BORDER_COLOR);

        // 3. Top Header
        graphics.fill(0, 0, this.width, 42, HEADER_BG);
        graphics.fill(0, 41, this.width, 42, BORDER_COLOR);

        // 4. Bottom Action Bar
        int bottomY = this.height - 42;
        graphics.fill(sidebarWidth + 1, bottomY, this.width, this.height, HEADER_BG);
        graphics.fill(sidebarWidth + 1, bottomY, this.width, bottomY + 1, BORDER_COLOR);

        // 5. Header Texts
        graphics.drawString(this.font, "Cosmonautics Parts", 16, 17, ACCENT_BLUE, false);
        int headerX = sidebarWidth + 24;
        graphics.drawString(this.font, activeTab.title, headerX, 10, TEXT_PRIMARY, false);
        graphics.drawString(this.font, activeTab.desc, headerX, 24, TEXT_SECONDARY, false);

        // 6. Render widgets (buttons, sliders, labels, switches)
        for (var widget : this.renderables) {
            widget.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    // ==========================================
    // UI Helpers & Modern Widgets
    // ==========================================

    private class SettingList {
        private final int startX;
        private int currentY;
        private final int width;

        public SettingList(int x, int y, int width) {
            this.startX = x;
            this.currentY = y;
            this.width = width;
        }

        public void addToggle(String name, String tooltip, boolean initial, Consumer<Boolean> onChange) {
            int rowH = 34;
            int btnW = 75;
            int btnH = 22;
            int btnX = startX + width - btnW - 8;
            int btnY = currentY + 6;

            ModernSwitch toggle = new ModernSwitch(btnX, btnY, btnW, btnH, initial, onChange);
            if (tooltip != null) {
                toggle.setTooltip(Tooltip.create(Component.literal(tooltip)));
            }

            addRenderableWidget(new SettingRowLabel(startX, currentY, width - btnW - 16, rowH, name, tooltip));
            addRenderableWidget(toggle);

            currentY += rowH + 6;
        }

        public void addCycle(String name, String tooltip, String[] options, int initialIndex, Consumer<Integer> onChange) {
            int rowH = 34;
            int btnW = 90;
            int btnH = 22;
            int btnX = startX + width - btnW - 8;
            int btnY = currentY + 6;

            final int[] index = { initialIndex };
            ModernButton cycleBtn = new ModernButton(btnX, btnY, btnW, btnH,
                Component.literal(options[index[0]]),
                b -> {
                    index[0] = (index[0] + 1) % options.length;
                    onChange.accept(index[0]);
                    b.setMessage(Component.literal(options[index[0]]));
                },
                ACCENT_BLUE);

            if (tooltip != null) {
                cycleBtn.setTooltip(Tooltip.create(Component.literal(tooltip)));
            }

            addRenderableWidget(new SettingRowLabel(startX, currentY, width - btnW - 16, rowH, name, tooltip));
            addRenderableWidget(cycleBtn);

            currentY += rowH + 6;
        }

        public void addSlider(String name, String tooltip, double current, double min, double max, double step, String suffix, Consumer<Double> onChange) {
            int rowH = 34;
            int sliderW = 120;
            int sliderH = 22;
            int sliderX = startX + width - sliderW - 8;
            int sliderY = currentY + 6;

            ModernSlider slider = new ModernSlider(sliderX, sliderY, sliderW, sliderH, current, min, max, step, suffix, onChange);
            if (tooltip != null) {
                slider.setTooltip(Tooltip.create(Component.literal(tooltip)));
            }

            addRenderableWidget(new SettingRowLabel(startX, currentY, width - sliderW - 16, rowH, name, tooltip));
            addRenderableWidget(slider);

            currentY += rowH + 6;
        }

        public void addPresetButton(String title, String description, Runnable onApply) {
            int rowH = 40;
            int btnW = 80;
            int btnH = 22;
            int btnX = startX + width - btnW - 8;
            int btnY = currentY + 9;

            addRenderableWidget(new SettingRowLabel(startX, currentY, width - btnW - 16, rowH, title, description));
            addRenderableWidget(new ModernButton(btnX, btnY, btnW, btnH,
                Component.literal("Apply"),
                b -> onApply.run(),
                ACCENT_GREEN));

            currentY += rowH + 6;
        }

        public void addNotice(String text) {
            addRenderableWidget(new SettingRowLabel(startX, currentY, width, 30, text, ""));
            currentY += 36;
        }
    }

    private static class SidebarButton extends Button {
        private final Tab tab;
        private final boolean isSelected;

        public SidebarButton(int x, int y, int width, int height, Tab tab, boolean isSelected, OnPress onPress) {
            super(x, y, width, height, Component.literal(tab.title), onPress, DEFAULT_NARRATION);
            this.tab = tab;
            this.isSelected = isSelected;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!this.visible) return;
            boolean hovered = this.isHoveredOrFocused();

            int bg = isSelected ? 0xFF222B38 : (hovered ? 0xFF1A222E : 0x00000000);
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bg);

            if (isSelected) {
                graphics.fill(this.getX(), this.getY() + 3, this.getX() + 3, this.getY() + this.height - 3, ACCENT_BLUE);
            }

            int textColor = isSelected ? ACCENT_BLUE : (hovered ? TEXT_PRIMARY : TEXT_SECONDARY);
            graphics.drawString(Minecraft.getInstance().font, tab.title, this.getX() + 10, this.getY() + 9, textColor, false);
        }
    }

    private static class SettingRowLabel extends Button {
        private final String title;
        private final String desc;

        public SettingRowLabel(int x, int y, int width, int height, String title, String desc) {
            super(x, y, width, height, Component.literal(title), b -> {}, DEFAULT_NARRATION);
            this.title = title;
            this.desc = desc;
            this.active = false;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.drawString(Minecraft.getInstance().font, title, this.getX() + 4, this.getY() + 5, TEXT_PRIMARY, false);
            if (desc != null && !desc.isEmpty()) {
                String sub = desc.length() > 65 ? desc.substring(0, 62) + "..." : desc;
                graphics.drawString(Minecraft.getInstance().font, sub, this.getX() + 4, this.getY() + 17, TEXT_SECONDARY, false);
            }
        }
    }

    private static class ModernButton extends Button {
        private final int accent;

        public ModernButton(int x, int y, int width, int height, Component message, OnPress onPress, int accent) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.accent = accent;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!this.visible) return;
            boolean hovered = this.isHoveredOrFocused();

            int bg = hovered ? CARD_HOVER : CARD_BG;
            int border = hovered ? accent : BORDER_COLOR;

            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bg);
            graphics.renderOutline(this.getX(), this.getY(), this.width, this.height, border);

            int textCol = hovered ? TEXT_PRIMARY : TEXT_SECONDARY;
            graphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
                this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textCol);
        }
    }

    private static class ModernSwitch extends Button {
        private boolean state;
        private final Consumer<Boolean> onChange;

        public ModernSwitch(int x, int y, int width, int height, boolean initial, Consumer<Boolean> onChange) {
            super(x, y, width, height, Component.literal(initial ? "ON" : "OFF"), b -> {}, DEFAULT_NARRATION);
            this.state = initial;
            this.onChange = onChange;
        }

        @Override
        public void onPress() {
            this.state = !this.state;
            this.setMessage(Component.literal(state ? "ON" : "OFF"));
            this.onChange.accept(this.state);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!this.visible) return;
            boolean hovered = this.isHoveredOrFocused();

            int bg = state ? (hovered ? 0xFF059669 : 0xFF10B981) : (hovered ? 0xFF334155 : 0xFF1E293B);
            int border = hovered ? 0xFF94A3B8 : BORDER_COLOR;

            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bg);
            graphics.renderOutline(this.getX(), this.getY(), this.width, this.height, border);

            int textCol = state ? 0xFFFFFFFF : 0xFF94A3B8;
            graphics.drawCenteredString(Minecraft.getInstance().font, state ? "ON" : "OFF",
                this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textCol);
        }
    }

    private static class ModernSlider extends AbstractSliderButton {
        private final double min;
        private final double max;
        private final double step;
        private final String suffix;
        private final Consumer<Double> onChange;

        public ModernSlider(int x, int y, int width, int height, double current, double min, double max, double step, String suffix, Consumer<Double> onChange) {
            super(x, y, width, height, Component.empty(), (current - min) / (max - min));
            this.min = min;
            this.max = max;
            this.step = step;
            this.suffix = suffix;
            this.onChange = onChange;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            double raw = min + this.value * (max - min);
            if (step >= 1.0) {
                setMessage(Component.literal((int) Math.round(raw) + suffix));
            } else {
                setMessage(Component.literal(String.format("%.2f", raw) + suffix));
            }
        }

        @Override
        protected void applyValue() {
            double raw = min + this.value * (max - min);
            double snapped = Math.round(raw / step) * step;
            this.onChange.accept(snapped);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!this.visible) return;
            boolean hovered = this.isHoveredOrFocused();

            int bg = CARD_BG;
            int border = hovered ? ACCENT_BLUE : BORDER_COLOR;

            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bg);

            int fillW = (int) (this.value * (this.width - 4));
            if (fillW > 0) {
                graphics.fill(this.getX() + 2, this.getY() + 2, this.getX() + 2 + fillW, this.getY() + this.height - 2, 0x4438BDF8);
                graphics.fill(this.getX() + fillW, this.getY() + 2, this.getX() + fillW + 3, this.getY() + this.height - 2, ACCENT_BLUE);
            }

            graphics.renderOutline(this.getX(), this.getY(), this.width, this.height, border);

            graphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
                this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, TEXT_PRIMARY);
        }
    }
}
