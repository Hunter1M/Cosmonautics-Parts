package dev.devce.rocketnautics.client;

import dev.devce.rocketnautics.RocketNauticsClient;
import dev.devce.rocketnautics.content.items.JetpackItem;
import dev.devce.rocketnautics.content.items.LegThrustersItem;
import dev.devce.rocketnautics.network.DampenersTogglePayload;
import dev.devce.rocketnautics.network.JetpackTogglePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Event subscriber for game-bus client-side events.
 * Manages camera shake, jetpack/dampener input, and ImGui editor rendering.
 */
public class RocketNauticsClientEvents {

    /**
     * Handles jetpack input processing and camera shake every tick.
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        MFDAudioEngine.tick();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (mc.screen instanceof net.minecraft.client.gui.screens.ReceivingLevelScreen) return;

        dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientLevelRopeManager ropeManager =
                dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientLevelRopeManager.getOrCreate(mc.level);
        if (ropeManager != null) {
            ropeManager.tickInterpolation((double) mc.level.getGameTime());
        }

        while (RocketNauticsClient.JETPACK_TOGGLE.consumeClick()) {
            if (mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof JetpackItem) {
                PacketDistributor.sendToServer(new JetpackTogglePayload());
            }
        }

        while (RocketNauticsClient.DAMPENERS_TOGGLE.consumeClick()) {
            if (mc.player.getItemBySlot(EquipmentSlot.LEGS).getItem() instanceof LegThrustersItem) {
                PacketDistributor.sendToServer(new DampenersTogglePayload());
            }
        }

        CameraShakeHandler.tick();
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float[] angles = new float[]{event.getPitch(), event.getYaw(), event.getRoll()};
        CameraShakeHandler.applyShake((float)event.getPartialTick(), angles);
        event.setPitch(angles[0]);
        event.setYaw(angles[1]);
        event.setRoll(angles[2]);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        RocketNauticsClient.onRenderLevelStage(event);
    }

    @SubscribeEvent
    public static void onRenderGui(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
        if (dev.devce.rocketnautics.client.ui.imgui.ImGuiManager.hasInstance()) {
            dev.devce.rocketnautics.client.ui.imgui.ImGuiManager.getInstance().render();
        }
    }

    @SubscribeEvent
    public static void onKeyInput(net.neoforged.neoforge.client.event.InputEvent.Key event) {
        if (event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS && event.getKey() == org.lwjgl.glfw.GLFW.GLFW_KEY_F8) {
            dev.devce.rocketnautics.client.ui.imgui.ImGuiManager.getInstance().toggle();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (dev.devce.rocketnautics.client.ui.imgui.ImGuiManager.hasInstance() && dev.devce.rocketnautics.client.ui.imgui.ImGuiManager.getInstance().isOpen() && mc.getWindow() != null) {
            dev.devce.rocketnautics.client.ui.imgui.ImGuiManager.getInstance().onKey(
                    mc.getWindow().getWindow(), event.getKey(), event.getScanCode(), event.getAction(), event.getModifiers());
        }
    }

    @SubscribeEvent
    public static void onClientLoggingOut(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        MFDAudioEngine.stopAll();
        if (dev.devce.rocketnautics.client.ui.imgui.ImGuiManager.hasInstance()) {
            dev.devce.rocketnautics.client.ui.imgui.ImGuiManager.getInstance().setOpen(false);
        }
    }
}
