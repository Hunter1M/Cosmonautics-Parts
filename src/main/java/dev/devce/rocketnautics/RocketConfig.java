package dev.devce.rocketnautics;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Configuration class for RocketNautics.
 * Uses NeoForge's ModConfigSpec to define and register server and client-side settings.
 */
public class RocketConfig {
    public static final ModConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    public static final ModConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        final Pair<Server, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Server::new);
        SERVER_SPEC = specPair.getRight();
        SERVER = specPair.getLeft();

        final Pair<Client, ModConfigSpec> clientPair = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = clientPair.getRight();
        CLIENT = clientPair.getLeft();
    }

    /**
     * Server-side configuration settings.
     * These settings are synced from the server to all connected clients.
     */
    public static class Server {
        public final ModConfigSpec.IntValue maxFuelConsumption;
        public final ModConfigSpec.DoubleValue jetpackThrust;
        public final ModConfigSpec.IntValue jetpackThrustConsumption;
        public final ModConfigSpec.DoubleValue legThrusterThrustFactor;
        public final ModConfigSpec.IntValue legThrusterBaseConsumption;
        public final ModConfigSpec.IntValue ignitionFlow;
        public final ModConfigSpec.IntValue steamMinFlow;
        public final ModConfigSpec.BooleanValue enableEngineDebugLogging;
        public final ModConfigSpec.BooleanValue brokenBarrier;
        public final ModConfigSpec.BooleanValue boomEnabled;
        public final ModConfigSpec.DoubleValue sonicBoomSpeedThreshold;
        public final ModConfigSpec.DoubleValue gyrodyneStrength;

        public final ModConfigSpec.BooleanValue tankPressureEnabled;
        public final ModConfigSpec.DoubleValue maxTankPressure;
        public final ModConfigSpec.DoubleValue tankExplosionAltitude;

        public final ModConfigSpec.BooleanValue fuelMassEnabled;
        public final ModConfigSpec.DoubleValue massMultiplier;
        public final ModConfigSpec.DoubleValue fuelMassMultiplier;
        public final ModConfigSpec.DoubleValue fuelMassPerBucket;
        public final ModConfigSpec.DoubleValue lavaMassPerBucket;
        public final ModConfigSpec.DoubleValue heavyFuelMassPerBucket;
        public final ModConfigSpec.DoubleValue rocketFuelMassPerBucket;

        public Server(ModConfigSpec.Builder builder) {
            builder.push("Thrusters");
            maxFuelConsumption = builder
                    .comment("Maximum fuel consumption in mB/tick")
                    .defineInRange("maxFuelConsumption", 40, 1, 1000);
            ignitionFlow = builder
                    .comment("Flow threshold for full ignition (mB/tick)")
                    .defineInRange("ignitionFlow", 5, 1, 100);
            steamMinFlow = builder
                    .comment("Flow threshold for pre-ignition steam phase (mB/tick)")
                    .defineInRange("steamMinFlow", 2, 1, 100);
            enableEngineDebugLogging = builder
                    .comment("Enable debug logging for engine fuel and thrust (can cause spam)")
                    .define("enableEngineDebugLogging", false);
            brokenBarrier = builder
                    .comment("Allow engine thrust to exceed standard limits (up to 5000N)")
                    .define("brokenBarrier", false);
            boomEnabled = builder
                    .comment("Enable or disable sonic boom effect and physics on breaking sound barrier velocity")
                    .define("boomEnabled", true);
            sonicBoomSpeedThreshold = builder
                    .comment("Speed in blocks/second (m/s) at which a ship breaks the sound barrier and triggers a sonic boom")
                    .defineInRange("sonicBoomSpeedThreshold", 166.0, 1.0, 500.0);
            builder.pop();

            builder.push("Jetpack");
            jetpackThrust = builder
                    .comment("Acceleration of the jetpack")
                    .defineInRange("jetpackThrust", 0.1, 0.05, 0.5);
            jetpackThrustConsumption = builder
                    .comment("Maximum consumption per tick of jetpack fuel")
                    .defineInRange("jetpackThrustConsumption", 10, 1, 100);
            legThrusterThrustFactor = builder
                    .comment("Fraction of the jetpack's acceleration")
                    .defineInRange("legThrusterThrustFactor", 0.8d, 0d, 1.0d);
            legThrusterBaseConsumption = builder
                    .comment("Base consumption per tick of pressurized air while leg thrusters are active")
                    .defineInRange("legThrusterBaseConsumption", 2, 1, 100);
            builder.pop();
            gyrodyneStrength = builder
                    .comment("Torque strength of the Gyrodyne (CMG) in stabilizing and reorienting ships")
                    .defineInRange("gyrodyneStrength", 100d, 1d, 1000000d);

            builder.push("TankPressure");
            tankPressureEnabled = builder
                    .comment("Enable pressure mechanics and high-altitude overpressure explosions for Create fluid tanks")
                    .define("tankPressureEnabled", true);
            maxTankPressure = builder
                    .comment("Maximum safe pressure in bars before tanks enter critical overpressure")
                    .defineInRange("maxTankPressure", 5.0d, 1.0d, 50.0d);
            tankExplosionAltitude = builder
                    .comment("Altitude (Y coordinate) above which critical tank overpressure triggers a catastrophic explosion")
                    .defineInRange("tankExplosionAltitude", 500.0d, 0.0d, 100000.0d);
            builder.pop();

            builder.push("FuelMass");
            fuelMassEnabled = builder
                    .comment("Enable dynamic fuel mass for Create fluid tanks on ships (fluids add mass to the ship and affect physics/center of mass)")
                    .define("fuelMassEnabled", true);
            massMultiplier = builder
                    .comment("Multiplier applied to the total fuel mass on ships (1.0 = normal mass, 0.0 = completely weightless fuel). Allows players who don't want heavy fuel to disable its weight.")
                    .defineInRange("massMultiplier", 1.0d, 0.0d, 100.0d);
            fuelMassMultiplier = massMultiplier;
            fuelMassPerBucket = builder
                    .comment("Base mass units added per bucket (1000 mB) of fluid in a tank for standard fluids like water (default 1.0)")
                    .defineInRange("fuelMassPerBucket", 1.0d, 0.0d, 10.0d);
            lavaMassPerBucket = builder
                    .comment("Mass units added per bucket (1000 mB) of lava (dense molten rock, default 3.0)")
                    .defineInRange("lavaMassPerBucket", 3.0d, 0.0d, 30.0d);
            heavyFuelMassPerBucket = builder
                    .comment("Mass units added per bucket (1000 mB) of heavy fuels / diesel / napalm / crude oil / fuel (default 0.6)")
                    .defineInRange("heavyFuelMassPerBucket", 0.6d, 0.0d, 20.0d);
            rocketFuelMassPerBucket = builder
                    .comment("Mass units added per bucket (1000 mB) of rocket fuels / kerosene / gasoline / naphtha (default 0.5)")
                    .defineInRange("rocketFuelMassPerBucket", 0.5d, 0.0d, 20.0d);
            builder.pop();
        }
    }

    /**
     * Client-side configuration settings.
     * These settings are local to each player's client.
     */
    public static class Client {
        public final ModConfigSpec.DoubleValue shakeIntensity;
        public final ModConfigSpec.DoubleValue shakeRadius;
        public final ModConfigSpec.BooleanValue showDebugOverlay;
        public final ModConfigSpec.BooleanValue enablePlumeMerging;
        public final ModConfigSpec.DoubleValue plumeMergeRadius;

        public Client(ModConfigSpec.Builder builder) {
            builder.push("Visuals");
            shakeIntensity = builder
                    .comment("Intensity multiplier for camera shake near engines")
                    .defineInRange("shakeIntensity", 0.5, 0.0, 5.0);
            shakeRadius = builder
                    .comment("Radius in blocks where camera shake is felt")
                    .defineInRange("shakeRadius", 8.0, 1.0, 64.0);
            enablePlumeMerging = builder
                    .comment("Enable merging of adjacent thruster plumes into a single larger plume cluster")
                    .define("enablePlumeMerging", true);
            plumeMergeRadius = builder
                    .comment("Distance threshold in blocks within which adjacent engine plumes are clustered and merged")
                    .defineInRange("plumeMergeRadius", 4.0, 1.0, 16.0);
            showDebugOverlay = builder
                    .comment("Show the Cosmonautics debug overlay (Alt/Speed/etc)")
                    .define("showDebugOverlay", false);
            builder.pop();
        }
    }
}
