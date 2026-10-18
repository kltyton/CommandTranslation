package io.github.kltyton.commandtranslation.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import io.github.kltyton.commandtranslation.client.command.candidate.NativeSuggestionQuery;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives hidden queries independent transaction IDs so they cannot cancel the visible or outgoing command query. */
@Mixin(ClientSuggestionProvider.class)
public abstract class ClientSuggestionProviderMixin {
    @Shadow private int pendingSuggestionsId;
    @Shadow private CompletableFuture<Suggestions> pendingSuggestionsFuture;
    @Unique private int commandTranslation$transaction = Integer.MIN_VALUE;
    @Unique private final Map<Integer, CompletableFuture<Suggestions>> commandTranslation$pending = new HashMap<>();

    @WrapMethod(method = "customSuggestion")
    private CompletableFuture<Suggestions> commandTranslation$query(CommandContext<?> context, Operation<CompletableFuture<Suggestions>> original) {
        if (!NativeSuggestionQuery.hidden()) { return original.call(context); }
        int visibleId = pendingSuggestionsId;
        CompletableFuture<Suggestions> visibleFuture = pendingSuggestionsFuture;
        pendingSuggestionsId = commandTranslation$transaction++;
        pendingSuggestionsFuture = null;
        try {
            CompletableFuture<Suggestions> result = original.call(context);
            if (pendingSuggestionsFuture != null) {
                commandTranslation$pending.put(pendingSuggestionsId, pendingSuggestionsFuture);
            }
            return result;
        } finally {
            pendingSuggestionsId = visibleId;
            pendingSuggestionsFuture = visibleFuture;
        }
    }

    @Inject(method = "completeCustomSuggestions", at = @At("HEAD"))
    private void commandTranslation$complete(int transaction, Suggestions suggestions, CallbackInfo callback) {
        CompletableFuture<Suggestions> future = commandTranslation$pending.remove(transaction);
        if (future != null) { future.complete(suggestions); }
    }
}
