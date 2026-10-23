package io.github.kltyton.commandtranslation.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.kltyton.commandtranslation.client.command.CommandQueueOwner;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;

/** Resolves commands before vanilla signing while preserving order with ordinary chat. */
@Mixin(ClientPacketListener.class)
public abstract class ClientCommandSendingMixin {
    @WrapMethod(method = "sendCommand")
    private void commandTranslation$send(String command, Operation<Void> original) {
        ClientPacketListener connection = (ClientPacketListener) (Object) this;
        ((CommandQueueOwner) this).commandTranslation$commandQueue().send(connection, command, text -> original.call(text));
    }

    @WrapMethod(method = "sendChat")
    private void commandTranslation$chat(String message, Operation<Void> original) {
        ClientPacketListener connection = (ClientPacketListener) (Object) this;
        ((CommandQueueOwner) this).commandTranslation$commandQueue().sendChat(connection, message, text -> original.call(text));
    }
}
