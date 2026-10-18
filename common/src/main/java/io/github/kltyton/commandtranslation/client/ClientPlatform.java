package io.github.kltyton.commandtranslation.client;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.kltyton.commandtranslation.client.command.CommandClient;
import io.github.kltyton.commandtranslation.client.config.ClientConfig;
import io.github.kltyton.commandtranslation.client.mixin.EditBoxAccessor;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.BuiltInRegistries;
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
        if (!ArgumentTypeInfos.isClassRecognized(argument.getClass())) { return List.of(); }
        var id = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(ArgumentTypeInfos.byClass(argument));
        if (id == null || !FINITE_ARGUMENTS.contains(id.toString())) { return List.of(); }
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
            case "minecraft:item": return BuiltInRegistries.ITEM;
            case "minecraft:block": return BuiltInRegistries.BLOCK;
            case "minecraft:entity_type": return BuiltInRegistries.ENTITY_TYPE;
            case "minecraft:attribute": return BuiltInRegistries.ATTRIBUTE;
            default: break;
        }
        var connection = Minecraft.getInstance().getConnection();
        net.minecraft.core.Registry<?> registry = connection == null ? null : connection.registryAccess().registries()
                .filter(entry -> registryId(entry.key()).equals(id)).<net.minecraft.core.Registry<?>>map(entry -> entry.value()).findFirst().orElse(null);
        return registry != null ? registry : BuiltInRegistries.REGISTRY.stream().<net.minecraft.core.Registry<?>>map(value -> value)
                .filter(value -> registryId(value.key()).equals(id)).findFirst().orElse(null);
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
                || mouseY < input.getY() || mouseY >= input.getY() + input.getHeight()
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
