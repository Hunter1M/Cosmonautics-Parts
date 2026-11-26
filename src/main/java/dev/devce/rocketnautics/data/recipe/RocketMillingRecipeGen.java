package dev.devce.rocketnautics.data.recipe;

import com.simibubi.create.api.data.recipe.MillingRecipeGen;
import com.simibubi.create.content.kinetics.millstone.MillingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import dev.devce.rocketnautics.RocketNautics;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.ItemLike;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class RocketMillingRecipeGen extends MillingRecipeGen {
    public RocketMillingRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, RocketNautics.MODID);
    }

    @Override
    protected GeneratedRecipe create(Supplier<ItemLike> singleIngredient, UnaryOperator<StandardProcessingRecipe.Builder<MillingRecipe>> transform) {
        return super.create(RocketNautics.MODID, singleIngredient, transform);
    }
}
