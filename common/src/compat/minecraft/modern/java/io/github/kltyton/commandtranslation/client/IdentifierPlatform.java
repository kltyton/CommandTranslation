package io.github.kltyton.commandtranslation.client;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import io.github.kltyton.commandtranslation.client.i18n.RegistryValueNames;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import java.util.stream.Collectors;

public final class IdentifierPlatform {
    private IdentifierPlatform() {}
    public static String registryId(ResourceKey<?> key) { return key.identifier().toString(); }
    public static java.util.List<String> ids(net.minecraft.core.Registry<?> registry, boolean tags) {
        return tags ? registry.getTags().map(set -> "#" + set.key().location()).toList()
                : registry.keySet().stream().map(Object::toString).toList();
    }
    public static String key(String kind, String raw) {
        Identifier identifier = Identifier.tryParse(raw);
        return identifier == null ? null : kind + "." + identifier.getNamespace() + "." + identifier.getPath().replace('/', '.');
    }
    public static Component name(String kind, String raw) {
        Identifier identifier;
        try { identifier = Identifier.read(new StringReader(raw)); }
        catch (CommandSyntaxException invalidIdentifier) { return null; }
        return switch (kind) {
            case "item" -> BuiltInRegistries.ITEM.getOptional(identifier).map(ItemStack::new).map(ItemStack::getHoverName).orElse(null);
            case "block" -> BuiltInRegistries.BLOCK.getOptional(identifier).map(Block::getName).orElse(null);
            case "entity" -> BuiltInRegistries.ENTITY_TYPE.getOptional(identifier).map(type -> type.getDescription()).orElse(null);
            case "attribute" -> BuiltInRegistries.ATTRIBUTE.getOptional(identifier).map(value -> Component.translatable(value.getDescriptionId())).orElse(null);
            case "effect" -> BuiltInRegistries.MOB_EFFECT.getOptional(identifier).map(value -> value.getDisplayName()).orElse(null);
            default -> RegistryValueNames.name(kind, identifier);
        };
    }
    public static String id(String raw) {
        try { return Identifier.read(new StringReader(raw)).toString(); }
        catch (CommandSyntaxException invalidIdentifier) { return null; }
    }
    public static String blockPropertyValues(String raw, String property) {
        Identifier identifier;
        try { identifier = Identifier.read(new StringReader(raw)); }
        catch (CommandSyntaxException invalidIdentifier) { return null; }
        return BuiltInRegistries.BLOCK.getOptional(identifier)
                .map(block -> block.getStateDefinition().getProperty(property)).map(IdentifierPlatform::propertyValues).orElse(null);
    }
    private static <T extends Comparable<T>> String propertyValues(Property<T> property) {
        return property.getPossibleValues().stream().map(property::getName).sorted().collect(Collectors.joining("_"));
    }
    public static MutableComponent hover(MutableComponent component, String id) {
        return component.withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(Component.literal(id))));
    }
}
