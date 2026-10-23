package io.github.kltyton.commandtranslation.client.integration.nbt;

import io.github.kltyton.commandtranslation.client.ClientPlatform;
import io.github.kltyton.commandtranslation.client.command.suggestion.RegistrySuggestion;

/** Registry identities declared by NBT Autocomplete's subtype schema. */
public final class NbtRegistryTypes {
    private NbtRegistryTypes() {}

    public static String registry(String subtype, String data) {
        return switch (subtype) {
            case "REGISTRY_KEY" -> data == null ? null : ClientPlatform.identifierId(data);
            case "RECIPE" -> "minecraft:recipe";
            case "LOOT_TABLE" -> "minecraft:loot_table";
            case "BANNER_PATTERN" -> "minecraft:banner_pattern";
            case "MAP_DECORATION_TYPE" -> "minecraft:map_decoration_type";
            case "POT_DECORATION" -> "minecraft:item";
            case "TRIM_PATTERN" -> "minecraft:trim_pattern";
            case "TRIM_MATERIAL" -> "minecraft:trim_material";
            case "JUKEBOX_SONG" -> "minecraft:jukebox_song";
            case "DAMAGE_TYPE_TAG" -> "minecraft:damage_type";
            default -> null;
        };
    }

    public static void attach(Iterable<?> suggestions, String registry) {
        if (registry == null) { return; }
        for (Object suggestion : suggestions) { ((RegistrySuggestion) suggestion).commandTranslation$registry(registry); }
    }
}
