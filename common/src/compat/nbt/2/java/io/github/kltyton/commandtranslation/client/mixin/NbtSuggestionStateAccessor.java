package io.github.kltyton.commandtranslation.client.mixin;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import net.mt1006.nbtac.autocomplete.SuggestionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Isolates hidden queries from NBT Autocomplete 2.x builder and list bookkeeping. */
@Mixin(value = SuggestionManager.class, remap = false)
public interface NbtSuggestionStateAccessor {
    @Accessor("oldBuilder") static SuggestionsBuilder commandTranslation$oldBuilder() { throw new AssertionError(); }
    @Accessor("oldBuilder") static void commandTranslation$oldBuilder(SuggestionsBuilder value) { throw new AssertionError(); }
    @Accessor("oldSuggestionList") static List<Suggestion> commandTranslation$oldList() { throw new AssertionError(); }
    @Accessor("oldSuggestionList") static void commandTranslation$oldList(List<Suggestion> value) { throw new AssertionError(); }
    @Accessor("suggestionListCounter") static int commandTranslation$counter() { throw new AssertionError(); }
    @Accessor("suggestionListCounter") static void commandTranslation$counter(int value) { throw new AssertionError(); }
}
