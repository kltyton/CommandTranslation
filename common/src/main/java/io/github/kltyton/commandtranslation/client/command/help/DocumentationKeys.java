package io.github.kltyton.commandtranslation.client.command.help;

import io.github.kltyton.commandtranslation.client.i18n.CommandLanguage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

/** Resolves exact context keys before single-member and trailing-path wildcard keys. */
public final class DocumentationKeys {
    private DocumentationKeys() {}

    public static String present(String suffix) {
        return present("help.", suffix);
    }

    public static String presentName(String suffix) {
        return present("name.", suffix);
    }

    private static String present(String domain, String suffix) {
        String key = CommandLanguage.PREFIX + domain + suffix;
        return CommandLanguage.present(key) == null ? null : key;
    }

    public static Component translated(String suffix) {
        String key = present(suffix);
        return key == null ? null : Component.translatable(key);
    }

    public static String field(String family, List<String> path, String suffix) {
        return field("help.", family, path, suffix);
    }

    public static String fieldName(String family, List<String> path, String suffix) {
        return field("name.", family, path, suffix);
    }

    private static String field(String domain, String family, List<String> path, String suffix) {
        String prefix = family + ".field.";
        String exact = present(domain, prefix + String.join(".", path) + suffix);
        if (exact != null) { return exact; }
        List<String> pattern = new ArrayList<>(path);
        for (int i = pattern.size() - 1; i >= 0; i--) {
            String part = pattern.set(i, "*");
            String key = present(domain, prefix + String.join(".", pattern) + suffix);
            if (key != null) { return key; }
            pattern.set(i, part);
        }
        for (int i = pattern.size() - 1; i >= 0; i--) {
            pattern.set(i, "*");
            String key = present(domain, prefix + String.join(".", pattern) + suffix);
            if (key != null) { return key; }
        }
        return null;
    }
}
