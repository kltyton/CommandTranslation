package io.github.kltyton.commandtranslation.client.i18n.export;

import java.io.FileNotFoundException;
import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

final class LanguageResources {
    private LanguageResources() {}

    static Map<String, String> read(ResourceManager resources, String locale) throws IOException {
        Map<String, String> values = new HashMap<>();
        for (String namespace : resources.getNamespaces()) {
            List<Resource> layers;
            try {
                layers = resources.getResources(new ResourceLocation(namespace, "lang/" + locale + ".json"));
            } catch (FileNotFoundException absentLanguage) {
                continue;
            }
            Closeable closeLayers = () -> {
                IOException failure = null;
                for (var resource : layers) {
                    try { resource.close(); }
                    catch (IOException closeFailure) {
                        if (failure == null) { failure = closeFailure; }
                        else { failure.addSuppressed(closeFailure); }
                    }
                }
                if (failure != null) { throw failure; }
            };
            try (closeLayers) {
                for (var resource : layers) { Language.loadFromJson(resource.getInputStream(), values::put); }
            }
        }
        return values;
    }
}
