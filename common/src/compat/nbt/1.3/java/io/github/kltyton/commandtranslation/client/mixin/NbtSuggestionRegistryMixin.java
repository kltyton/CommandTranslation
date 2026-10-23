package io.github.kltyton.commandtranslation.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.suggestion.Suggestion;
import io.github.kltyton.commandtranslation.client.command.suggestion.RegistrySuggestion;
import io.github.kltyton.commandtranslation.client.integration.nbt.NbtAutocompleteIntegration;
import com.mt1006.nbt_ac.autocomplete.suggestions.CustomSuggestion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/** Transfers NBT Autocomplete 1.x identifier metadata independently of visible hint text. */
@Mixin(value = CustomSuggestion.class, remap = false)
public abstract class NbtSuggestionRegistryMixin implements RegistrySuggestion {
    @Unique private String commandTranslation$registry;

    @Override public String commandTranslation$registry() { return commandTranslation$registry; }
    @Override public void commandTranslation$registry(String registry) { commandTranslation$registry = registry; }

    // NBTAC already handles the loader-specific builder layout; Brigadier itself is not transformable on NeoForge.
    @Shadow private static Suggestion getLastAddedSuggestion(SuggestionsBuilder builder) { throw new AssertionError(); }

    @WrapMethod(method = "suggest")
    private void commandTranslation$registry(SuggestionsBuilder builder, Operation<Void> original) {
        Suggestion before = getLastAddedSuggestion(builder);
        original.call(builder);
        Suggestion added = getLastAddedSuggestion(builder);
        if (added != null && added != before) {
            NbtAutocompleteIntegration.registry(added, commandTranslation$registry);
        }
    }
}
