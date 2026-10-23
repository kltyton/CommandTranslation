package io.github.kltyton.commandtranslation.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.github.kltyton.commandtranslation.client.ClientPlatform;
import io.github.kltyton.commandtranslation.client.command.candidate.LocalizedInput;
import io.github.kltyton.commandtranslation.client.command.candidate.LocalizedSuggestions;
import io.github.kltyton.commandtranslation.client.command.help.CommandDocumentation;
import io.github.kltyton.commandtranslation.client.command.help.ArgumentHover;
import io.github.kltyton.commandtranslation.client.command.help.ArgumentDocumentation;
import io.github.kltyton.commandtranslation.client.config.ClientConfig;
import io.github.kltyton.commandtranslation.client.i18n.CommandLanguage;
import io.github.kltyton.commandtranslation.client.i18n.NamespaceNames;
import io.github.kltyton.commandtranslation.client.i18n.IdentifierNames;
import io.github.kltyton.commandtranslation.client.screen.ClientScreenAccess;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

public final class CommandClient {
    public record Batch<S>(CommandDispatcher<S> dispatcher, CommandAliases<S> aliases, CommandText text,
                           ParseResults<S> canonical, ParseResults<S> displayed, Map<String, Component> documentation,
                           EditBox input, int start, Runnable refresh, Language language, ClientConfig config, Object connection) {}
    private record Resolved(CommandDispatcher<?> dispatcher, CommandText text, Language language, ClientConfig config, Object connection) {}
    private static final Map<CommandDispatcher<?>, CommandAliases<?>> MIRRORS = new WeakHashMap<>();
    private static final Map<EditBox, java.lang.ref.WeakReference<Batch<?>>> INPUTS = new WeakHashMap<>();
    private static final Map<EditBox, Resolved> RESOLVED = new WeakHashMap<>();
    private static Language language;

    private CommandClient() {}

    @SuppressWarnings("unchecked")
    public static <S> CommandText translate(CommandDispatcher<S> dispatcher, String text, int start, S source) {
        if (!ClientConfig.current().enabled || !ClientConfig.current().localizedInput) { return new CommandText(text, List.of()); }
        if (language != Language.getInstance()) {
            language = Language.getInstance();
            MIRRORS.clear();
        }
        CommandAliases<S> aliases = (CommandAliases<S>) MIRRORS.computeIfAbsent(dispatcher,
                key -> new CommandAliases<>(dispatcher, (node, path) -> CommandLanguage.aliases(path,
                        path.startsWith("value.") ? path.substring("value.".length()) : node.getName()),
                        type -> ClientPlatform.enumValues(type, source),
                        (root, alias) -> java.util.stream.Stream.concat(
                                NamespaceNames.aliases(NamespaceNames.commandNamespace(root), alias).stream(),
                                java.util.stream.Stream.of(alias)).distinct().toList()));
        return aliases.translate(text, start, source);
    }

    @SuppressWarnings("unchecked")
    public static <S> Batch<S> parse(CommandDispatcher<S> dispatcher, StringReader reader, S source, EditBox input,
                                   Runnable refresh, BiFunction<StringReader, S, ParseResults<S>> parser) {
        boolean localInput = ClientScreenAccess.current() instanceof ChatScreen;
        int commandStart = reader.getCursor();
        CommandText text = localInput ? translate(dispatcher, reader.getString(), reader.getCursor(), source)
                : new CommandText(reader.getString(), List.of());
        Resolved ready = RESOLVED.get(input);
        if (localInput && ready != null && ready.dispatcher() == dispatcher && ready.text().original().equals(reader.getString())
                && ready.language() == Language.getInstance() && ready.config() == ClientConfig.current()
                && ready.connection() == Minecraft.getInstance().getConnection()) { text = ready.text(); }
        StringReader canonical = text.original().equals(text.canonical()) ? reader : new StringReader(text.canonical());
        canonical.setCursor(text.canonicalOffset(reader.getCursor()));
        ParseResults<S> result = parser.apply(canonical, source);
        ParseResults<S> displayed = text.original().equals(text.canonical()) ? result : CommandAliases.originalOffsets(dispatcher, result, text);
        CommandAliases<S> aliases = localInput && ClientConfig.current().enabled && ClientConfig.current().localizedInput
                ? (CommandAliases<S>) MIRRORS.get(dispatcher) : null;
        Batch<S> batch = new Batch<>(dispatcher, aliases, text, result, displayed, new HashMap<>(), input, commandStart, refresh,
                Language.getInstance(), ClientConfig.current(), Minecraft.getInstance().getConnection());
        INPUTS.put(input, new java.lang.ref.WeakReference<>(batch));
        return batch;
    }

    private static Batch<?> inputBatch(EditBox input) {
        var reference = INPUTS.get(input);
        return reference == null ? null : reference.get();
    }

    public static void invalidateNames() {
        RESOLVED.clear();
        for (var reference : INPUTS.values()) {
            Batch<?> batch = reference.get();
            if (batch != null) { batch.documentation().clear(); }
        }
    }

    public static ParseResults<?> inputParse(EditBox input) {
        Batch<?> batch = inputBatch(input);
        return batch == null ? null : batch.displayed();
    }

    public static Component inputHelp(EditBox input, int cursor) {
        Batch<?> batch = inputBatch(input);
        return batch == null || !ClientConfig.current().enabled ? null : inputHelp(batch, cursor);
    }

