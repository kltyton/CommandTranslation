package io.github.kltyton.commandtranslation.client.command.help;

import io.github.kltyton.commandtranslation.client.i18n.MinecraftText;
import io.github.kltyton.commandtranslation.client.i18n.CommandLanguage;
import net.minecraft.network.chat.Component;

/** Reuses the native selector names instead of inserting explanatory sentences as names. */
public final class SelectorDocumentation {
    private SelectorDocumentation() {}

    public static Component name(String selector) {
        String suffix = switch (selector) {
            case "@p" -> "nearestPlayer";
            case "@a" -> "allPlayers";
            case "@r" -> "randomPlayer";
            case "@s" -> "self";
            case "@e" -> "allEntities";
            case "@n" -> "nearestEntity";
            default -> null;
        };
        if (suffix == null) { return null; }
        String key = "argument.entity.selector." + suffix;
        return CommandLanguage.present(key) == null ? null : MinecraftText.translatable(key);
    }
}
