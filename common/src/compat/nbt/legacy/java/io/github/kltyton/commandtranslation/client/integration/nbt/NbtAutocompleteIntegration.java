package io.github.kltyton.commandtranslation.client.integration.nbt;

import com.mojang.brigadier.suggestion.Suggestion;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/** Preserves identity-keyed hints in NBT Autocomplete versions preceding priority metadata. */
public final class NbtAutocompleteIntegration {
    public record Metadata(String subtext, String registry) {}
    public record State(Map<Suggestion, String> subtexts) {}

    private NbtAutocompleteIntegration() {}

    public static void preserve(Suggestion original, Suggestion mapped) { restore(snapshot(original), mapped); }

    public static Metadata snapshot(Suggestion suggestion) {
        return new Metadata(LegacyNbtMetadata.subtexts().get(suggestion), NbtRegistryMetadata.get(suggestion));
    }

    public static void registry(Suggestion suggestion, String registry) { NbtRegistryMetadata.set(suggestion, registry); }

    public static void restore(Metadata metadata, Suggestion suggestion) {
        registry(suggestion, metadata.registry());
        if (metadata.subtext() != null) { LegacyNbtMetadata.subtexts().put(suggestion, metadata.subtext()); }
    }

    public static State snapshot() {
        return new State(Collections.unmodifiableMap(new IdentityHashMap<>(LegacyNbtMetadata.subtexts())));
    }

    public static void restore(State state) {
        LegacyNbtMetadata.subtexts().clear();
        LegacyNbtMetadata.subtexts().putAll(state.subtexts());
    }

    public static void publish(Map<Suggestion, Metadata> metadata) { metadata.forEach((suggestion, value) -> restore(value, suggestion)); }
}
