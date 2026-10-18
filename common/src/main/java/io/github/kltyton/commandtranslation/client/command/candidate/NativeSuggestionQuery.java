package io.github.kltyton.commandtranslation.client.command.candidate;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.context.SuggestionContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.github.kltyton.commandtranslation.client.integration.nbt.NbtAutocompleteIntegration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import net.minecraft.client.Minecraft;

/** Runs one native completion query and records the real per-node results it produced. */
public final class NativeSuggestionQuery {
    public record Source<S>(CommandNode<S> node, CommandContext<S> context, int argumentStart, Suggestion original,
                            NbtAutocompleteIntegration.Metadata metadata) {}

    public record Result(Suggestions suggestions, List<Source<?>> sources) {
        public Result {
            sources = List.copyOf(sources);
        }
    }

    private static final ThreadLocal<Integer> HIDDEN_DEPTH = ThreadLocal.withInitial(() -> 0);

    private NativeSuggestionQuery() {}

    public static <S> CompletableFuture<Result> query(CommandDispatcher<S> dispatcher, ParseResults<S> parse, int cursor,
                                                      BiFunction<ParseResults<S>, Integer, CompletableFuture<Suggestions>> provider,
                                                      boolean hidden) {
        Frame frame = new Frame();
        int previous = HIDDEN_DEPTH.get();
        if (hidden) { HIDDEN_DEPTH.set(previous + 1); }
        NbtAutocompleteIntegration.State state = hidden ? NbtAutocompleteIntegration.snapshot() : null;
        CompletableFuture<Suggestions> suggestions;
        try {
            var view = new CompletionContext<>(parse.getContext(), frame);
            suggestions = provider.apply(new ParseResults<>(view, parse.getReader(), parse.getExceptions()), cursor);
        } finally {
            if (state != null) { NbtAutocompleteIntegration.restore(state); }
            if (previous == 0) { HIDDEN_DEPTH.remove(); }
            else { HIDDEN_DEPTH.set(previous); }
        }
        CompletableFuture<List<Source<?>>> sources = frame.collect();
        return sources.thenCombine(suggestions, (list, merged) -> new Result(merged, list));
    }

    public static boolean hidden() {
        return HIDDEN_DEPTH.get() > 0;
    }

    /** A completion-only view. Providers always receive the original built context and original nodes. */
    private static final class CompletionContext<S> extends CommandContextBuilder<S> {
        private final CommandContextBuilder<S> original;
        private final Frame frame;

        CompletionContext(CommandContextBuilder<S> original, Frame frame) {
            super(original.getDispatcher(), original.getSource(), original.getRootNode(), original.getRange().getStart());
            this.original = original;
            this.frame = frame;
        }

        @Override public S getSource() { return original.getSource(); }
        @Override public CommandNode<S> getRootNode() { return original.getRootNode(); }
        @Override public Map<String, ParsedArgument<S, ?>> getArguments() { return original.getArguments(); }
        @Override public Command<S> getCommand() { return original.getCommand(); }
        @Override public List<ParsedCommandNode<S>> getNodes() { return original.getNodes(); }
        @Override public CommandContextBuilder<S> getChild() { return original.getChild(); }
        @Override public CommandContextBuilder<S> getLastChild() { return original.getLastChild(); }
        @Override public CommandDispatcher<S> getDispatcher() { return original.getDispatcher(); }
        @Override public StringRange getRange() { return original.getRange(); }
        @Override public CommandContext<S> build(String input) { return original.build(input); }
        @Override public CommandContextBuilder<S> copy() { return new CompletionContext<>(original.copy(), frame); }
        @Override public CommandContextBuilder<S> withSource(S source) { original.withSource(source); return this; }
        @Override public CommandContextBuilder<S> withCommand(Command<S> command) { original.withCommand(command); return this; }
        @Override public CommandContextBuilder<S> withArgument(String name, ParsedArgument<S, ?> argument) { original.withArgument(name, argument); return this; }
        @Override public CommandContextBuilder<S> withNode(CommandNode<S> node, StringRange range) { original.withNode(node, range); return this; }
        @Override public CommandContextBuilder<S> withChild(CommandContextBuilder<S> child) { original.withChild(child); return this; }

        @Override public SuggestionContext<S> findSuggestionContext(int cursor) {
            SuggestionContext<S> context = original.findSuggestionContext(cursor);
            List<CommandNode<S>> children = context.parent.getChildren().stream().map(node -> observe(node, frame)).toList();
            CommandNode<S> parent = new RootCommandNode<>() {
                @Override public Collection<CommandNode<S>> getChildren() { return children; }
            };
            return new SuggestionContext<>(parent, context.startPos);
        }
    }

    private static <S> CommandNode<S> observe(CommandNode<S> node, Frame frame) {
        if (node instanceof LiteralCommandNode<S> literal) {
            return new LiteralCommandNode<>(literal.getLiteral(), literal.getCommand(), literal.getRequirement(),
                    literal.getRedirect(), literal.getRedirectModifier(), literal.isFork()) {
                @Override public CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
                    var future = literal.listSuggestions(context, builder);
                    frame.observe(literal, context, builder.getStart(), future);
                    return future;
                }
            };
        }
        if (node instanceof ArgumentCommandNode<S, ?> argument) { return observeArgument(argument, frame); }
        // Other client-defined node kinds retain their native provider without inventing a parser for them.
        return node;
    }

    private static <S, T> CommandNode<S> observeArgument(ArgumentCommandNode<S, T> node, Frame frame) {
        return new ArgumentCommandNode<>(node.getName(), node.getType(), node.getCommand(), node.getRequirement(),
                node.getRedirect(), node.getRedirectModifier(), node.isFork(), node.getCustomSuggestions()) {
            @Override public CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) throws CommandSyntaxException {
                var future = node.listSuggestions(context, builder);
                frame.observe(node, context, builder.getStart(), future);
                return future;
            }
        };
    }

    private static <S> List<Source<?>> sources(CommandNode<S> node, CommandContext<S> context, int argumentStart,
                                               Suggestions suggestions) {
        List<Source<?>> result = new ArrayList<>();
        for (Suggestion suggestion : suggestions.getList()) {
            result.add(new Source<>(node, context, argumentStart, suggestion, NbtAutocompleteIntegration.snapshot(suggestion)));
        }
        return List.copyOf(result);
    }

    private static final class Frame {
        private final List<CompletableFuture<List<Source<?>>>> groups = new ArrayList<>();

        <S> void observe(CommandNode<S> node, CommandContext<S> context, int argumentStart,
                         CompletableFuture<Suggestions> future) {
            if (future.isDone()) {
                groups.add(future.thenApply(suggestions -> sources(node, context, argumentStart, suggestions)));
            } else {
                groups.add(future.thenApplyAsync(suggestions -> sources(node, context, argumentStart, suggestions), Minecraft.getInstance()));
            }
        }

        CompletableFuture<List<Source<?>>> collect() {
            CompletableFuture<List<Source<?>>> collected = CompletableFuture.completedFuture(List.of());
            for (CompletableFuture<List<Source<?>>> group : groups) {
                collected = collected.thenCombine(group, Frame::concat);
            }
            return collected;
        }

        private static List<Source<?>> concat(List<Source<?>> left, List<Source<?>> right) {
            List<Source<?>> merged = new ArrayList<>(left.size() + right.size());
            merged.addAll(left);
            merged.addAll(right);
            return List.copyOf(merged);
        }
    }
}
