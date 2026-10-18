package io.github.kltyton.commandtranslation.client.command.help;

import io.github.kltyton.commandtranslation.client.ClientPlatform;
import io.github.kltyton.commandtranslation.client.config.ClientConfig;
import io.github.kltyton.commandtranslation.client.screen.tooltip.ResourceTooltip;
import io.github.kltyton.commandtranslation.client.i18n.CommandLanguage;
import io.github.kltyton.commandtranslation.client.i18n.IdentifierNames;
import io.github.kltyton.commandtranslation.client.i18n.NamespaceNames;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/** Shares value descriptions between native command arguments and nested structured arguments. */
public final class ValueDocumentation {
    private ValueDocumentation() {}

    public static Component registry(String registry, String raw, boolean names, boolean usage) {
        if (registry == null || raw.isEmpty()) { return null; }
        Component name = names ? IdentifierNames.displayRegistry(registry, raw) : null;
        if (raw.startsWith("#")) { return ClientConfig.current().tagTooltips ? name : null; }
        String id = ClientPlatform.identifierId(raw);
        Component description = id == null || !usage ? null : DocumentationKeys.translated("registry." + registry + "." + id);
        if ("minecraft:mob_effect".equals(registry) && !ClientConfig.current().effectTooltips) { description = null; }
        Component details = usage ? ResourceTooltip.describe(registry, raw, name, description) : null;
        if (details != null) { return details; }
        if (description == null) { return name; }
        return NamespaceNames.qualify(id.substring(0, id.indexOf(':')), description);
    }

    public static String registry(String family, List<String> path, boolean key) {
        String binding = path.isEmpty() ? DocumentationKeys.present(family + ".registry")
                : DocumentationKeys.field(family, path, key ? ".key_registry" : ".registry");
        return binding == null ? null : CommandLanguage.present(binding);
    }

    public static Component value(String family, List<String> path, String raw) {
        String suffix = ".value." + raw;
        String key = path.isEmpty() ? DocumentationKeys.present(family + suffix) : DocumentationKeys.field(family, path, suffix);
        String id = ClientPlatform.identifierId(raw);
        if (key == null && id != null && !id.equals(raw)) {
            suffix = ".value." + id;
            key = path.isEmpty() ? DocumentationKeys.present(family + suffix) : DocumentationKeys.field(family, path, suffix);
        }
        if (key == null) {
            String binding = path.isEmpty() ? DocumentationKeys.present(family + ".value_type")
                    : DocumentationKeys.field(family, path, ".value_type");
            if (binding != null) { key = DocumentationKeys.present("value.type." + CommandLanguage.present(binding) + "." + raw); }
        }
        return key == null ? null : Component.translatable(key);
    }

    public static boolean specific(Component component) {
        if (component == null) { return false; }
        if (component.getContents() instanceof TranslatableContents contents) {
            if (contents.getKey().contains(".value.") && !contents.getKey().equals(CommandLanguage.PREFIX + "help.value.context")) { return true; }
            for (Object argument : contents.getArgs()) {
                if (argument instanceof Component nested && specific(nested)) { return true; }
            }
        }
        return component.getSiblings().stream().anyMatch(ValueDocumentation::specific);
    }
}
