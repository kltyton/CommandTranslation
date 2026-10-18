package io.github.kltyton.commandtranslation.client.command.suggestion;

import com.mojang.brigadier.suggestion.Suggestion;

/** Unwraps this project's suggestion wrappers back to the native suggestion they delegate to. */
public interface DelegatingSuggestion {
    Suggestion commandTranslation$original();

    static Suggestion unwrap(Suggestion suggestion) {
        Suggestion current = suggestion;
        while (current instanceof DelegatingSuggestion wrapper) {
            current = wrapper.commandTranslation$original();
        }
        return current;
    }
}
