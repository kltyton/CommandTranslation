package io.github.kltyton.commandtranslation.client.i18n;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class MinecraftText {
    private MinecraftText() {}

    public static MutableComponent literal(String text) { return Component.literal(text); }
    public static MutableComponent translatable(String key, Object... arguments) { return Component.translatable(key, arguments); }
    public static MutableComponent empty() { return Component.empty(); }

    public static TranslationContent translation(Component component) {
        return component.getContents() instanceof TranslatableContents text ? TranslationFallback.read(text) : null;
    }

    public static MutableComponent rebuild(TranslationContent text, Object... arguments) {
        return TranslationFallback.rebuild(text, arguments);
    }
}
