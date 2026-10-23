package io.github.kltyton.commandtranslation.client.i18n;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

final class TranslationFallback {
    private TranslationFallback() {}

    static TranslationContent read(TranslatableContents text) {
        return new TranslationContent(text.getKey(), text.getFallback(), text.getArgs());
    }

    static MutableComponent rebuild(TranslationContent text, Object[] arguments) {
        return Component.translatableWithFallback(text.key(), text.fallback(), arguments);
    }
}