    private static <S> Component inputHelp(Batch<S> batch, int cursor) {
        int position = batch.text().canonicalOffset(cursor);
        String text = batch.text().canonical();
        if (position < batch.canonical().getContext().getRange().getStart()
                || position >= text.length() || Character.isWhitespace(text.charAt(position))) { return null; }
        return batch.documentation().computeIfAbsent("input:" + position, key -> {
            var target = CommandHelp.target(batch.canonical().getContext(), position);
            List<CommandNode<S>> path;
            int start;
            int end;
            if (target != null && position >= target.range().getStart() && position < target.range().getEnd()) {
                path = CommandHelp.nodes(batch.canonical().getContext(), position);
                start = target.range().getStart();
                end = target.range().getEnd();
            } else {
                var context = batch.canonical().getContext().findSuggestionContext(position);
                start = context.startPos;
                end = text.length();
                if (start > position) { return null; }
                CommandNode<S> node = suggestionNode(context.parent, text.substring(start, end), batch);
                if (node == null) { return null; }
                path = suggestionPath(batch, context.parent, node, position);
            }
            if (path.isEmpty()) { return null; }
            var subject = new CommandDocumentation.Subject<>(path, text.substring(start, end), entityType(batch, position), batch.canonical().getContext().getSource());
            return ArgumentHover.describe(subject, subject.value(), position - start,
                    identifierKind(batch, path.get(path.size() - 1), position));
        });
    }

    public static <S> CompletableFuture<Suggestions> complete(CommandDispatcher<S> dispatcher, ParseResults<S> display, int cursor, Batch<S> batch,
                                                             BiFunction<ParseResults<S>, Integer, CompletableFuture<Suggestions>> provider) {
        if (batch == null || batch.displayed() != display || !ClientConfig.current().enabled) { return provider.apply(display, cursor); }
        CompletableFuture<CommandText> resolved = batch.aliases() == null ? CompletableFuture.completedFuture(batch.text())
                : LocalizedInput.resolve(dispatcher, batch.text(), batch.start(), batch.canonical().getContext().getSource(), provider, false, () -> current(batch));
        return resolved.thenComposeAsync(text -> {
            if (!current(batch)) { return CompletableFuture.completedFuture(new Suggestions(StringRange.at(cursor), List.of())); }
            RESOLVED.put(batch.input(), new Resolved(dispatcher, text, batch.language(), batch.config(), batch.connection()));
            if (!text.canonical().equals(batch.text().canonical())) {
                io.github.kltyton.commandtranslation.client.thread.ClientTaskScheduler.enqueue(() -> { if (current(batch)) { batch.refresh().run(); } });
                return CompletableFuture.completedFuture(new Suggestions(StringRange.at(cursor), List.of()));
            }
            return LocalizedSuggestions.complete(dispatcher, batch.canonical(), text, batch.start(), cursor, provider,
                    () -> current(batch), batch.aliases() != null).thenApply(suggestions -> {
                        if (current(batch)) { batch.documentation().clear(); }
                        return suggestions;
                    });
        }, Minecraft.getInstance());
    }

    private static boolean current(Batch<?> batch) {
        return inputBatch(batch.input()) == batch && batch.input().getValue().equals(batch.text().original())
                && batch.language() == Language.getInstance() && batch.config() == ClientConfig.current()
                && batch.connection() == Minecraft.getInstance().getConnection();
    }

    private static <S> List<CommandNode<S>> suggestionPath(Batch<S> batch, CommandNode<S> parent, CommandNode<S> node, int cursor) {
        List<CommandNode<S>> path = parent instanceof RootCommandNode<?> ? new ArrayList<>()
                : new ArrayList<>(CommandHelp.nodes(batch.canonical().getContext(), cursor));
        while (!path.isEmpty() && path.get(path.size() - 1) != parent) { path.remove(path.size() - 1); }
        path.add(node);
        return path;
    }

    private static <S> String identifierKind(Batch<S> batch, CommandNode<S> node, int cursor) {
        if (node instanceof LiteralCommandNode<?>) {
            return IdentifierNames.literalKind(String.join(".", CommandHelp.path(batch.canonical().getContext(), cursor)), node.getName());
        }
        String kind = CommandHelp.identifierKind(batch.canonical().getContext(), cursor);
        return kind != null && CommandHelp.isIdentifierArgument(kind, node.getName()) ? kind : null;
    }

    private static <S> String entityType(Batch<S> batch, int cursor) {
        String type = null;
        for (var context = batch.canonical().getContext(); context != null; context = context.getChild()) {
            for (var parsed : context.getNodes()) {
                if (parsed.getRange().getEnd() <= cursor && "minecraft:entity_type".equals(ArgumentDocumentation.registry(parsed.getNode()))) {
                    type = ClientPlatform.identifierId(batch.text().canonical().substring(parsed.getRange().getStart(), parsed.getRange().getEnd()));
                }
            }
        }
        return type;
    }

    private static <S> CommandNode<S> suggestionNode(CommandNode<S> parent, String text, Batch<S> batch) {
        CommandNode<S> node = parent.getChild(text);
        if (node != null) { return node; }
        CommandAliases<S> aliases = batch.aliases();
        if (aliases != null) {
            for (var child : parent.getChildren()) {
                if (child instanceof LiteralCommandNode<S> literal && aliases.names(child, literal.getLiteral()).contains(text)) { return child; }
            }
        }
        var arguments = parent.getChildren().stream().filter(ArgumentCommandNode.class::isInstance).toList();
        if (arguments.size() == 1) { return arguments.get(0); }
        return null;
    }

}
