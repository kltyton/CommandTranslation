package io.github.kltyton.commandtranslation.client.command.candidate;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import io.github.kltyton.commandtranslation.client.command.CommandHelp;
import io.github.kltyton.commandtranslation.client.command.CommandText;
import io.github.kltyton.commandtranslation.client.command.help.ArgumentDocumentation;
import io.github.kltyton.commandtranslation.client.command.suggestion.ProjectedSuggestion;
import io.github.kltyton.commandtranslation.client.command.suggestion.QueryProjection;
import io.github.kltyton.commandtranslation.client.config.ClientConfig;
import io.github.kltyton.commandtranslation.client.integration.nbt.NbtAutocompleteIntegration;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import net.minecraft.client.Minecraft;

/** Combines native prefix results with a contextual empty-prefix query for translated names. */
public final class LocalizedSuggestions {
    private record Choice(CommandNode<?> node, String token, StringRange range) {}
    private record Query(NativeSuggestionQuery.Result result, QueryProjection projection, boolean search) {}

    private LocalizedSuggestions() {}

    public static <S> CompletableFuture<Suggestions> complete(CommandDispatcher<S> dispatcher, ParseResults<S> parse,
            CommandText text, int commandStart, int cursor, BiFunction<ParseResults<S>, Integer, CompletableFuture<Suggestions>> provider,
            java.util.function.BooleanSupplier current, boolean localize) {
        int canonicalCursor = text.canonicalOffset(cursor);
        QueryProjection visible = new QueryProjection(text, cursor, cursor);
        return NativeSuggestionQuery.query(dispatcher, parse, canonicalCursor, provider, false).thenComposeAsync(nativeResult -> {
            if (!current.getAsBoolean()) { return CompletableFuture.completedFuture(new Suggestions(StringRange.at(cursor), List.of())); }
            int leaf = leafStart(parse, canonicalCursor);
            int sourceStart = text.originalOffset(leaf);
            if (!localize || sourceStart == cursor) {
                return CompletableFuture.completedFuture(combine(text, cursor, List.of(new Query(nativeResult, visible, false)), current, localize));
            }
            QueryProjection search = new QueryProjection(text, sourceStart, cursor);
            StringReader reader = new StringReader(search.input());
            reader.setCursor(text.canonicalOffset(commandStart));
            ParseResults<S> empty = LocalizedInput.parseForQuery(dispatcher, reader, parse.getContext().getSource(), search.cursor());
            return NativeSuggestionQuery.query(dispatcher, empty, search.cursor(), provider, true).thenApplyAsync(all ->
                    combine(text, cursor, List.of(new Query(nativeResult, visible, false), new Query(all, search, true)), current, localize), Minecraft.getInstance());
        }, Minecraft.getInstance());
    }

