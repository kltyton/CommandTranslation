package io.github.kltyton.commandtranslation.client.command.candidate;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import io.github.kltyton.commandtranslation.client.command.CommandClient;
import io.github.kltyton.commandtranslation.client.command.CommandText;
import io.github.kltyton.commandtranslation.client.command.suggestion.QueryProjection;
import io.github.kltyton.commandtranslation.client.i18n.NamespaceNames;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;

/** Resolves qualified localized tokens only against the provider for that exact command position. */
public final class LocalizedInput {
    private static Language language;
    private static Object connection;
    private static Pattern qualifiers;

    private LocalizedInput() {}

    public static <S> CompletableFuture<CommandText> resolve(CommandDispatcher<S> dispatcher, CommandText text, int start, S source,
            BiFunction<ParseResults<S>, Integer, CompletableFuture<Suggestions>> provider) {
        return resolve(dispatcher, text, start, source, provider, false, () -> true);
    }

    public static <S> CompletableFuture<CommandText> resolve(CommandDispatcher<S> dispatcher, CommandText text, int start, S source,
            BiFunction<ParseResults<S>, Integer, CompletableFuture<Suggestions>> provider, boolean sending, java.util.function.BooleanSupplier current) {
        return next(dispatcher, text, start, source, provider, start, sending, current);
    }

    private static <S> CompletableFuture<CommandText> next(CommandDispatcher<S> dispatcher, CommandText text, int commandStart, S source,
            BiFunction<ParseResults<S>, Integer, CompletableFuture<Suggestions>> provider, int position, boolean sending, java.util.function.BooleanSupplier current) {
        if (!current.getAsBoolean()) { return CompletableFuture.completedFuture(text); }
        Matcher marker = qualifiers().matcher(text.original());
        while (marker.find(position)) {
            int markerStart = marker.start();
            int next = marker.end();
            int tokenStart = markerStart;
            boolean replaced = text.replacements().stream().anyMatch(r -> tokenStart >= r.start() && tokenStart < r.end());
            if (replaced) { position = next; continue; }
            if (markerStart > 0 && (text.original().charAt(markerStart - 1) == '"' || text.original().charAt(markerStart - 1) == '\'')) {
                markerStart--;
            }
            final int queryStart = markerStart;
            QueryProjection projection = new QueryProjection(text, queryStart, queryStart);
            StringReader reader = new StringReader(projection.input());
            reader.setCursor(text.canonicalOffset(commandStart));
            ParseResults<S> parse = parseForQuery(dispatcher, reader, source, projection.cursor());
            return NativeSuggestionQuery.query(dispatcher, parse, projection.cursor(), provider, true)
                    .thenComposeAsync(result -> {
                        if (!current.getAsBoolean()) { return CompletableFuture.completedFuture(text); }
                        Map<String, CommandText.Replacement> matches = new LinkedHashMap<>();
                        int longest = -1;
                        for (var nativeSource : result.sources()) {
                            CommandCandidate candidate = CommandCandidate.describe(nativeSource);
                            int from = candidate.tokenRange().getStart() > projection.cursor() ? queryStart
                                    : projection.toOriginal(com.mojang.brigadier.context.StringRange.at(candidate.tokenRange().getStart())).getStart();
                            int canonicalEnd = match(text.original(), from, CommandCandidate.decode(candidate.canonical()));
                            if (canonicalEnd >= 0 && text.original().substring(from, canonicalEnd).equals(candidate.canonical())) {
                                return next(dispatcher, text, commandStart, source, provider, canonicalEnd, sending, current);
                            }
                            for (String alias : candidate.aliases()) {
                                int end = match(text.original(), from, alias);
                                if (end < 0) { continue; }
                                if (end > longest) { matches.clear(); longest = end; }
                                if (end == longest) {
                                    if (nativeSource.node() instanceof com.mojang.brigadier.tree.LiteralCommandNode<?>
                                            && nativeSource.argumentStart() == text.canonicalOffset(commandStart)) {
                                        String canonical = candidate.canonical();
                                        if (canonical.startsWith("minecraft:")) { matches.remove(canonical.substring(10)); }
                                        else if (matches.containsKey("minecraft:" + canonical)) { continue; }
                                    }
                                    matches.putIfAbsent(candidate.canonical(), new CommandText.Replacement(from, end, candidate.canonical()));
                                }
                            }
                        }
                        if (matches.size() > 1 && sending) { return CompletableFuture.failedFuture(new IllegalArgumentException("Ambiguous localized argument")); }
                        if (matches.size() != 1) { return next(dispatcher, text, commandStart, source, provider, next, sending, current); }
                        CommandText.Replacement replacement = matches.values().iterator().next();
                        if (text.original().substring(replacement.start(), replacement.end()).equals(replacement.value())) {
                            return next(dispatcher, text, commandStart, source, provider, replacement.end(), sending, current);
                        }
                        List<CommandText.Replacement> changes = new ArrayList<>(text.replacements());
                        changes.add(replacement);
                        CommandText resolved = new CommandText(text.original(), changes);
                        CommandText fixed = CommandClient.translate(dispatcher, resolved.canonical(), resolved.canonicalOffset(commandStart), source);
                        for (var literal : fixed.replacements()) {
                            int from = resolved.originalOffset(literal.start());
                            int to = resolved.originalOffset(literal.end());
                            changes.add(new CommandText.Replacement(from, to, literal.value()));
                        }
                        return next(dispatcher, new CommandText(text.original(), changes), commandStart, source, provider, replacement.end(), sending, current);
                    }, Minecraft.getInstance());
        }
        return CompletableFuture.completedFuture(text);
    }

