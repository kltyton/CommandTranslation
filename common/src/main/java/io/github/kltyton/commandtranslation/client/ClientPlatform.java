package io.github.kltyton.commandtranslation.client;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.kltyton.commandtranslation.client.command.CommandClient;
import io.github.kltyton.commandtranslation.client.command.help.ArgumentDocumentation;
import io.github.kltyton.commandtranslation.client.config.ClientConfig;
import io.github.kltyton.commandtranslation.client.mixin.EditBoxAccessor;
import io.github.kltyton.commandtranslation.client.i18n.CommandRegistries;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;

public final class ClientPlatform {
    private static final Set<String> FINITE_ARGUMENTS = Set.of("brigadier:bool", "minecraft:gamemode",
            "minecraft:color", "minecraft:team_color", "minecraft:entity_anchor", "minecraft:heightmap",
            "minecraft:template_mirror", "minecraft:template_rotation", "minecraft:scoreboard_slot",
            "minecraft:item_slot", "minecraft:item_slots");
    private ClientPlatform() {}

    public static boolean finiteArgument(String parser) { return FINITE_ARGUMENTS.contains(parser); }

    public static <S> List<String> enumValues(ArgumentType<?> argument, S source) {
        String parser = ArgumentDocumentation.parser(argument);
        if (parser == null || !FINITE_ARGUMENTS.contains(parser)) { return List.of(); }
        CommandDispatcher<S> dispatcher = new CommandDispatcher<>();
        var context = new CommandContextBuilder<>(dispatcher, source, dispatcher.getRoot(), 0).build("");
        // These native providers build completed futures from finite tables, without registry or network requests.
        return argument.listSuggestions(context, new SuggestionsBuilder("", 0)).join().getList().stream()
                .map(com.mojang.brigadier.suggestion.Suggestion::getText).toList();
    }

    public static String identifierKey(String kind, String id) { return IdentifierPlatform.key(kind, id); }
    public static String identifierId(String raw) { return IdentifierPlatform.id(raw); }
    public static String registryId(ResourceKey<?> key) { return IdentifierPlatform.registryId(key); }
    public static String blockPropertyValues(String id, String property) { return IdentifierPlatform.blockPropertyValues(id, property); }
    public static Component identifierName(String kind, String id) { return IdentifierPlatform.name(kind, id); }
    public static net.minecraft.core.Registry<?> registry(String id) {
        switch (id) {
            case "minecraft:item": return CommandRegistries.ITEM;
            case "minecraft:block": return CommandRegistries.BLOCK;
            case "minecraft:entity_type": return CommandRegistries.ENTITY_TYPE;
            case "minecraft:attribute": return CommandRegistries.ATTRIBUTE;
            default: break;
        }
        net.minecraft.core.Registry<?> registry = CommandRegistries.serverRegistries()
                .filter(value -> CommandRegistries.id(value).equals(id)).findFirst().orElse(null);
        return registry != null ? registry : CommandRegistries.REGISTRY.stream().<net.minecraft.core.Registry<?>>map(value -> value)
                .filter(value -> CommandRegistries.id(value).equals(id)).findFirst().orElse(null);
    }
    public static Set<String> namespaces() {
        Set<String> namespaces = new java.util.LinkedHashSet<>();
        namespaces.add("minecraft");
        CommandRegistries.ITEM.keySet().forEach(id -> namespaces.add(id.getNamespace()));
        CommandRegistries.BLOCK.keySet().forEach(id -> namespaces.add(id.getNamespace()));
        CommandRegistries.ENTITY_TYPE.keySet().forEach(id -> namespaces.add(id.getNamespace()));
        CommandRegistries.serverRegistries().forEach(registry -> registry.keySet().forEach(id -> namespaces.add(id.getNamespace())));
        return namespaces;
    }
    public static List<String> registryIds(net.minecraft.core.Registry<?> registry, boolean tags) { return IdentifierPlatform.ids(registry, tags); }
    public static MutableComponent withIdentifierHover(MutableComponent component, String id) {
        return IdentifierPlatform.hover(component, id);
    }

    public static String outgoing(String command) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null || !ClientConfig.current().enabled || !ClientConfig.current().localizedInput) { return command; }
        return CommandClient.translate(connection.getCommands(), command, 0, connection.getSuggestionsProvider()).canonical();
    }

    public static Component inputTooltip(EditBox input, int mouseX, int mouseY, int textX) {
        if (!ClientConfig.current().enabled || !ClientConfig.current().inputTooltips || (!ClientConfig.current().usageTooltips && !ClientConfig.current().identifierTranslation)
                || mouseY < io.github.kltyton.commandtranslation.client.screen.WidgetCoordinates.y(input) || mouseY >= io.github.kltyton.commandtranslation.client.screen.WidgetCoordinates.y(input) + input.getHeight()
                || mouseX < textX || mouseX >= textX + input.getInnerWidth()) { return null; }
        var parse = CommandClient.inputParse(input);
        if (parse == null || !parse.getReader().getString().equals(input.getValue())) { return null; }
        int first = ((EditBoxAccessor) input).commandTranslation$displayPos();
        var font = Minecraft.getInstance().font;
        String visible = font.plainSubstrByWidth(input.getValue().substring(first), input.getInnerWidth());
        if (mouseX >= textX + font.width(visible)) { return null; }
        int position = first + font.plainSubstrByWidth(visible, mouseX - textX).length();
        return CommandClient.inputHelp(input, position);
    }
}
