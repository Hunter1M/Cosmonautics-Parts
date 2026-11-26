package dev.devce.rocketnautics.data;

import com.tterrag.registrate.providers.ProviderType;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.data.recipe.*;
import dev.devce.rocketnautics.data.worldgen.BiomeModifiers;
import dev.devce.rocketnautics.data.worldgen.ConfiguredFeatures;
import dev.devce.rocketnautics.data.worldgen.PlacedFeatures;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.concurrent.CompletableFuture;

public class RocketDatagen {

    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        CompletableFuture<HolderLookup.Provider> registries = event.getLookupProvider();

        event.addProvider(new RocketCrushingRecipeGen(output, registries));
        event.addProvider(new RocketItemApplicationRecipeGen(output, registries));
        event.addProvider(new RocketMechanicalCraftingRecipeGen(output, registries));
        event.addProvider(new RocketMillingRecipeGen(output, registries));
        event.addProvider(new RocketMixingRecipeGen(output, registries));
        event.addProvider(new RocketPressingRecipeGen(output, registries));
        event.addProvider(new RocketStandardRecipeGen(output, registries));
        event.addProvider(new RocketWashingRecipeGen(output, registries));

        RocketNautics.getRegistrate().addDataGenerator(ProviderType.BLOCK_TAGS, RocketBlockTagsProvider::addTags);
        RocketNautics.getRegistrate().addDataGenerator(ProviderType.FLUID_TAGS, RocketFluidTagsProvider::addTags);
        
        RegistrySetBuilder registry = new RegistrySetBuilder();
        registry.add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, BiomeModifiers::bootstrap);
        registry.add(Registries.PLACED_FEATURE, PlacedFeatures::bootstrap);
        registry.add(Registries.CONFIGURED_FEATURE, ConfiguredFeatures::bootstrap);
        event.createDatapackRegistryObjects(registry);
    }
}
