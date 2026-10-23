package io.github.kltyton.commandtranslation.client.command.help;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;

public final class ArgumentTypeMetadata {
    private ArgumentTypeMetadata() {}

    public static String parser(ArgumentType<?> type) {
        if (!ArgumentTypeInfos.isClassRecognized(type.getClass())) { return null; }
        return ArgumentParserRegistry.id(ArgumentTypeInfos.byClass(type));
    }

    public static <A extends ArgumentType<?>> JsonObject properties(A argument) {
        return ArgumentTypeInfos.isClassRecognized(argument.getClass())
                ? properties(ArgumentTypeInfos.byClass(argument), argument) : new JsonObject();
    }

    private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> JsonObject properties(ArgumentTypeInfo<A, T> info, A argument) {
        JsonObject properties = new JsonObject();
        info.serializeToJson(info.unpack(argument), properties);
        return properties;
    }
}
