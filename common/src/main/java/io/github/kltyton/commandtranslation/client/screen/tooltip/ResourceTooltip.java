package io.github.kltyton.commandtranslation.client.screen.tooltip;

import io.github.kltyton.commandtranslation.client.i18n.MinecraftText;
import io.github.kltyton.commandtranslation.client.i18n.TranslationContent;
import io.github.kltyton.commandtranslation.client.i18n.CommandRegistries;
import io.github.kltyton.commandtranslation.client.ClientPlatform;
import io.github.kltyton.commandtranslation.client.config.ClientConfig;
import io.github.kltyton.commandtranslation.client.i18n.IdentifierNames;
import io.github.kltyton.commandtranslation.client.i18n.NamespaceNames;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
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
            return title == null ? null : MinecraftText.translatable(ITEM, title,
                    description == null ? MinecraftText.empty() : description, raw);
        }
        if ("minecraft:entity_type".equals(registry) && settings.entityTooltips) {
            EntityType<?> type = CommandRegistries.ENTITY_TYPE.stream()
                    .filter(value -> CommandRegistries.ENTITY_TYPE.getKey(value).toString().equals(id)).findFirst().orElse(null);
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
        TranslationContent contents = MinecraftText.translation(component);
        if (contents == null || !contents.key().equals(ITEM)
                || contents.args().length != 3 || !(contents.args()[0] instanceof Component title)
                || !(contents.args()[1] instanceof Component description) || !(contents.args()[2] instanceof String raw)) {
            return component;
        }
        var result = title.copy();
        var stack = TooltipItemParser.parse(raw);
        if (stack != null) {
            // Native hooks run only for the displayed item, not for every completion in the registry.
            List<Component> lines = ItemTooltipAccess.lines(stack);
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
