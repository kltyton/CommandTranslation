package io.github.kltyton.commandtranslation.client.command.suggestion;

/** Carries the native registry identity independently of visible completion hints. */
public interface RegistrySuggestion {
    String commandTranslation$registry();
    void commandTranslation$registry(String registry);
}
