package io.github.kltyton.commandtranslation.client.integration.nbt;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mt1006.nbt_ac.autocomplete.NbtSuggestionManager;
import java.util.Map;

final class LegacyNbtMetadata {
    private LegacyNbtMetadata() {}

    static Map<Suggestion, String> subtexts() { return NbtSuggestionManager.subtextMap; }
}