    public static <S> ParseResults<S> parseForQuery(CommandDispatcher<S> dispatcher, StringReader reader, S source, int cursor) {
        ParseResults<S> parse = dispatcher.parse(reader, source);
        var target = io.github.kltyton.commandtranslation.client.command.CommandHelp.target(parse.getContext(), cursor);
        if (target != null && io.github.kltyton.commandtranslation.client.command.CommandHelp.canonicalRoot(target.command()).equals("help")
                && target.argument().equals("command") && target.range().getStart() <= cursor) {
            StringReader query = new StringReader(reader.getString());
            query.setCursor(target.range().getStart());
            return dispatcher.parse(query, source);
        }
        return parse;
    }

    private static int match(String input, int from, String alias) {
        if (from >= input.length()) { return -1; }
        char quote = input.charAt(from);
        if (quote == '"' || quote == '\'') {
            StringReader reader = new StringReader(input);
            reader.setCursor(from);
            try { return reader.readString().equals(alias) ? reader.getCursor() : -1; }
            catch (com.mojang.brigadier.exceptions.CommandSyntaxException incomplete) { return -1; }
        }
        if (!input.startsWith(alias, from)) { return -1; }
        int end = from + alias.length();
        return end == input.length() || Character.isWhitespace(input.charAt(end)) || "[,{}]=~:;".indexOf(input.charAt(end)) >= 0 ? end : -1;
    }

    private static Pattern qualifiers() {
        var current = Minecraft.getInstance().getConnection();
        if (qualifiers == null || language != Language.getInstance() || connection != current) {
            language = Language.getInstance();
            connection = current;
            java.util.Set<String> namespaces = new java.util.LinkedHashSet<>();
            namespaces.add("minecraft");
            BuiltInRegistries.ITEM.keySet().forEach(id -> namespaces.add(id.getNamespace()));
            BuiltInRegistries.BLOCK.keySet().forEach(id -> namespaces.add(id.getNamespace()));
            BuiltInRegistries.ENTITY_TYPE.keySet().forEach(id -> namespaces.add(id.getNamespace()));
            if (current != null) {
                current.registryAccess().registries().forEach(entry -> entry.value().keySet().forEach(id -> namespaces.add(id.getNamespace())));
            }
            String translated = namespaces.stream().map(id -> NamespaceNames.name(id).getString()).distinct()
                    .sorted(java.util.Comparator.comparingInt(String::length).reversed()).map(Pattern::quote)
                    .collect(java.util.stream.Collectors.joining("|"));
            qualifiers = Pattern.compile("(?:(?<=^/)|(?<![\\p{L}\\p{N}_./-]))(?:#|!)?(?:" + translated + "|[\\p{L}\\p{N}_.-]+):");
        }
        return qualifiers;
    }
}
