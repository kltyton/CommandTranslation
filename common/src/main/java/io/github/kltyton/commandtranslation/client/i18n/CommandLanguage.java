package io.github.kltyton.commandtranslation.client.i18n;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.locale.Language;

public final class CommandLanguage {
    public static final String PREFIX = "command_translation.";

    private CommandLanguage() {}

    public static List<String> aliases(String path, String literal) {
        if (path.startsWith("minecraft:")) { path = path.substring("minecraft:".length()); }
        if (literal.startsWith("minecraft:")) { literal = literal.substring("minecraft:".length()); }
        boolean nested = path.contains(".") && !path.equals(literal);
        String key = PREFIX + (path.startsWith("value.") ? path : nested ? "literal." + path : "command." + path) + ".alias";
        Language language = Language.getInstance();
        if (!language.has(key) && !path.startsWith("value.") && nested) { key = PREFIX + "literal." + literal + ".alias"; }
        List<String> result = new ArrayList<>();
        if (language.has(key)) { result.addAll(Arrays.asList(language.getOrDefault(key).split("\\|"))); }
        if (path.startsWith("gamerule.")) {
            String qualified = literal.contains(":") ? literal : "minecraft:" + literal;
            String nativeKey = language.has("gamerule." + literal) ? "gamerule." + literal
                    : "gamerule." + qualified.replace(':', '.');
            if (language.has(nativeKey)) { result.add(language.getOrDefault(nativeKey).replaceAll("\\s+", "_")); }
        }
        if (path.startsWith("playsound.") || path.startsWith("stopsound.")) {
            String nativeKey = "soundCategory." + literal;
            if (language.has(nativeKey)) { result.add(language.getOrDefault(nativeKey).replaceAll("\\s+", "_")); }
        }
        if (result.isEmpty() && path.startsWith("value.") && literal.contains(".")) {
            String[] parts = literal.split("\\.");
            for (int index = 0; index < parts.length; index++) {
                String partKey = PREFIX + "value." + parts[index] + ".alias";
                if (language.has(partKey)) { parts[index] = language.getOrDefault(partKey).split("\\|")[0]; }
            }
            result.add(String.join(".", parts));
        }
        return result.stream().filter(s -> !s.isBlank() && s.codePoints().noneMatch(Character::isWhitespace)).distinct().toList();
    }

    public static String present(String key) {
        Language language = Language.getInstance();
        return language.has(key) ? language.getOrDefault(key) : null;
    }
}
