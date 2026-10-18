package io.github.kltyton.commandtranslation.client.command;

import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.ArrayList;
import java.util.List;

public final class CommandHelp {
    public record Target(String command, String argument, CommandNode<?> node, StringRange range) {}

    private CommandHelp() {}

    public static String canonicalRoot(String name) {
        return name.startsWith("minecraft:") ? name.substring("minecraft:".length()) : name;
    }

    public static Target target(CommandContextBuilder<?> context, int cursor) {
        Target found = null;
        for (CommandContextBuilder<?> current = context; current != null; current = current.getChild()) {
            String root = current.getRootNode() instanceof LiteralCommandNode<?> literal ? literal.getLiteral() : null;
            for (var parsed : current.getNodes()) {
                if (parsed.getRange().getStart() > cursor) { break; }
                if (root == null && parsed.getNode() instanceof LiteralCommandNode<?>) { root = parsed.getNode().getName(); }
                if (root != null) {
                    found = new Target(root, parsed.getNode().getName(), parsed.getNode(), parsed.getRange());
                    if (parsed.getRange().getStart() <= cursor && cursor < parsed.getRange().getEnd()) { return found; }
                }
            }
        }
        return found;
    }

    public static List<String> path(CommandContextBuilder<?> context, int cursor) {
        return nodes(context, cursor).stream().map(CommandNode::getName).toList();
    }

    public static <S> List<CommandNode<S>> nodes(CommandContextBuilder<S> context, int cursor) {
        List<CommandNode<S>> path = new ArrayList<>();
        for (CommandContextBuilder<S> current = context; current != null; current = current.getChild()) {
            if (current.getRange().getStart() <= cursor) {
                path.clear();
                if (current.getRootNode() instanceof LiteralCommandNode<?>) { path.add(current.getRootNode()); }
            }
            for (var node : current.getNodes()) {
                if (node.getRange().getStart() <= cursor) { path.add(node.getNode()); }
            }
        }
        return path;
    }

    public static String identifierKind(CommandContextBuilder<?> context, int cursor) {
        List<String> path = path(context, cursor);
        if (path.isEmpty()) { return null; }
        return switch (canonicalRoot(path.get(0))) {
            case "locate" -> path.size() > 1 ? switch (path.get(1)) {
                case "structure", "biome", "poi" -> path.get(1);
                default -> null;
            } : null;
            case "place" -> path.size() > 1 ? switch (path.get(1)) {
                case "feature" -> "configured_feature";
                case "structure" -> "structure";
                default -> null;
            } : null;
            case "fillbiome" -> "biome";
            case "effect" -> "effect";
            case "enchant" -> "enchantment";
            default -> null;
        };
    }

    public static boolean isIdentifierArgument(String kind, String name) {
        return switch (kind) {
            case "biome" -> name.equals("biome") || name.equals("filter");
            case "configured_feature" -> name.equals("feature");
            case "effect" -> name.equals("effect");
            case "enchantment" -> name.equals("enchantment");
            case "structure", "poi" -> name.equals(kind);
            default -> false;
        };
    }
}
