package io.github.kltyton.commandtranslation.client.command.export;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import io.github.kltyton.commandtranslation.client.ClientPlatform;
import io.github.kltyton.commandtranslation.client.command.help.ArgumentDocumentation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Takes an immutable description of the command tree before file work leaves the client thread. */
public final class CommandExportCatalog {
    public record Entry(List<String> path, boolean argument, String parser, String properties,
                        List<String> examples, List<String> values, boolean executable, String redirect) {
        public Entry { path = List.copyOf(path); examples = List.copyOf(examples); values = List.copyOf(values); }
        public String root() { return path.get(0); }
        public String name() { return path.get(path.size() - 1); }
        public String keyPath() { return String.join(".", path); }
    }

    private CommandExportCatalog() {}

    public static <S> List<Entry> capture(CommandDispatcher<S> dispatcher, S source) {
        List<Entry> entries = new ArrayList<>();
        Set<CommandNode<S>> ancestors = Collections.newSetFromMap(new IdentityHashMap<>());
        for (CommandNode<S> root : dispatcher.getRoot().getChildren()) {
            visit(root, List.of(), source, ancestors, entries);
        }
        return List.copyOf(entries);
    }

    private static <S> void visit(CommandNode<S> node, List<String> parent, S source,
                                 Set<CommandNode<S>> ancestors, List<Entry> entries) {
        if (!node.canUse(source) || !ancestors.add(node)) { return; }
        try {
            List<String> path = new ArrayList<>(parent);
            path.add(node.getName());
            String parser = ArgumentDocumentation.parser(node);
            List<String> values = node instanceof ArgumentCommandNode<S, ?> argument
                    ? ClientPlatform.enumValues(argument.getType(), source) : List.of();
            entries.add(new Entry(path, node instanceof ArgumentCommandNode<?, ?>, parser,
                    ArgumentDocumentation.properties(node).toString(), new ArrayList<>(node.getExamples()), values,
                    node.getCommand() != null, node.getRedirect() == null ? null : node.getRedirect().getName()));
            for (CommandNode<S> child : node.getChildren()) { visit(child, path, source, ancestors, entries); }
        } finally { ancestors.remove(node); }
    }
}
