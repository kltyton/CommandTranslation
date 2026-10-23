package io.github.kltyton.commandtranslation.client.i18n;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

public final class MinecraftText {
    private MinecraftText() {}

    public static MutableComponent literal(String text) { return new TextComponent(text); }
    public static MutableComponent translatable(String key, Object... arguments) { return new TranslatableComponent(key, arguments); }
    public static MutableComponent empty() { return new TextComponent(""); }

    public static TranslationContent translation(Component component) {
        return component instanceof TranslatableComponent text
                ? new TranslationContent(text.getKey(), null, text.getArgs()) : null;
    }

    public static MutableComponent rebuild(TranslationContent text, Object... arguments) {
        return new TranslatableComponent(text.key(), arguments);
    }
}
