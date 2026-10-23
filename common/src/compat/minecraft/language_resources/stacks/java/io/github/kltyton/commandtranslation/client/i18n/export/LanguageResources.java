package io.github.kltyton.commandtranslation.client.i18n.export;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.locale.Language;
import net.minecraft.server.packs.resources.ResourceManager;

final class LanguageResources {
    private LanguageResources() {}

    static Map<String, String> read(ResourceManager resources, String locale) throws IOException {
        Map<String, String> values = new HashMap<>();
        var stacks = resources.listResourceStacks("lang", id -> id.getPath().equals("lang/" + locale + ".json"));
        for (String namespace : resources.getNamespaces()) {
            for (var entry : stacks.entrySet()) {
                if (!entry.getKey().getNamespace().equals(namespace)) { continue; }
                for (var resource : entry.getValue()) {
                    try (var stream = resource.open()) { Language.loadFromJson(stream, values::put); }
                }
            }
        }
        return values;
    }
}
