package io.github.kltyton.commandtranslation.client.command.help;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import io.github.kltyton.commandtranslation.client.i18n.CommandLanguage;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.registries.BuiltInRegistries;
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
        return properties.has("registry") ? properties.get("registry").getAsString() : null;
    }

    public static JsonObject properties(CommandNode<?> node) {
        if (!(node instanceof ArgumentCommandNode<?, ?> argument)
                || !ArgumentTypeInfos.isClassRecognized(argument.getType().getClass())) { return new JsonObject(); }
        return properties(argument.getType());
    }

    private static <A extends ArgumentType<?>> JsonObject properties(A argument) {
        return properties(ArgumentTypeInfos.byClass(argument), argument);
    }

    private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> JsonObject properties(ArgumentTypeInfo<A, T> info, A argument) {
        JsonObject properties = new JsonObject();
        info.serializeToJson(info.unpack(argument), properties);
        return properties;
    }

    static String valueKey(CommandNode<?> node, String value) {
        if (!(node instanceof ArgumentCommandNode<?, ?> argument)) { return null; }
        String parser = parser(argument.getType());
        if (parser == null || parser.equals("brigadier:bool")) { return null; }
        String key = CommandLanguage.PREFIX + "help.value.type." + parser + "." + value;
        return CommandLanguage.present(key) == null ? null : key;
    }

    private static String parser(ArgumentType<?> type) {
        if (!ArgumentTypeInfos.isClassRecognized(type.getClass())) { return null; }
        return BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(ArgumentTypeInfos.byClass(type)).toString();
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
            return Component.translatable("command_translation.help.range.between", minimum, maximum);
        }
        if (minimum != null) { return Component.translatable("command_translation.help.range.minimum", minimum); }
        if (maximum != null) { return Component.translatable("command_translation.help.range.maximum", maximum); }
        return null;
    }
}
