package io.github.kltyton.commandtranslation.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Builds a client parsing view; the received server tree remains authoritative. */
public final class CommandAliases<S> {
    private final CommandDispatcher<S> mirror;
    private final Map<ArgumentType<?>, List<String>> choices = new IdentityHashMap<>();
    private final Map<CommandNode<S>, CommandNode<S>> originals = new IdentityHashMap<>();
    private final Map<CommandNode<S>, Map<String, String>> enumAliases = new IdentityHashMap<>();
    private final Map<CommandNode<S>, List<String>> literalNames = new IdentityHashMap<>();
    private final Map<CommandNode<S>, Map<String, String>> argumentNames = new IdentityHashMap<>();

    public CommandAliases(CommandDispatcher<S> server,
                          BiFunction<CommandNode<S>, String, List<String>> names,
                          Function<ArgumentType<?>, List<String>> enumerations,
                          BiFunction<String, String, List<String>> qualifiedNames) {
        Map<CommandNode<S>, CommandNode<S>> copies = new IdentityHashMap<>();
        Map<CommandNode<S>, String> paths = new IdentityHashMap<>();
        Map<CommandNode<S>, String> roots = new IdentityHashMap<>();
        ArrayDeque<CommandNode<S>> queue = new ArrayDeque<>();
        queue.add(server.getRoot());
        paths.put(server.getRoot(), "");
        roots.put(server.getRoot(), "");
        List<CommandNode<S>> ordered = new ArrayList<>();
        while (!queue.isEmpty()) {
            CommandNode<S> node = queue.removeFirst();
            if (copies.containsKey(node)) { continue; }
            if (node instanceof ArgumentCommandNode<S, ?> argument) {
                choices.computeIfAbsent(argument.getType(), enumerations);
            }
            CommandNode<S> copy = copy(node, node.getName(), copies, Map.of());
            copies.put(node, copy);
            originals.put(copy, node);
            ordered.add(node);
            for (CommandNode<S> child : node.getChildren()) {
                paths.putIfAbsent(child, paths.get(node).isEmpty() ? child.getName() : paths.get(node) + "." + child.getName());
                roots.putIfAbsent(child, node instanceof RootCommandNode<?> ? child.getName() : roots.get(node));
                queue.addLast(child);
            }
            if (node.getRedirect() != null) {
                paths.putIfAbsent(node.getRedirect(), "");
                roots.putIfAbsent(node.getRedirect(), "");
                queue.addLast(node.getRedirect());
            }
        }
        for (CommandNode<S> parent : ordered) {
            CommandNode<S> parentCopy = copies.get(parent);
            Map<String, List<CommandNode<S>>> candidates = new LinkedHashMap<>();
            for (CommandNode<S> child : parent.getChildren()) {
                parentCopy.addChild(copies.get(child));
                if (child instanceof LiteralCommandNode<S>) {
                    for (String alias : names.apply(child, paths.get(child)).stream()
                            .flatMap(name -> qualifiedNames.apply(roots.get(child), name).stream()).distinct().toList()) {
                        if (validAlias(alias) && parent.getChild(alias) == null) {
                            candidates.computeIfAbsent(alias, ignored -> new ArrayList<>()).add(child);
                        }
                    }
                } else if (child instanceof ArgumentCommandNode<S, ?> argument) {
                    Map<String, String> aliases = new LinkedHashMap<>();
                    List<String> values = values(argument);
                    for (String value : values) {
                        for (String alias : names.apply(child, "value." + value).stream()
                                .flatMap(name -> qualifiedNames.apply(roots.get(child), name).stream()).distinct().toList()) {
                            if (validAlias(alias) && !values.contains(alias)) {
                                String prior = aliases.putIfAbsent(alias, value);
                                if (prior != null && !prior.equals(value)) { aliases.put(alias, ""); }
                            }
                        }
                    }
                    aliases.values().removeIf(String::isEmpty);
                    if (!values.isEmpty()) {
                        enumAliases.put(copies.get(child), aliases);
                        argumentNames.put(child, aliases);
                        setEnumAliases(copies.get(child), aliases);
                    }
                }
            }
            candidates.forEach((alias, nodes) -> {
                if (nodes.size() == 1) {
                    CommandNode<S> original = nodes.get(0);
                    CommandNode<S> aliasNode = copy(original, alias, copies, Map.of());
                    originals.put(aliasNode, original);
                    literalNames.computeIfAbsent(original, ignored -> new ArrayList<>()).add(alias);
                    parentCopy.addChild(aliasNode);
                }
            });
        }
        // Alias nodes share only the completed mirror children, never the server nodes.
        for (Map.Entry<CommandNode<S>, CommandNode<S>> entry : originals.entrySet()) {
            CommandNode<S> canonical = copies.get(entry.getValue());
            if (entry.getKey() != canonical) {
                canonical.getChildren().forEach(entry.getKey()::addChild);
            }
        }
        mirror = new CommandDispatcher<>((RootCommandNode<S>) copies.get(server.getRoot()));
        literalNames.replaceAll((node, aliases) -> List.copyOf(aliases));
    }

    public List<String> names(CommandNode<S> original, String value) {
        if (original instanceof LiteralCommandNode<?>) { return literalNames.getOrDefault(original, List.of()); }
        return argumentNames.getOrDefault(original, Map.of()).entrySet().stream()
                .filter(entry -> entry.getValue().equals(value)).map(Map.Entry::getKey).toList();
    }

    public List<String> values(ArgumentCommandNode<S, ?> node) {
        return choices.getOrDefault(node.getType(), List.of());
    }

