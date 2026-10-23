package io.github.kltyton.commandtranslation.client.i18n;

import io.github.kltyton.commandtranslation.client.ClientPlatform;
import net.minecraft.network.chat.Component;

/** Resolves tag meanings without conflating equal tag IDs in different registries. */
public final class TagNames {
    private TagNames() {}

    public static Component name(String registry, String raw) {
        return IdentifierCollisions.qualify(registry, raw, nameOnly(registry, raw), id -> nameOnly(registry, id));
    }

    static Component nameOnly(String registry, String raw) {
        String id = ClientPlatform.identifierId(raw.startsWith("#") ? raw.substring(1) : raw);
        if (id == null) { return null; }
        if (registry != null) {
            String registryKey = registry.startsWith("minecraft:") ? registry.substring(10) : registry.replace(':', '.');
            String key = ClientPlatform.identifierKey("tag." + registryKey.replace('/', '.'), id);
            Component specific = translated(key);
            if (specific != null) { return NamespaceNames.identifier("#" + id, specific); }
        }
        Component legacy = translated(ClientPlatform.identifierKey("tag", id));
        return legacy == null ? null : NamespaceNames.identifier("#" + id, legacy);
    }

    private static Component translated(String key) {
        if (key == null) { return null; }
        String own = CommandLanguage.PREFIX + key;
        String value = CommandLanguage.present(own);
        if (value != null) {
            return MinecraftText.translatable(CommandLanguage.present(value) == null ? own : value);
        }
        return CommandLanguage.present(key) == null ? null : MinecraftText.translatable(key);
    }
}
