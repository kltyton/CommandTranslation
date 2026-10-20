package io.github.kltyton.commandtranslation.client.command.help;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import io.github.kltyton.commandtranslation.client.command.CommandHelp;
import io.github.kltyton.commandtranslation.client.config.ClientConfig;
import io.github.kltyton.commandtranslation.client.i18n.CommandLanguage;
import io.github.kltyton.commandtranslation.client.i18n.NamespaceNames;
import io.github.kltyton.commandtranslation.client.screen.tooltip.CommandTooltip;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;

/** Uses the live Brigadier tree for syntax and the language table for annotations and parameter help. */
public final class CommandSyntax {
    private static final Pattern TOKENS = Pattern.compile("<[^>]+>|->|[\\p{L}\\p{N}_:./-]+|\\s+|.");
    private CommandSyntax() {}

    public static <S> Component describe(CommandDocumentation.Subject<S> subject, Component purpose) {
        List<CommandNode<S>> path = subject.nodes();
        CommandNode<S> node = path.get(path.size() - 1);
        CommandDispatcher<S> usage = new CommandDispatcher<>();
        usage.getRoot().addChild(node);
        String grammar = usage.getSmartUsage(usage.getRoot(), subject.source()).get(node);
        if (grammar == null) { return purpose; }
        String prefix = path.size() == 1 ? "/" : "/" + String.join(" ", path.subList(0, path.size() - 1).stream().map(CommandNode::getUsageText).toList()) + " ";
        String syntax = prefix + grammar;
        String root = path.get(0).getName();
        String owner = NamespaceNames.commandNamespace(root);
        Map<String, Component> arguments = new LinkedHashMap<>();
        List<CommandTooltip.Parameter> parameters = new ArrayList<>();
        collect(path, subject, arguments, parameters, owner);
        var settings = ClientConfig.current();
        List<CommandTooltip.Token> tokens = new ArrayList<>();
        var matcher = TOKENS.matcher(syntax);
        while (matcher.find()) {
            String raw = matcher.group();
            Component annotation = Component.empty();
            if (raw.startsWith("<") && raw.endsWith(">")) {
                String name = raw.substring(1, raw.length() - 1);
                annotation = arguments.getOrDefault(name, label(List.of(name), name));
            } else if (!raw.isBlank()) {
                String literal = raw.startsWith("/") ? raw.substring(1) : raw;
                List<String> aliases = literalAliases(path, literal);
                if (!aliases.isEmpty()) {
                    annotation = Component.literal(aliases.get(0));
                    if (literal.equals(root) && !aliases.get(0).equalsIgnoreCase(CommandHelp.canonicalRoot(root))) {
                        annotation = NamespaceNames.qualify(owner, annotation);
                    }
                }
            }
            if (settings.commandSyntax) {
                tokens.add(new CommandTooltip.Token(Component.literal(raw), settings.syntaxAnnotations ? annotation : Component.empty()));
            }
        }
        Component title = NamespaceNames.qualify(owner, Component.literal("/" + String.join(" ", path.stream().map(CommandNode::getUsageText).toList())));
        return CommandTooltip.component(new CommandTooltip.Document(title, purpose == null ? Component.empty() : purpose,
                tokens, settings.parameterTooltips ? parameters : List.of()));
    }

    private static <S> void collect(List<CommandNode<S>> path, CommandDocumentation.Subject<S> subject,
            Map<String, Component> labels, List<CommandTooltip.Parameter> parameters, String owner) {
        CommandNode<S> parent = path.get(path.size() - 1);
        List<CommandNode<S>> children = parent.getChildren().stream().filter(child -> child.canUse(subject.source())).toList();
        for (CommandNode<S> child : children) {
            if (!(child instanceof ArgumentCommandNode<?, ?>)) { continue; }
            List<CommandNode<S>> next = new ArrayList<>(path);
            next.add(child);
            List<String> names = next.stream().map(CommandNode::getName).toList();
            Component label = label(names, child.getName());
            labels.putIfAbsent(child.getName(), label);
            Component meaning = CommandDocumentation.parameter(new CommandDocumentation.Subject<>(next, "", subject.entityType(), subject.source()));
            if (meaning != null) {
                var body = meaning.copy();
                String heading = parent.getCommand() == null ? "command_translation.help.syntax.required" : "command_translation.help.syntax.optional";
                Component title = NamespaceNames.qualify(owner, Component.translatable(heading, label, Component.literal(child.getUsageText())));
                parameters.add(new CommandTooltip.Parameter(title, body));
            }
            if (children.size() == 1 && child.getRedirect() == null) { collect(next, subject, labels, parameters, owner); }
        }
    }

    private static Component label(List<String> path, String name) {
        String full = String.join(".", path);
        if (full.startsWith("minecraft:")) { full = full.substring(10); }
        String key = CommandLanguage.PREFIX + "syntax.argument." + full;
        if (CommandLanguage.present(key) == null) { key = CommandLanguage.PREFIX + "syntax.argument." + name; }
        return CommandLanguage.present(key) == null ? Component.literal(name) : Component.translatable(key);
    }

    private static <S> List<String> literalAliases(List<CommandNode<S>> path, String literal) {
        for (int i = 0; i < path.size(); i++) {
            if (path.get(i).getName().equals(literal)) {
                String names = String.join(".", path.subList(0, i + 1).stream().map(CommandNode::getName).toList());
                return CommandLanguage.aliases(CommandHelp.canonicalRoot(names), literal);
            }
        }
        List<String> names = new ArrayList<>(path.stream().map(CommandNode::getName).toList());
        CommandNode<S> node = path.get(path.size() - 1);
        while (node.getChildren().size() == 1 && node.getChildren().iterator().next() instanceof ArgumentCommandNode<?, ?> && node.getRedirect() == null) {
            node = node.getChildren().iterator().next();
            names.add(node.getName());
        }
        names.add(literal);
        return CommandLanguage.aliases(CommandHelp.canonicalRoot(String.join(".", names)), literal);
    }
}
