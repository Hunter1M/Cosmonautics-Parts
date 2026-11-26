package dev.devce.rocketnautics.compat.computercraft;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.devce.rocketnautics.api.peripherals.PeripheralRegistry;
import dev.devce.rocketnautics.content.blocks.SputnikBlockEntity;
import dev.devce.rocketnautics.content.blocks.gyrodyne.GyrodyneBlockEntity;
import dev.devce.rocketnautics.content.blocks.gyrodyne.GyrodyneMode;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.joml.Vector3d;

public class SputnikPeripheral implements IPeripheral {
    private final SputnikBlockEntity sputnik;

    public SputnikPeripheral(SputnikBlockEntity sputnik) {
        this.sputnik = sputnik;
    }

    @NotNull
    @Override
    public String getType() {
        return "sputnik";
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof SputnikPeripheral && ((SputnikPeripheral) other).sputnik == this.sputnik;
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getBiomeInfo() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", sputnik.getBiomeName());
        data.put("color", sputnik.getBiomeColor());
        return data;
    }

    @LuaFunction(mainThread = true)
    public final String getBiomeName() {
        return sputnik.getBiomeName();
    }

    @LuaFunction(mainThread = true)
    public final int getBiomeColor() {
        return sputnik.getBiomeColor();
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Double> getGlobalPos() {
        org.joml.Vector3d pos = sputnik.getGlobalPos();
        Map<String, Double> data = new HashMap<>();
        data.put("x", pos.x);
        data.put("y", pos.y);
        data.put("z", pos.z);
        return data;
    }

    @LuaFunction(mainThread = true)
    public final String getGlobalBiomeName() {
        return sputnik.getGlobalBiomeName();
    }

    @LuaFunction(mainThread = true)
    public final int getGlobalBiomeColor() {
        return sputnik.getGlobalBiomeColor();
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getPhysics() {
        Map<String, Object> data = new HashMap<>();
        if (sputnik.getLevel() == null) return data;

        var subLevel = dev.ryanhcode.sable.Sable.HELPER.getContaining(sputnik.getLevel(), sputnik.getBlockPos());
        if (subLevel instanceof ServerSubLevel serverSubLevel) {
            data.put("mass", serverSubLevel.getMassTracker().getMass());

            var pose = serverSubLevel.logicalPose();
            var lastPose = serverSubLevel.lastPose();

            Map<String, Double> quat = new HashMap<>();
            quat.put("w", pose.orientation().w());
            quat.put("x", pose.orientation().x());
            quat.put("y", pose.orientation().y());
            quat.put("z", pose.orientation().z());
            data.put("quaternion", quat);

            Vector3d euler = pose.orientation().getEulerAnglesYXZ(new Vector3d());
            Map<String, Double> angles = new HashMap<>();
            angles.put("pitch", Math.toDegrees(euler.x));
            angles.put("yaw", Math.toDegrees(euler.y));
            angles.put("roll", Math.toDegrees(euler.z));
            data.put("euler", angles);

            Vector3d velocity = new Vector3d(pose.position()).sub(lastPose.position()).mul(20.0);
            Map<String, Double> velData = new HashMap<>();
            velData.put("x", velocity.x);
            velData.put("y", velocity.y);
            velData.put("z", velocity.z);
            data.put("velocity", velData);

            Vector3d gravity = DimensionPhysicsData.getGravity(sputnik.getLevel());
            data.put("gravityX", gravity.x());
            data.put("gravityY", gravity.y());
            data.put("gravityZ", gravity.z());
        }

        return data;
    }

    @LuaFunction(mainThread = true)
    public final List<Map<String, Object>> getGyrodynes() {
        List<Map<String, Object>> list = new ArrayList<>();
        if (sputnik.getLevel() == null) return list;

        for (dev.devce.rocketnautics.api.peripherals.IPeripheral p : PeripheralRegistry.getPeripherals(sputnik.getLevel())) {
            if (p instanceof GyrodyneBlockEntity g && !g.isRemoved()) {
                Map<String, Object> gData = new HashMap<>();
                gData.put("id", g.getPeripheralId());
                gData.put("uuid", g.getUniqueId().toString());
                gData.put("mode", g.getMode().getSerializedName());
                gData.put("active", g.isActive());
                gData.put("rotorSpeed", (double) g.getRotorSpeed());
                gData.put("x", g.getBlockPos().getX());
                gData.put("y", g.getBlockPos().getY());
                gData.put("z", g.getBlockPos().getZ());
                list.add(gData);
            }
        }
        return list;
    }

    @LuaFunction(mainThread = true)
    public final boolean setGyrodyneMode(IArguments args) throws LuaException {
        if (sputnik.getLevel() == null) return false;

        String modeName;
        Integer targetId = null;

        if (args.count() == 1) {
            modeName = args.getString(0);
        } else if (args.count() >= 2) {
            targetId = args.getInt(0);
            modeName = args.getString(1);
        } else {
            throw new LuaException("Expected: setGyrodyneMode([id], mode)");
        }

        GyrodyneMode targetMode = null;
        for (GyrodyneMode m : GyrodyneMode.values()) {
            if (m.getSerializedName().equalsIgnoreCase(modeName)) {
                targetMode = m;
                break;
            }
        }

        if (targetMode == null) {
            throw new LuaException("Invalid gyrodyne mode: " + modeName + ". Valid modes: " + String.join(", ", getAvailableGyrodyneModes()));
        }

        boolean anyUpdated = false;
        for (dev.devce.rocketnautics.api.peripherals.IPeripheral p : PeripheralRegistry.getPeripherals(sputnik.getLevel())) {
            if (p instanceof GyrodyneBlockEntity g && !g.isRemoved()) {
                if (targetId == null || g.getPeripheralId() == targetId) {
                    g.setMode(targetMode);
                    anyUpdated = true;
                }
            }
        }
        return anyUpdated;
    }

    @LuaFunction(mainThread = true)
    public final List<String> getAvailableGyrodyneModes() {
        return Arrays.stream(GyrodyneMode.values()).map(GyrodyneMode::getSerializedName).toList();
    }
}
