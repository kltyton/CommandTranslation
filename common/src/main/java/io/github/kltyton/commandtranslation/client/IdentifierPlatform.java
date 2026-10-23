package io.github.kltyton.commandtranslation.client;

import io.github.kltyton.commandtranslation.client.i18n.MinecraftText;
import io.github.kltyton.commandtranslation.client.i18n.CommandRegistries;
import io.github.kltyton.commandtranslation.client.i18n.IdentifierAccess;
import io.github.kltyton.commandtranslation.client.i18n.IdentifierHover;
import io.github.kltyton.commandtranslation.client.i18n.RegistryTagIds;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import io.github.kltyton.commandtranslation.client.i18n.RegistryValueNames;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import java.util.stream.Collectors;

public final class IdentifierPlatform {
    private IdentifierPlatform() {}
    public static String registryId(ResourceKey<?> key) { return IdentifierAccess.registryId(key); }
    public static java.util.List<String> ids(net.minecraft.core.Registry<?> registry, boolean tags) {
        return tags ? RegistryTagIds.ids(registry).toList()
                : registry.keySet().stream().map(Object::toString).toList();
    }
    public static String key(String kind, String raw) {
        var identifier = IdentifierAccess.tryParse(raw);
        return identifier == null ? null : kind + "." + identifier.getNamespace() + "." + identifier.getPath().replace('/', '.');
    }
    public static Component name(String kind, String raw) {
        var identifier = IdentifierAccess.read(raw);
        if (identifier == null) { return null; }
        return switch (kind) {
            case "item" -> CommandRegistries.ITEM.getOptional(identifier).map(ItemStack::new).map(ItemStack::getHoverName).orElse(null);
            case "block" -> CommandRegistries.BLOCK.getOptional(identifier).map(Block::getName).orElse(null);
            case "entity" -> CommandRegistries.ENTITY_TYPE.getOptional(identifier).map(type -> type.getDescription()).orElse(null);
            case "attribute" -> CommandRegistries.ATTRIBUTE.getOptional(identifier).map(value -> MinecraftText.translatable(value.getDescriptionId())).orElse(null);
            case "effect" -> CommandRegistries.MOB_EFFECT.getOptional(identifier).map(value -> value.getDisplayName()).orElse(null);
            default -> RegistryValueNames.name(kind, identifier);
        };
    }
    public static String id(String raw) {
        var identifier = IdentifierAccess.read(raw);
        return identifier == null ? null : identifier.toString();
    }
    public static String blockPropertyValues(String raw, String property) {
        var identifier = IdentifierAccess.read(raw);
        if (identifier == null) { return null; }
        return CommandRegistries.BLOCK.getOptional(identifier)
                .map(block -> block.getStateDefinition().getProperty(property)).map(IdentifierPlatform::propertyValues).orElse(null);
    }
    private static <T extends Comparable<T>> String propertyValues(Property<T> property) {
        return property.getPossibleValues().stream().map(property::getName).sorted().collect(Collectors.joining("_"));
    }
    public static MutableComponent hover(MutableComponent component, String id) {
        return component.withStyle(style -> style.withHoverEvent(IdentifierHover.text(MinecraftText.literal(id))));
    }
}
