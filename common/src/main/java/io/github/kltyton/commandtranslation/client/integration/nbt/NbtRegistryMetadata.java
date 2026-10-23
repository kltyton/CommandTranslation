package io.github.kltyton.commandtranslation.client.integration.nbt;

import com.google.common.collect.MapMaker;
import com.mojang.brigadier.suggestion.Suggestion;
import java.util.Map;

final class NbtRegistryMetadata {
    private static final Map<Suggestion, String> REGISTRIES = new MapMaker().weakKeys().makeMap();

    private NbtRegistryMetadata() {}

    static String get(Suggestion suggestion) { return REGISTRIES.get(suggestion); }

    static void set(Suggestion suggestion, String registry) {
        if (registry == null) { REGISTRIES.remove(suggestion); }
        else { REGISTRIES.put(suggestion, registry); }
    }
}
