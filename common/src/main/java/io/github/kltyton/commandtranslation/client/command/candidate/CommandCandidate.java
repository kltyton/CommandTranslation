package io.github.kltyton.commandtranslation.client.command.candidate;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.kltyton.commandtranslation.client.ClientPlatform;
import io.github.kltyton.commandtranslation.client.command.CommandHelp;
import io.github.kltyton.commandtranslation.client.command.help.ArgumentDocumentation;
import io.github.kltyton.commandtranslation.client.command.help.ArgumentHover;
import io.github.kltyton.commandtranslation.client.command.help.CommandDocumentation;
import io.github.kltyton.commandtranslation.client.command.help.StructuredFieldLocator;
import io.github.kltyton.commandtranslation.client.command.help.ValueDocumentation;
import io.github.kltyton.commandtranslation.client.command.help.DocumentationKeys;
import io.github.kltyton.commandtranslation.client.config.ClientConfig;
import io.github.kltyton.commandtranslation.client.i18n.IdentifierNames;
import io.github.kltyton.commandtranslation.client.i18n.CommandLanguage;
import io.github.kltyton.commandtranslation.client.i18n.NamespaceNames;
import io.github.kltyton.commandtranslation.client.screen.tooltip.CommandTooltip;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/** Names one real provider choice, retaining the exact token separately from its completion template. */
public record CommandCandidate(NativeSuggestionQuery.Source<?> source, StringRange tokenRange, String canonical,
                               String identifier, String namespace, String insertion, String label, List<String> aliases, Component description) {
    public CommandCandidate { aliases = List.copyOf(aliases); }

    public Message tooltip(Message nativeTooltip) {
        if (!ClientConfig.current().suggestionTooltips) { return nativeTooltip; }
        if (nativeTooltip == null) { return description; }
        Component nativeText = nativeTooltip instanceof Component component ? component : Component.literal(nativeTooltip.getString());
        if (!NamespaceNames.isQualified(nativeText)) { nativeText = NamespaceNames.qualify(namespace, nativeText); }
        if (description != null) {
            var existing = description.getString().lines().map(this::withoutDisambiguation).collect(java.util.stream.Collectors.toSet());
            var result = description.copy();
            boolean separator = false;
            for (Component line : tooltipLines(nativeText)) {
                String text = withoutDisambiguation(line.getString());
                if (text.isBlank()) { separator = true; continue; }
                if (!existing.add(text)) { continue; }
                if (separator) { result.append("\n"); separator = false; }
                result.append("\n").append(line);
            }
            return result;
        }
        return nativeText;
    }

    private String withoutDisambiguation(String line) {
        String suffix = "(" + identifier + ")";
        return line.endsWith(suffix) ? line.substring(0, line.length() - suffix.length()) : line;
    }

    private static List<MutableComponent> tooltipLines(Component component) {
        List<MutableComponent> lines = new ArrayList<>();
        lines.add(Component.empty());
        component.visit((style, text) -> {
            String[] parts = text.split("\n", -1);
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) { lines.add(Component.empty()); }
                lines.get(lines.size() - 1).append(Component.literal(parts[i]).withStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return lines;
    }

    public String disambiguatedInsertion() {
        int relativeEnd = tokenRange.getEnd() - source.original().getRange().getStart();
        if (relativeEnd > source.original().getText().length()) { return insertion; }
        int end = insertion.length() - (source.original().getText().length() - relativeEnd);
        if (!canonical.isEmpty() && (canonical.charAt(0) == '"' || canonical.charAt(0) == '\'')) { end--; }
        if (insertion.substring(0, end).endsWith("(" + identifier + ")")) { return insertion; }
        return insertion.substring(0, end) + "(" + identifier + ")" + insertion.substring(end);
    }

    public boolean matches(String query) {
        String needle = decode(query).toLowerCase(Locale.ROOT);
        return canonical.toLowerCase(Locale.ROOT).contains(needle)
                || aliases.stream().anyMatch(alias -> alias.toLowerCase(Locale.ROOT).contains(needle));
    }

    public static <S> CommandCandidate describe(NativeSuggestionQuery.Source<S> source) {
        var suggestion = source.original();
        String input = source.context().getInput();
        int start = suggestion.getRange().getStart();
        String prefix = input.substring(source.argumentStart(), start);
        String argument = prefix + suggestion.getText();
        int pointer = Math.min(prefix.length(), Math.max(0, argument.length() - 1));
        String parser = ArgumentDocumentation.parser(source.node());
        StructuredFieldLocator.Located located = locate(argument, pointer, parser);
        int from = located == null ? 0 : located.start();
        int to = located == null ? argument.length() : located.end();
        String token = argument.substring(from, to);
        String raw = decode(token);
        List<CommandNode<S>> path = path(source.context(), source.node(), source.argumentStart());
        String entity = entityType(source.context(), source.argumentStart());
        String kind = kind(path, source.node());
        var subject = new CommandDocumentation.Subject<>(path, argument, entity, source.context().getSource());
        Component identity = ArgumentHover.identity(subject, argument, pointer, kind);
        String registry = source.metadata().registry();
        if (registry == null && located == null) { registry = ArgumentDocumentation.registry(source.node()); }
        if (registry == null && located == null) {
            registry = switch (parser == null ? "" : parser) {
                case "minecraft:item_stack", "minecraft:item_predicate" -> "minecraft:item";
                case "minecraft:block_state", "minecraft:block_predicate" -> "minecraft:block";
                case "minecraft:function" -> "minecraft:function";
                default -> null;
            };
        }
        if (registry != null) { IdentifierNames.observeRegistry(registry, raw); }
        Component resource = registry == null ? null : IdentifierNames.displayRegistry(registry, raw);
        if (resource != null) { identity = resource; }
        String owner = owner(path, argument, located, raw, entity, registry);
        Component explanation = ArgumentHover.describe(subject, argument, pointer, kind);
        var settings = ClientConfig.current();
        boolean explain = located == null || settings.structuredTooltips && (located.field().key() || settings.valueTooltips);
        Component registryHelp = explain ? ValueDocumentation.registry(registry, raw,
                settings.identifierTranslation, settings.usageTooltips) : null;
        if (registryHelp != null && !ValueDocumentation.specific(explanation)) { explanation = registryHelp; }
        if (identity != null && !NamespaceNames.isQualified(identity)) { identity = NamespaceNames.qualify(owner, identity); }
        if (explanation != null && !NamespaceNames.isQualified(explanation)) { explanation = NamespaceNames.qualify(owner, explanation); }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        String preferred = null;
        if (identity != null) {
            String name = (raw.startsWith("!") ? "!" : "") + identity.getString();
            names.add(name);
            String marker = raw.startsWith("#") ? "#" : raw.startsWith("!") ? "!" : "";
            String translatedPrefix = marker + NamespaceNames.name(owner).getString() + ":";
            if (name.startsWith(translatedPrefix)) {
                names.add(marker + owner + ":" + name.substring(translatedPrefix.length()));
            }
        }
        if (located == null && (identity != null || source.node() instanceof LiteralCommandNode<?> || parser != null && ClientPlatform.finiteArgument(parser))) {
            String aliasPath = source.node() instanceof LiteralCommandNode<?> ? String.join(".", path.stream().map(CommandNode::getName).toList())
                    : "value." + raw;
            if (aliasPath.startsWith("minecraft:")) { aliasPath = aliasPath.substring(10); }
            for (String alias : CommandLanguage.aliases(aliasPath, raw)) {
                List<String> qualified = NamespaceNames.aliases(owner, alias);
                if (preferred == null) { preferred = qualified.get(0); }
                names.addAll(qualified);
            }
        }
        String label = preferred != null ? preferred : names.isEmpty() ? raw : names.iterator().next();
        if (settings.descriptionSearch && explanation != null) {
            var document = CommandTooltip.read(explanation);
            Component meaning = document == null ? explanation : document.description();
            if (!meaning.getString().isBlank()) {
                names.add(NamespaceNames.isQualified(meaning) ? meaning.getString() : NamespaceNames.qualify(owner, meaning).getString());
            }
            if (raw.startsWith("@") && raw.length() == 2) {
                Component selector = DocumentationKeys.translated("value.type.minecraft:entity." + raw);
                if (selector != null) { names.add(NamespaceNames.qualify(owner, selector).getString()); }
            }
        }
        Message nativeTooltip = suggestion.getTooltip();
        if (ClientConfig.current().descriptionSearch && nativeTooltip != null && (registry != null || source.node() instanceof LiteralCommandNode<?>
                || located != null && located.field().key() && identity != null || parser != null && ClientPlatform.finiteArgument(parser))) {
            String description = nativeTooltip.getString();
            if (!description.isBlank()) {
                String qualified = nativeTooltip instanceof Component component && NamespaceNames.isQualified(component)
                        ? description : NamespaceNames.name(owner).getString() + ":" + description;
                names.add(qualified);
            }
        }
        boolean resourceToken = registry != null || ArgumentDocumentation.registry(source.node()) != null || kind != null
                || located == null && ("minecraft:item_stack".equals(parser) || "minecraft:item_predicate".equals(parser)
                || "minecraft:block_state".equals(parser) || "minecraft:block_predicate".equals(parser));
        String identifier = resourceToken ? ClientPlatform.identifierId(raw.startsWith("#") || raw.startsWith("!") ? raw.substring(1) : raw) : null;
        if (identifier == null) {
            identifier = source.node() instanceof LiteralCommandNode<?> && path.size() == 1 && !raw.contains(":")
                    ? owner + ":" + raw : raw;
        }
        else if (raw.startsWith("#") || raw.startsWith("!")) { identifier = raw.charAt(0) + identifier; }
        for (String name : List.copyOf(names)) {
            String suffix = "(" + identifier + ")";
            if (name.endsWith(suffix)) { names.add(name.substring(0, name.length() - suffix.length())); }
            else { names.add(name + suffix); }
        }
        String insertion = suggestion.getText();
        int relativeStart = from - prefix.length();
        int relativeEnd = to - prefix.length();
        if (!names.isEmpty() && relativeStart >= 0 && relativeEnd <= insertion.length()) {
            insertion = insertion.substring(0, relativeStart) + encode(label, token) + insertion.substring(relativeEnd);
        }
        return new CommandCandidate(source, StringRange.between(source.argumentStart() + from, source.argumentStart() + to),
                token, identifier, owner, insertion, insertion, List.copyOf(names), explanation);
    }

    public static StructuredFieldLocator.Located locate(String argument, int cursor, String parser) {
        if ("minecraft:nbt_path".equals(parser)) { return StructuredFieldLocator.locatePath(argument, cursor); }
        int list = argument.indexOf('[');
        int compound = argument.indexOf('{');
        boolean assignment = "minecraft:item_stack".equals(parser) || "minecraft:item_predicate".equals(parser)
                || "minecraft:block_state".equals(parser) || "minecraft:block_predicate".equals(parser);
        if (list >= 0 && (compound < 0 || list < compound) && assignment) {
            return StructuredFieldLocator.locate(argument, list, cursor, StructuredFieldLocator.Format.ASSIGNMENTS);
        }
        if (argument.startsWith("@") && list >= 0) {
            return StructuredFieldLocator.locate(argument, list, cursor, StructuredFieldLocator.Format.SELECTOR);
        }
        return StructuredFieldLocator.locate(argument, compound, cursor, StructuredFieldLocator.Format.COMPOUND);
    }

    private static <S> List<CommandNode<S>> path(CommandContext<S> context, CommandNode<S> node, int start) {
        List<CommandNode<S>> path = new ArrayList<>();
        for (CommandContext<S> current = context; current != null; current = current.getChild()) {
            if (current.getRange().getStart() <= start) {
                path.clear();
                if (current.getRootNode() instanceof LiteralCommandNode<?>) { path.add(current.getRootNode()); }
            }
            for (var parsed : current.getNodes()) {
                if (parsed.getRange().getStart() < start) { path.add(parsed.getNode()); }
            }
        }
        path.add(node);
        return path;
    }

    private static String kind(List<? extends CommandNode<?>> path, CommandNode<?> node) {
        if (path.isEmpty()) { return null; }
        String root = CommandHelp.canonicalRoot(path.get(0).getName());
        String kind = switch (root) {
            case "locate" -> path.size() > 1 ? path.get(1).getName() : null;
            case "place" -> path.size() > 1 && path.get(1).getName().equals("feature") ? "configured_feature"
                    : path.size() > 1 && path.get(1).getName().equals("structure") ? "structure" : null;
            case "fillbiome" -> "biome";
            case "effect" -> "effect";
            case "enchant" -> "enchantment";
            default -> null;
        };
        return kind != null && CommandHelp.isIdentifierArgument(kind, node.getName()) ? kind : null;
    }

    private static String entityType(CommandContext<?> context, int before) {
        String entity = null;
        for (CommandContext<?> current = context; current != null; current = current.getChild()) {
            for (var parsed : current.getNodes()) {
                if (parsed.getRange().getEnd() <= before && "minecraft:entity_type".equals(ArgumentDocumentation.registry(parsed.getNode()))) {
                    entity = ClientPlatform.identifierId(current.getInput().substring(parsed.getRange().getStart(), parsed.getRange().getEnd()));
                }
            }
        }
        return entity;
    }

    private static String owner(List<? extends CommandNode<?>> path, String argument, StructuredFieldLocator.Located located,
                                String raw, String entity, String registry) {
        if (registry != null || raw.matches("#?[a-z0-9_.-]+:[a-z0-9/._-]+")) { return namespace(raw); }
        if (located != null) {
            if (!located.field().path().isEmpty()) {
                String component = located.field().path().get(0);
                if (component.contains(":")) { return namespace(component); }
            }
            if (entity != null) { return namespace(entity); }
            if (!argument.startsWith("{") && !argument.startsWith("@")) { return namespace(argument); }
        }
        return path.isEmpty() ? "minecraft" : NamespaceNames.commandNamespace(path.get(0).getName());
    }

    private static String namespace(String value) {
        String id = ClientPlatform.identifierId(value.startsWith("#") || value.startsWith("!") ? value.substring(1) : value);
        return id == null ? "minecraft" : id.substring(0, id.indexOf(':'));
    }

    public static String decode(String token) {
        if (!token.isEmpty() && (token.charAt(0) == '"' || token.charAt(0) == '\'')) {
            try { return new StringReader(token).readString(); }
            catch (CommandSyntaxException incomplete) { return token.substring(1); }
        }
        return token;
    }

    private static String encode(String value, String original) {
        if (!original.isEmpty() && (original.charAt(0) == '"' || original.charAt(0) == '\'')) {
            char quote = original.charAt(0);
            return quote + value.replace("\\", "\\\\").replace(String.valueOf(quote), "\\" + quote) + quote;
        }
        return value;
    }
}