    private static Suggestions combine(CommandText text, int cursor, List<Query> queries, java.util.function.BooleanSupplier current, boolean localize) {
        if (!current.getAsBoolean()) { return new Suggestions(StringRange.at(cursor), List.of()); }
        List<Suggestion> rows = new ArrayList<>();
        Set<Choice> choices = new LinkedHashSet<>();
        Map<String, Set<String>> labels = new java.util.HashMap<>();
        for (Query query : queries) {
            for (var source : query.result().sources()) {
                CommandCandidate candidate = CommandCandidate.describe(source);
                labels.computeIfAbsent(candidate.label(), ignored -> new LinkedHashSet<>()).add(candidate.identifier());
            }
        }
        for (Query query : queries) {
            String search = text.original().substring(query.projection().toOriginal(StringRange.at(query.projection().cursor())).getStart(), cursor);
            Map<Suggestion, NativeSuggestionQuery.Source<?>> identities = new IdentityHashMap<>();
            for (var source : query.result().sources()) { identities.putIfAbsent(source.original(), source); }
            for (Suggestion nativeRow : query.result().suggestions().getList()) {
                NativeSuggestionQuery.Source<?> source = identities.get(nativeRow);
                if (source == null) { source = source(query.result(), nativeRow); }
                if (source == null) {
                    if (!query.search()) {
                        Suggestion row = new ProjectedSuggestion(nativeRow, query.projection(), nativeRow.getText(), nativeRow.getText(), java.util.function.UnaryOperator.identity());
                        rows.add(row);
                    }
                    continue;
                }
                CommandCandidate candidate = CommandCandidate.describe(source);
                if (query.search() && (candidate.aliases().isEmpty() || !candidate.matches(search))) { continue; }
                StringRange range = query.projection().toOriginal(nativeRow.getRange());
                if (!choices.add(new Choice(source.node(), candidate.canonical(), range))) { continue; }
                boolean localized = localize && !candidate.aliases().isEmpty();
                String insertion = localized ? candidate.insertion() : nativeRow.getText();
                String display = ClientConfig.current().identifierTranslation || localized ? candidate.label() : nativeRow.getText();
                boolean ambiguous = labels.getOrDefault(candidate.label(), Set.of()).size() > 1;
                if (ambiguous) {
                    display = candidate.disambiguatedInsertion();
                    insertion = localize ? candidate.disambiguatedInsertion() : nativeRow.getText();
                }
                NbtAutocompleteIntegration.restore(source.metadata(), nativeRow);
                java.util.function.UnaryOperator<com.mojang.brigadier.Message> tooltip = nativeTooltip -> {
                    com.mojang.brigadier.Message message = candidate.tooltip(nativeTooltip);
                    return ambiguous && message instanceof net.minecraft.network.chat.Component component
                            ? component.copy().append("(" + candidate.identifier() + ")") : message;
                };
                rows.add(new ProjectedSuggestion(nativeRow, query.projection(), nativeRow.getText(), nativeRow.getText(), tooltip));
                if (!display.equals(nativeRow.getText()) || !insertion.equals(nativeRow.getText())) {
                    rows.add(new ProjectedSuggestion(nativeRow, query.projection(), display, insertion, tooltip));
                }
            }
        }
        // Suggestions.merge expands the native wrappers once and retains their apply/compare behavior.
        Suggestions merged = Suggestions.merge(text.original(), rows.stream().map(row -> new Suggestions(row.getRange(), List.of(row))).toList());
        Map<Suggestion, NbtAutocompleteIntegration.Metadata> metadata = new IdentityHashMap<>();
        for (Suggestion row : merged.getList()) { metadata.put(row, NbtAutocompleteIntegration.snapshot(row)); }
        NbtAutocompleteIntegration.publish(metadata);
        return merged;
    }

    private static NativeSuggestionQuery.Source<?> source(NativeSuggestionQuery.Result result, Suggestion row) {
        NativeSuggestionQuery.Source<?> match = null;
        for (var source : result.sources()) {
            Suggestion original = source.original();
            if (original == row) { return source; }
            StringRange from = original.getRange();
            StringRange to = row.getRange();
            String input = source.context().getInput();
            if (to.getStart() <= from.getStart() && from.getEnd() <= to.getEnd() && to.getEnd() <= input.length()) {
                String expanded = input.substring(to.getStart(), from.getStart()) + original.getText() + input.substring(from.getEnd(), to.getEnd());
                if (expanded.equals(row.getText())) {
                    if (match != null && match.node() != source.node()) { return null; }
                    match = source;
                }
            }
        }
        return match == null ? null : withRow(match, row);
    }

    private static <S> NativeSuggestionQuery.Source<S> withRow(NativeSuggestionQuery.Source<S> source, Suggestion row) {
        return new NativeSuggestionQuery.Source<>(source.node(), source.context(), source.argumentStart(), row, source.metadata());
    }

    private static <S> int leafStart(ParseResults<S> parse, int cursor) {
        var context = parse.getContext().findSuggestionContext(cursor);
        int argumentStart = context.startPos;
        CommandNode<?> node = null;
        var parsed = CommandHelp.target(parse.getContext(), Math.max(argumentStart, cursor - 1));
        if (parsed != null && parsed.node() instanceof ArgumentCommandNode<?, ?> && parsed.range().getStart() == argumentStart) { node = parsed.node(); }
        if (node == null) {
            var children = context.parent.getChildren().stream().filter(ArgumentCommandNode.class::isInstance).toList();
            if (children.size() == 1) { node = children.get(0); }
        }
        if (node == null || argumentStart == cursor) { return argumentStart; }
        String argument = parse.getReader().getString().substring(argumentStart, cursor);
        var located = CommandCandidate.locate(argument, argument.length() - 1, ArgumentDocumentation.parser(node));
        if (located != null) { return argumentStart + located.start(); }
        char last = argument.charAt(argument.length() - 1);
        return "[{},=~:".indexOf(last) >= 0 ? cursor : argumentStart;
    }
}
