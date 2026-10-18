package io.github.kltyton.commandtranslation.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.kltyton.commandtranslation.client.command.CommandSendQueue;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Canonicalizes before vanilla permission checks, argument signing and packet creation. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Unique private final CommandSendQueue commandTranslation$commands = new CommandSendQueue();

    @WrapMethod(method = "sendCommand")
    private void commandTranslation$send(String command, Operation<Void> original) {
        commandTranslation$commands.send((ClientPacketListener) (Object) this, command, canonical -> original.call(canonical));
    }

    @Inject(method = "handleUpdateTags", at = @At("RETURN"))
    private void commandTranslation$tags(CallbackInfo callback) {
        io.github.kltyton.commandtranslation.client.i18n.IdentifierNames.clearNameIndex();
        io.github.kltyton.commandtranslation.client.command.CommandClient.invalidateNames();
    }
}
