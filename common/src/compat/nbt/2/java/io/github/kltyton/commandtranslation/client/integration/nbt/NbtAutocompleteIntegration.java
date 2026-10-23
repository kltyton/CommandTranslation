package io.github.kltyton.commandtranslation.client.integration.nbt;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.IdentityHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.mt1006.nbtac.autocomplete.SuggestionManager;
import net.mt1006.nbtac.autocomplete.suggestions.CustomSuggestion;
import io.github.kltyton.commandtranslation.client.mixin.NbtSuggestionStateAccessor;

/** Retains NBT Autocomplete 2.x metadata across localized suggestion ranges and hidden queries. */
public final class NbtAutocompleteIntegration {
    public record Metadata(CustomSuggestion.Data data, String registry) {}
    public record State(Map<Suggestion, CustomSuggestion.Data> data, boolean custom,
                        SuggestionsBuilder builder, List<Suggestion> list, int counter) {}

    private NbtAutocompleteIntegration() {}

    public static void preserve(Suggestion original, Suggestion mapped) {
        restore(snapshot(original), mapped);
    }

    public static Metadata snapshot(Suggestion suggestion) {
        return new Metadata(SuggestionManager.dataMap.get(suggestion), NbtRegistryMetadata.get(suggestion));
    }

    public static void registry(Suggestion suggestion, String registry) {
        NbtRegistryMetadata.set(suggestion, registry);
    }

    public static void restore(Metadata metadata, Suggestion suggestion) {
        registry(suggestion, metadata.registry());
        if (metadata.data() != null) { SuggestionManager.dataMap.put(suggestion, metadata.data()); }
    }

    public static State snapshot() {
        return new State(Collections.unmodifiableMap(new IdentityHashMap<>(SuggestionManager.dataMap)), SuggestionManager.hasCustomSuggestions,
                NbtSuggestionStateAccessor.commandTranslation$oldBuilder(), NbtSuggestionStateAccessor.commandTranslation$oldList(),
                NbtSuggestionStateAccessor.commandTranslation$counter());
    }

    public static void restore(State state) {
        SuggestionManager.dataMap.clear();
        SuggestionManager.dataMap.putAll(state.data());
        SuggestionManager.hasCustomSuggestions = state.custom();
        NbtSuggestionStateAccessor.commandTranslation$oldBuilder(state.builder());
        NbtSuggestionStateAccessor.commandTranslation$oldList(state.list());
        NbtSuggestionStateAccessor.commandTranslation$counter(state.counter());
    }

    public static void publish(Map<Suggestion, Metadata> metadata) {
        boolean custom = metadata.values().stream().anyMatch(value -> value.data() != null);
        if (!custom) { return; }
        SuggestionManager.hasCustomSuggestions = true;
        metadata.forEach((suggestion, value) -> {
            restore(value, suggestion);
            // A native row has no hint or priority; it must not suppress neighboring NBT hints or become an error-priority row.
            if (value.data() == null) { SuggestionManager.dataMap.put(suggestion, new CustomSuggestion.Data("", 0, false)); }
        });
    }
}
