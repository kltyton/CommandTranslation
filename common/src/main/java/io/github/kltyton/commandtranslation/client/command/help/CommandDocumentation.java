package io.github.kltyton.commandtranslation.client.command.help;

import com.mojang.brigadier.tree.CommandNode;
import io.github.kltyton.commandtranslation.client.i18n.CommandLanguage;
import io.github.kltyton.commandtranslation.client.ClientPlatform;
import io.github.kltyton.commandtranslation.client.command.CommandHelp;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class CommandDocumentation {
    public record Subject<S>(List<CommandNode<S>> nodes, String value, String entityType, S source) {
        public Subject { nodes = List.copyOf(nodes); }
    }

    private CommandDocumentation() {}

    public static <S> Component describe(Subject<S> subject) {
        Component value = value(subject);
        return value == null ? parameter(subject) : value;
    }

    public static <S> Component parameter(Subject<S> subject) {
        List<CommandNode<S>> nodes = subject.nodes();
        CommandNode<S> node = nodes.get(nodes.size() - 1);
        String root = CommandHelp.canonicalRoot(nodes.get(0).getName());
        var names = new ArrayList<>(nodes.stream().map(CommandNode::getName).toList());
        names.set(0, root);
        String path = String.join(".", names);
        String parameterKey = nodes.size() == 1 ? present("command." + root)
                : first("argument." + path, "argument." + root + "." + node.getName());
        if (parameterKey == null) { return null; }
        var result = Component.translatable(parameterKey);
        Component bounds = ArgumentDocumentation.bounds(node);
        return bounds == null ? result : result.append("\n").append(bounds.copy().withStyle(ChatFormatting.GRAY));
    }

    public static <S> Component value(Subject<S> subject) {
        List<CommandNode<S>> nodes = subject.nodes();
        CommandNode<S> node = nodes.get(nodes.size() - 1);
        String root = CommandHelp.canonicalRoot(nodes.get(0).getName());
        var names = new ArrayList<>(nodes.stream().map(CommandNode::getName).toList());
        names.set(0, root);
        String path = String.join(".", names);
        String valueKey = nodes.size() == 1 ? null : first("value." + path + "." + subject.value(),
                "value." + root + "." + node.getName() + "." + subject.value());
        if (valueKey == null) { valueKey = ArgumentDocumentation.valueKey(node, subject.value()); }
        String parser = ArgumentDocumentation.parser(node);
        if (valueKey == null && ("minecraft:dialog".equals(parser) || "minecraft:resource_location".equals(parser))) {
            String identifier = ClientPlatform.identifierId(subject.value());
            if (identifier != null) { valueKey = first("value." + path + "." + identifier, "value." + root + "." + node.getName() + "." + identifier); }
        }
        if (valueKey != null) { return Component.translatable(valueKey); }
        String binding = present("argument." + path + ".registry");
        String registry = binding == null ? ArgumentDocumentation.registry(node) : CommandLanguage.present(binding);
        return ValueDocumentation.registry(registry, subject.value(), false, true);
    }

    private static String first(String specific, String shared) {
        String key = present(specific);
        return key == null ? present(shared) : key;
    }

    private static String present(String suffix) {
        String key = CommandLanguage.PREFIX + "help." + suffix;
        return CommandLanguage.present(key) == null ? null : key;
    }
}
