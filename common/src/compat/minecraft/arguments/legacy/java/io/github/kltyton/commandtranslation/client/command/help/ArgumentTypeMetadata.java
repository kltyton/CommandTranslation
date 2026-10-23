package io.github.kltyton.commandtranslation.client.command.help;

import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import net.minecraft.commands.synchronization.ArgumentTypes;

public final class ArgumentTypeMetadata {
    private ArgumentTypeMetadata() {}

    public static String parser(ArgumentType<?> argument) {
        JsonObject metadata = metadata(argument);
        return metadata.has("parser") ? metadata.get("parser").getAsString() : null;
    }

    public static JsonObject properties(ArgumentType<?> argument) {
        JsonObject metadata = metadata(argument);
        return metadata.has("properties") ? metadata.getAsJsonObject("properties") : new JsonObject();
    }

    private static <T> JsonObject metadata(ArgumentType<T> argument) {
        if (!ArgumentTypes.isTypeRegistered(argument)) { return new JsonObject(); }
        // A leaf retains the native serializer without walking unrelated command children or redirects.
        ArgumentCommandNode<Object, T> leaf = RequiredArgumentBuilder.<Object, T>argument("value", argument).build();
        return ArgumentTypes.serializeNodeToJson(new CommandDispatcher<>(), leaf);
    }
}
