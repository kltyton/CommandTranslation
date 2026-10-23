package io.github.kltyton.commandtranslation.client.i18n;

/** Native translation arguments and the optional fallback supported by its text format. */
public record TranslationContent(String key, String fallback, Object[] args) {}
