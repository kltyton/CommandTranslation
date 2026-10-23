package io.github.kltyton.commandtranslation.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mt1006.nbt_ac.autocomplete.suggestions.CustomSuggestion;
import io.github.kltyton.commandtranslation.client.command.suggestion.RegistrySuggestion;
import io.github.kltyton.commandtranslation.client.integration.nbt.NbtAutocompleteIntegration;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/** Retains the old two-argument emitter and its identity-keyed hint map. */
@Mixin(value = CustomSuggestion.class, remap = false)
public abstract class NbtSuggestionRegistryMixin implements RegistrySuggestion {
    @Unique private String commandTranslation$registry;

    @Override public String commandTranslation$registry() { return commandTranslation$registry; }
    @Override public void commandTranslation$registry(String registry) { commandTranslation$registry = registry; }

    @Shadow private static Suggestion getLastAddedSuggestion(SuggestionsBuilder builder) { throw new AssertionError(); }

    @WrapMethod(method = "suggest")
    private void commandTranslation$registry(SuggestionsBuilder builder, Map<Suggestion, String> hints, Operation<Void> original) {
        Suggestion before = getLastAddedSuggestion(builder);
        original.call(builder, hints);
        Suggestion added = getLastAddedSuggestion(builder);
        if (added != null && added != before) { NbtAutocompleteIntegration.registry(added, commandTranslation$registry); }
    }
}
