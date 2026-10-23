package io.github.kltyton.commandtranslation.client.command.candidate;

import com.mojang.brigadier.context.SuggestionContext;
import com.mojang.brigadier.tree.CommandNode;

final class SuggestionContexts {
    private SuggestionContexts() {}

    static <S> SuggestionContext<S> withParent(SuggestionContext<S> context, CommandNode<S> parent) {
        return new SuggestionContext<>(context.context, parent, context.startPos);
    }
}
