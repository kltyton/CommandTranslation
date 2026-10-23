package io.github.kltyton.commandtranslation.client.integration.nbt;

import com.mojang.brigadier.suggestion.Suggestion;
import java.util.IdentityHashMap;
import java.util.Collections;
import java.util.Map;
import com.mt1006.nbt_ac.autocomplete.NbtSuggestionManager;
import com.mt1006.nbt_ac.autocomplete.suggestions.CustomSuggestion;

/** Retains NBT Autocomplete 1.3 metadata across localized suggestion ranges and hidden queries. */
public final class NbtAutocompleteIntegration {
    public record Metadata(CustomSuggestion.Data data, String registry) {}
    public record State(Map<Suggestion, CustomSuggestion.Data> data, boolean custom) {}

    private NbtAutocompleteIntegration() {}

    public static void preserve(Suggestion original, Suggestion mapped) {
        restore(snapshot(original), mapped);
    }

    public static Metadata snapshot(Suggestion suggestion) {
        return new Metadata(NbtSuggestionManager.dataMap.get(suggestion), NbtRegistryMetadata.get(suggestion));
    }

    public static void registry(Suggestion suggestion, String registry) {
        NbtRegistryMetadata.set(suggestion, registry);
    }

    public static void restore(Metadata metadata, Suggestion suggestion) {
        registry(suggestion, metadata.registry());
        if (metadata.data() != null) { NbtSuggestionManager.dataMap.put(suggestion, metadata.data()); }
    }

    public static State snapshot() {
        return new State(Collections.unmodifiableMap(new IdentityHashMap<>(NbtSuggestionManager.dataMap)), NbtSuggestionManager.hasCustomSuggestions);
    }

    public static void restore(State state) {
        NbtSuggestionManager.dataMap.clear();
        NbtSuggestionManager.dataMap.putAll(state.data());
        NbtSuggestionManager.hasCustomSuggestions = state.custom();
    }

    public static void publish(Map<Suggestion, Metadata> metadata) {
        boolean custom = metadata.values().stream().anyMatch(value -> value.data() != null);
        if (!custom) { return; }
        NbtSuggestionManager.hasCustomSuggestions = true;
        metadata.forEach((suggestion, value) -> {
            restore(value, suggestion);
            // A native row has no hint or priority; it must not suppress neighboring NBT hints or become an error-priority row.
            if (value.data() == null) { NbtSuggestionManager.dataMap.put(suggestion, new CustomSuggestion.Data("", 0, false)); }
        });
    }
}
