package io.github.kltyton.commandtranslation.client.command.help;

import io.github.kltyton.commandtranslation.client.i18n.MinecraftText;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import io.github.kltyton.commandtranslation.client.i18n.CommandLanguage;
import net.minecraft.network.chat.Component;

public final class ArgumentDocumentation {
    private ArgumentDocumentation() {}

    public static Component bounds(CommandNode<?> node) {
        if (!(node instanceof ArgumentCommandNode<?, ?> argument)) { return null; }
        return bounds(argument.getType());
    }

    public static String parser(CommandNode<?> node) {
        return node instanceof ArgumentCommandNode<?, ?> argument ? parser(argument.getType()) : null;
    }

    public static String registry(CommandNode<?> node) {
        JsonObject properties = properties(node);
        if (properties.has("registry")) { return properties.get("registry").getAsString(); }
        String parser = parser(node);
        return parser == null ? null : CommandLanguage.present(CommandLanguage.PREFIX + "help.argument.type." + parser + ".registry");
    }

    public static JsonObject properties(CommandNode<?> node) {
        return node instanceof ArgumentCommandNode<?, ?> argument
                ? ArgumentTypeMetadata.properties(argument.getType()) : new JsonObject();
    }

    static String valueKey(CommandNode<?> node, String value) {
        if (!(node instanceof ArgumentCommandNode<?, ?> argument)) { return null; }
        String parser = parser(argument.getType());
        if (parser == null || parser.equals("brigadier:bool")) { return null; }
        String key = CommandLanguage.PREFIX + "help.value.type." + parser + "." + value;
        return CommandLanguage.present(key) == null ? null : key;
    }

    public static String parser(ArgumentType<?> type) {
        return ArgumentTypeMetadata.parser(type);
    }

    private static Component bounds(ArgumentType<?> type) {
        if (type instanceof IntegerArgumentType value) {
            return range(value.getMinimum() == Integer.MIN_VALUE ? null : Integer.toString(value.getMinimum()),
                    value.getMaximum() == Integer.MAX_VALUE ? null : Integer.toString(value.getMaximum()));
        }
        if (type instanceof FloatArgumentType value) {
            return range(value.getMinimum() == -Float.MAX_VALUE ? null : Float.toString(value.getMinimum()),
                    value.getMaximum() == Float.MAX_VALUE ? null : Float.toString(value.getMaximum()));
        }
        if (type instanceof DoubleArgumentType value) {
            return range(value.getMinimum() == -Double.MAX_VALUE ? null : Double.toString(value.getMinimum()),
                    value.getMaximum() == Double.MAX_VALUE ? null : Double.toString(value.getMaximum()));
        }
        return null;
    }

    private static Component range(String minimum, String maximum) {
        if (minimum != null && maximum != null) {
            return MinecraftText.translatable("command_translation.help.range.between", minimum, maximum);
        }
        if (minimum != null) { return MinecraftText.translatable("command_translation.help.range.minimum", minimum); }
        if (maximum != null) { return MinecraftText.translatable("command_translation.help.range.maximum", maximum); }
        return null;
    }
}
