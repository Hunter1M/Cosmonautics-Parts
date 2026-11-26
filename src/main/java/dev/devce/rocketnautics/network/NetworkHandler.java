package dev.devce.rocketnautics.network;

import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.api.FreeMotionEntity;
import dev.devce.rocketnautics.content.items.JetpackItem;
import dev.devce.rocketnautics.content.items.LegThrustersItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkHandler {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(RocketNautics.MODID).versioned("1.0");

        registrar.playToClient(
            FreeMotionSetupPayload.TYPE,
            FreeMotionSetupPayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> handleFreeMotionSetup(context, payload))
        );

        registrar.playToServer(
            FreeMotionPayload.TYPE,
            FreeMotionPayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> handleFreeMotionMovement(context, payload))
        );

        registrar.playToClient(
            FreeMotionSyncPayload.TYPE,
            FreeMotionSyncPayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> handleFreeMotionClientSync(payload))
        );

        registrar.playToServer(
            JetpackTogglePayload.TYPE,
            JetpackTogglePayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> JetpackItem.toggle((ServerPlayer) context.player()))
        );

        registrar.playToServer(
            DampenersTogglePayload.TYPE,
            DampenersTogglePayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> LegThrustersItem.toggle((ServerPlayer) context.player()))
        );

        registrar.playToServer(
                SputnikNodeSyncPayload.TYPE,
                SputnikNodeSyncPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> handleSputnikSync(context.player(), payload.pos(), payload.graphData()))
        );

        registrar.playToServer(
                dev.devce.rocketnautics.content.sputnik.network.SputnikOpenRequestPayload.TYPE,
                dev.devce.rocketnautics.content.sputnik.network.SputnikOpenRequestPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> handleSputnikOpenRequest(context.player(), payload.pos()))
        );

        registrar.playToClient(
                dev.devce.rocketnautics.content.sputnik.network.SputnikOpenClientPayload.TYPE,
                dev.devce.rocketnautics.content.sputnik.network.SputnikOpenClientPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> handleSputnikOpenClient(payload))
        );

        registrar.playToServer(
                dev.devce.rocketnautics.content.sputnik.network.SputnikSaveGraphPayload.TYPE,
                dev.devce.rocketnautics.content.sputnik.network.SputnikSaveGraphPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> handleSputnikSaveGraph(context.player(), payload.pos(), payload.sputnikId(), payload.graphBytes()))
        );

        registrar.playToClient(
                PlayAudioPayload.TYPE,
                PlayAudioPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> handlePlayAudio(payload))
        );
    }

    private static void handleFreeMotionSetup(IPayloadContext context, FreeMotionSetupPayload payload) {
        if (context.player() instanceof FreeMotionEntity fme) {
            fme.set6DOFEnabled(payload.is6DOFEnabled());
            fme.setAmbulant(payload.isAmbulant());
            fme.setMovementAcceleration(payload.movementAcceleration());
            fme.setDampenerForce(payload.dampenerForce());
            if (!payload.is6DOFEnabled() && context.player() instanceof net.minecraft.world.entity.player.Player player) {
                player.setSwimming(false);
                player.setPose(net.minecraft.world.entity.Pose.STANDING);
            }
        }
    }

    private static void handleFreeMotionMovement(IPayloadContext context, FreeMotionPayload payload) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!(player instanceof FreeMotionEntity fme)) return;

        fme.setOrientation(payload.orientation());
        player.setDeltaMovement(new Vec3(payload.deltaMovement()));

        FreeMotionSyncPayload sync = new FreeMotionSyncPayload(
                player.getId(),
                fme.is6DOFEnabled(),
                fme.isAmbulant(),
                payload.orientation(),
                payload.thrustStrength()
        );

        for (ServerPlayer target : player.serverLevel().players()) {
            if (target == player) continue;
            PacketDistributor.sendToPlayer(target, sync);
        }
    }

    @net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    private static void handleFreeMotionClientSync(FreeMotionSyncPayload payload) {
        if (net.minecraft.client.Minecraft.getInstance().level == null) return;
        Entity entity = net.minecraft.client.Minecraft.getInstance().level.getEntity(payload.entityId());

        if (entity instanceof FreeMotionEntity fme) {
            fme.setOrientation(payload.orientation());
            fme.set6DOFEnabled(payload.freeMotionEnabled());
            fme.setAmbulant(payload.ambulant());
            if (!payload.freeMotionEnabled()) {
                entity.setSwimming(false);
                if (entity instanceof net.minecraft.world.entity.player.Player player) {
                    player.setPose(net.minecraft.world.entity.Pose.STANDING);
                }
            }
            dev.devce.rocketnautics.client.FreeMotionHandler.putThrustStrength(payload.entityId(), payload.thrustStrength());
        }
    }

    private static void handleSputnikSync(net.minecraft.world.entity.player.Player player, net.minecraft.core.BlockPos pos, net.minecraft.nbt.CompoundTag graphData) {
        net.minecraft.world.level.Level foundLevel = findSputnikLevel(player, pos);
        if (foundLevel != null && foundLevel.getBlockEntity(pos) instanceof dev.devce.rocketnautics.content.blocks.SputnikBlockEntity sputnik) {
            sputnik.setChanged();
            foundLevel.sendBlockUpdated(pos, sputnik.getBlockState(), sputnik.getBlockState(), 3);
        }
    }

    private static void handleSputnikOpenRequest(net.minecraft.world.entity.player.Player player, net.minecraft.core.BlockPos pos) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        net.minecraft.world.level.Level foundLevel = findSputnikLevel(player, pos);
        if (foundLevel != null && foundLevel.getBlockEntity(pos) instanceof dev.devce.rocketnautics.content.blocks.SputnikBlockEntity sputnik) {
            int spId = sputnik.getSputnikId();
            if (spId <= 0) {
                spId = dev.devce.rocketnautics.content.sputnik.storage.SputnikStorageManager.allocateId(serverPlayer.getServer());
                sputnik.setSputnikId(spId);
                sputnik.setChanged();
            }
            dev.devce.rocketnautics.content.sputnik.model.SputnikGraph graph = sputnik.getGraph();
            byte[] bytes = dev.devce.rocketnautics.content.sputnik.storage.SputnikStorageManager.serializeToGzip(graph);
            PacketDistributor.sendToPlayer(serverPlayer, new dev.devce.rocketnautics.content.sputnik.network.SputnikOpenClientPayload(pos, spId, bytes));
        }
    }

    @net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    private static void handleSputnikOpenClient(dev.devce.rocketnautics.content.sputnik.network.SputnikOpenClientPayload payload) {
        dev.devce.rocketnautics.content.sputnik.model.SputnikGraph graph =
                dev.devce.rocketnautics.content.sputnik.storage.SputnikStorageManager.deserializeFromGzip(payload.graphBytes());
        net.minecraft.client.Minecraft.getInstance().setScreen(
                new dev.devce.rocketnautics.client.ui.imgui.SputnikImGuiScreen(payload.pos(), payload.sputnikId(), graph));
    }

    private static void handleSputnikSaveGraph(net.minecraft.world.entity.player.Player player, net.minecraft.core.BlockPos pos, int sputnikId, byte[] graphBytes) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        dev.devce.rocketnautics.content.sputnik.model.SputnikGraph graph =
                dev.devce.rocketnautics.content.sputnik.storage.SputnikStorageManager.deserializeFromGzip(graphBytes);
        dev.devce.rocketnautics.content.sputnik.storage.SputnikStorageManager.saveGraph(serverPlayer.getServer(), sputnikId, graph);

        net.minecraft.world.level.Level foundLevel = findSputnikLevel(player, pos);
        if (foundLevel != null && foundLevel.getBlockEntity(pos) instanceof dev.devce.rocketnautics.content.blocks.SputnikBlockEntity sputnik) {
            sputnik.setSputnikId(sputnikId);
            sputnik.setGraph(graph);
            sputnik.setChanged();
        }
    }

    private static net.minecraft.world.level.Level findSputnikLevel(net.minecraft.world.entity.player.Player player, net.minecraft.core.BlockPos pos) {
        if (player.level().getBlockEntity(pos) instanceof dev.devce.rocketnautics.content.blocks.SputnikBlockEntity) {
            return player.level();
        }
        if (player.getServer() != null) {
            for (net.minecraft.server.level.ServerLevel serverLevel : player.getServer().getAllLevels()) {
                if (serverLevel.getBlockEntity(pos) instanceof dev.devce.rocketnautics.content.blocks.SputnikBlockEntity) {
                    return serverLevel;
                }
            }
        }
        return null;
    }

    @net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    private static void handlePlayAudio(PlayAudioPayload payload) {
        dev.devce.rocketnautics.client.ClientSynthAudio.play(payload);
    }

    public static void sendPlayAudio(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, double frequency, double endFrequency, double volume, double duration, String waveform, double attack, double decay, double sustain, double release, double dutyCycle, double fmFreq, double fmDepth, double lfoFreq, double lfoDepth, String harmonics, String formula) {
        if (level instanceof ServerLevel serverLevel) {
            PlayAudioPayload payload = new PlayAudioPayload(pos, frequency, endFrequency, volume, duration, waveform, attack, decay, sustain, release, dutyCycle, fmFreq, fmDepth, lfoFreq, lfoDepth, harmonics, formula);
            PacketDistributor.sendToPlayersNear(serverLevel, null, pos.getX(), pos.getY(), pos.getZ(), 64.0, payload);
        }
    }

}