    public CommandText translate(String input, int start, S source) {
        Map<Integer, CommandText.Replacement> replacements = new LinkedHashMap<>();
        ArrayDeque<Integer> commands = new ArrayDeque<>();
        commands.add(start);
        while (!commands.isEmpty()) {
            int commandStart = commands.removeFirst();
            StringReader reader = new StringReader(input);
            reader.setCursor(commandStart);
            ParseResults<S> result = mirror.parse(reader, source);
            for (CommandContextBuilder<S> context = result.getContext(); context != null; context = context.getChild()) {
                CommandNode<S> contextRoot = originals.get(context.getRootNode());
                String root = contextRoot instanceof LiteralCommandNode<S> literal ? literal.getLiteral() : null;
                for (var parsed : context.getNodes()) {
                    CommandNode<S> original = originals.get(parsed.getNode());
                    if (root == null && original instanceof LiteralCommandNode<S> literal) { root = literal.getLiteral(); }
                    String raw = input.substring(parsed.getRange().getStart(), parsed.getRange().getEnd());
                    String value = original instanceof LiteralCommandNode<S> literal ? literal.getLiteral()
                            : enumAliases.getOrDefault(parsed.getNode(), Map.of()).get(raw);
                    if (value != null && !value.equals(raw)) {
                        replacements.put(parsed.getRange().getStart(), new CommandText.Replacement(
                                parsed.getRange().getStart(), parsed.getRange().getEnd(), value));
                    }
                    // Vanilla help reparses its greedy query as a command, not a message.
                    if (root != null && "help".equals(CommandHelp.canonicalRoot(root)) && original instanceof ArgumentCommandNode<S, ?>
                            && original.getName().equals("command") && parsed.getRange().getStart() > commandStart) {
                        commands.addLast(parsed.getRange().getStart());
                    }
                }
            }
        }
        return new CommandText(input, List.copyOf(replacements.values()));
    }

    public static <S> ParseResults<S> originalOffsets(CommandDispatcher<S> dispatcher, ParseResults<S> parsed, CommandText text) {
        CommandContextBuilder<S> context = mapContext(dispatcher, parsed.getContext(), text);
        StringReader reader = new StringReader(text.original());
        reader.setCursor(text.originalOffset(parsed.getReader().getCursor()));
        Map<CommandNode<S>, CommandSyntaxException> errors = new HashMap<>();
        parsed.getExceptions().forEach((node, error) -> errors.put(node, new CommandSyntaxException(
                error.getType(), error.getRawMessage(), text.original(),
                error.getCursor() < 0 ? error.getCursor() : text.originalOffset(error.getCursor()))));
        return new ParseResults<>(context, reader, errors);
    }

    private static <S> CommandContextBuilder<S> mapContext(CommandDispatcher<S> dispatcher, CommandContextBuilder<S> source, CommandText text) {
        CommandContextBuilder<S> result = new CommandContextBuilder<>(dispatcher, source.getSource(), source.getRootNode(),
                text.originalOffset(source.getRange().getStart()));
        result.withCommand(source.getCommand());
        source.getArguments().forEach((name, argument) -> result.withArgument(name,
                new ParsedArgument<>(text.originalOffset(argument.getRange().getStart()),
                        text.originalOffset(argument.getRange().getEnd()), argument.getResult())));
        source.getNodes().forEach(node -> result.withNode(node.getNode(), text.originalRange(node.getRange())));
        if (source.getChild() != null) { result.withChild(mapContext(dispatcher, source.getChild(), text)); }
        return result;
    }

    private CommandNode<S> copy(CommandNode<S> original, String name,
                                Map<CommandNode<S>, CommandNode<S>> copies, Map<String, String> aliases) {
        if (original instanceof RootCommandNode<S>) { return new RootCommandNode<>(); }
        if (original instanceof LiteralCommandNode<S>) {
            return new LiteralCommandNode<>(name, original.getCommand(), original.getRequirement(), null,
                    original.getRedirectModifier(), original.isFork()) {
                @Override public CommandNode<S> getRedirect() { return copies.get(original.getRedirect()); }
            };
        }
        return copyArgument((ArgumentCommandNode<S, ?>) original, copies, aliases);
    }

    private <T> CommandNode<S> copyArgument(ArgumentCommandNode<S, T> original,
                                          Map<CommandNode<S>, CommandNode<S>> copies, Map<String, String> aliases) {
        ArgumentType<T> type = values(original).isEmpty()
                ? original.getType() : new EnumArgument<>(original.getType(), aliases);
        return new ArgumentCommandNode<>(original.getName(), type,
                original.getCommand(), original.getRequirement(), null, original.getRedirectModifier(),
                original.isFork(), original.getCustomSuggestions()) {
            @Override public CommandNode<S> getRedirect() { return copies.get(original.getRedirect()); }
        };
    }

    private static void setEnumAliases(CommandNode<?> node, Map<String, String> aliases) {
        ((EnumArgument<?>) ((ArgumentCommandNode<?, ?>) node).getType()).aliases = aliases;
    }

    private static boolean validAlias(String alias) {
        return !alias.isEmpty() && alias.codePoints().noneMatch(Character::isWhitespace);
    }

    private static final class EnumArgument<T> implements ArgumentType<T> {
        private final ArgumentType<T> original;
        private Map<String, String> aliases;
        private EnumArgument(ArgumentType<T> original, Map<String, String> aliases) {
            this.original = original;
            this.aliases = aliases;
        }
        @Override public T parse(StringReader reader) throws CommandSyntaxException {
            int start = reader.getCursor();
            int end = start;
            while (end < reader.getTotalLength() && !Character.isWhitespace(reader.getString().charAt(end))) { end++; }
            String canonical = aliases.get(reader.getString().substring(start, end));
            if (canonical == null) { return original.parse(reader); }
            T value = original.parse(new StringReader(canonical));
            reader.setCursor(end);
            return value;
        }
    }
}
