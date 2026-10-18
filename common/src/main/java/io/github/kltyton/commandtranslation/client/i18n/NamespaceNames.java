package io.github.kltyton.commandtranslation.client.i18n;

import io.github.kltyton.commandtranslation.client.ClientPlatform;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/** Shares namespace names between localized input, suggestions and tooltips. */
public final class NamespaceNames {
    private NamespaceNames() {}

    public static Component name(String namespace) {
        for (String key : new String[]{CommandLanguage.PREFIX + "namespace." + namespace,
                "modmenu.nameTranslation." + namespace, namespace + ".ponder.mod_name"}) {
            if (CommandLanguage.present(key) != null) { return Component.translatable(key); }
        }
        return Component.literal(namespace);
    }

    public static Component identifier(String raw, Component name) {
        boolean tag = raw.startsWith("#");
        String id = ClientPlatform.identifierId(tag ? raw.substring(1) : raw);
        if (id == null) { return name; }
        String namespace = id.substring(0, id.indexOf(':'));
        return Component.empty().append(tag ? "#" : "").append(qualify(namespace, name));
    }

    public static Component qualify(String namespace, Component value) {
        return Component.translatable(CommandLanguage.PREFIX + "identifier.qualified", name(namespace), value);
    }

    public static String commandNamespace(String root) {
        int separator = root.indexOf(':');
        if (separator >= 0) { return root.substring(0, separator); }
        String declared = CommandLanguage.present(CommandLanguage.PREFIX + "command." + root + ".namespace");
        return declared == null || declared.isBlank() ? "minecraft" : declared;
    }

    public static boolean isQualified(Component value) {
        if (value.getContents() instanceof TranslatableContents text) {
            if (text.getKey().equals(CommandLanguage.PREFIX + "identifier.qualified")) { return true; }
            for (Object argument : text.getArgs()) {
                if (argument instanceof Component nested && isQualified(nested)) { return true; }
            }
        }
        return value.getSiblings().stream().anyMatch(NamespaceNames::isQualified);
    }
}
