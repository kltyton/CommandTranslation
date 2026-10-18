package io.github.kltyton.commandtranslation.client.screen.tooltip;

import io.github.kltyton.commandtranslation.client.ClientPlatform;
import io.github.kltyton.commandtranslation.client.config.ClientConfig;
import io.github.kltyton.commandtranslation.client.i18n.IdentifierNames;
import io.github.kltyton.commandtranslation.client.i18n.NamespaceNames;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.EntityType;

/** Reads native tooltip lines and registered defaults without creating world entities. */
public final class ResourceTooltip {
    private static final String ITEM = "command_translation.help.panel.item";
    private ResourceTooltip() {}

    public static Component describe(String registry, String raw, Component name, Component description) {
        var settings = ClientConfig.current();
        String id = ClientPlatform.identifierId(raw);
        if (id == null) { return null; }
        if ("minecraft:item".equals(registry) && settings.itemTooltips) {
            Component title = name != null ? name : IdentifierNames.displayRegistry(registry, id);
            return title == null ? null : Component.translatable(ITEM, title,
                    description == null ? Component.empty() : description, raw);
        }
        if ("minecraft:entity_type".equals(registry) && settings.entityTooltips) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.stream()
                    .filter(value -> BuiltInRegistries.ENTITY_TYPE.getKey(value).toString().equals(id)).findFirst().orElse(null);
            if (type == null) { return null; }
            Component title = name != null ? name : IdentifierNames.displayRegistry(registry, id);
            if (title == null) { return null; }
            return EntityAttributeTooltips.describe(type, title, description);
        }
        if ("minecraft:mob_effect".equals(registry) && settings.effectTooltips && description != null) {
            Component title = name != null ? name : IdentifierNames.displayRegistry(registry, id);
            return title == null ? null : title.copy().append("\n").append(description);
        }
        return null;
    }

    public static Component resolve(Component component) {
        if (!(component.getContents() instanceof TranslatableContents contents) || !contents.getKey().equals(ITEM)
                || contents.getArgs().length != 3 || !(contents.getArgs()[0] instanceof Component title)
                || !(contents.getArgs()[1] instanceof Component description) || !(contents.getArgs()[2] instanceof String raw)) {
            return component;
        }
        var result = title.copy();
        var stack = TooltipItemParser.parse(raw);
        if (stack != null) {
            // Native hooks run only for the displayed item, not for every completion in the registry.
            List<Component> lines = Screen.getTooltipFromItem(Minecraft.getInstance(), stack);
            if (!lines.isEmpty()) {
                String id = ClientPlatform.identifierId(raw);
                result = NamespaceNames.qualify(id.substring(0, id.indexOf(':')), lines.get(0)).copy();
                String suffix = "(" + id + ")";
                if (title.getString().endsWith(suffix)) { result.append(suffix); }
            }
            for (int i = 1; i < lines.size(); i++) { result.append("\n").append(lines.get(i)); }
        }
        if (!description.getString().isEmpty() && !description.getString().equals(title.getString())) {
            result.append("\n").append(description);
        }
        for (Component sibling : component.getSiblings()) { result.append(sibling); }
        return result;
    }

}
