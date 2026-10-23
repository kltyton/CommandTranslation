package io.github.kltyton.commandtranslation.client.mixin;

import io.github.kltyton.commandtranslation.client.command.CommandSendQueue;
import io.github.kltyton.commandtranslation.client.command.CommandQueueOwner;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Owns outgoing order and invalidates localized names after server tags change. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin implements CommandQueueOwner {
    @Unique private final CommandSendQueue commandTranslation$commands = new CommandSendQueue();

    @Override public CommandSendQueue commandTranslation$commandQueue() { return commandTranslation$commands; }

    @Inject(method = "handleUpdateTags", at = @At("RETURN"))
    private void commandTranslation$tags(CallbackInfo callback) {
        io.github.kltyton.commandtranslation.client.i18n.IdentifierNames.clearNameIndex();
        io.github.kltyton.commandtranslation.client.command.CommandClient.invalidateNames();
    }
}
