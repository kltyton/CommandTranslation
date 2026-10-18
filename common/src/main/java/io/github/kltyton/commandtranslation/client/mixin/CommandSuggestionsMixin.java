package io.github.kltyton.commandtranslation.client.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.kltyton.commandtranslation.client.command.CommandClient;
import io.github.kltyton.commandtranslation.client.command.suggestion.CommandInput;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import io.github.kltyton.commandtranslation.client.config.ClientConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps native parsing and suggestion providers on canonical command text. */
@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin implements CommandInput {
    @Shadow @Final private EditBox input;
    @Shadow private ParseResults<?> currentParse;
    @Shadow private boolean keepSuggestions;
    @Shadow public abstract void updateCommandInfo();
    @Unique private CommandClient.Batch<?> commandTranslation$batch;
    @Unique private Language commandTranslation$language;
    @Unique private CommandDispatcher<?> commandTranslation$dispatcher;
    @Unique private ClientConfig commandTranslation$config;

    @Override
    public EditBox commandTranslation$input() { return input; }

    @Override
    public void commandTranslation$refresh() {
        var connection = Minecraft.getInstance().getConnection();
        if (commandTranslation$language != Language.getInstance()
                || commandTranslation$dispatcher != (connection == null ? null : connection.getCommands())
                || commandTranslation$config != ClientConfig.current()) {
            updateCommandInfo();
        }
    }

    @Inject(method = "updateCommandInfo", at = @At("HEAD"))
    private void commandTranslation$refresh(CallbackInfo callback) {
        var connection = Minecraft.getInstance().getConnection();
        var dispatcher = connection == null ? null : connection.getCommands();
        if (commandTranslation$language != Language.getInstance() || commandTranslation$dispatcher != dispatcher
                || commandTranslation$config != ClientConfig.current()) {
            currentParse = null;
            keepSuggestions = false;
            commandTranslation$batch = null;
            commandTranslation$language = Language.getInstance();
            commandTranslation$dispatcher = dispatcher;
            commandTranslation$config = ClientConfig.current();
        }
    }

    @WrapOperation(method = "updateCommandInfo", at = @At(value = "INVOKE",
            target = "Lcom/mojang/brigadier/CommandDispatcher;parse(Lcom/mojang/brigadier/StringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/ParseResults;", remap = false))
    private <S> ParseResults<S> commandTranslation$parse(CommandDispatcher<S> dispatcher, StringReader reader, S source, Operation<ParseResults<S>> original) {
        var batch = CommandClient.parse(dispatcher, reader, source, input, () -> {
            currentParse = null;
            keepSuggestions = false;
            updateCommandInfo();
        }, (canonical, provider) -> original.call(dispatcher, canonical, provider));
        commandTranslation$batch = batch;
        return batch.displayed();
    }

    @WrapOperation(method = "updateCommandInfo", at = @At(value = "INVOKE",
            target = "Lcom/mojang/brigadier/CommandDispatcher;getCompletionSuggestions(Lcom/mojang/brigadier/ParseResults;I)Ljava/util/concurrent/CompletableFuture;", remap = false))
    @SuppressWarnings("unchecked")
    private <S> CompletableFuture<Suggestions> commandTranslation$complete(CommandDispatcher<S> dispatcher, ParseResults<S> parse, int cursor, Operation<CompletableFuture<Suggestions>> original) {
        return CommandClient.complete(dispatcher, parse, cursor, (CommandClient.Batch<S>) commandTranslation$batch,
                (canonical, position) -> original.call(dispatcher, canonical, position));
    }
}
