package io.github.kltyton.commandtranslation.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.kltyton.commandtranslation.client.command.CommandQueueOwner;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Preserves the shared pre-signing chat transport and its slash-prefixed command format. */
@Mixin(LocalPlayer.class)
public abstract class ClientCommandSendingMixin {
    @Shadow @Final public ClientPacketListener connection;

    @WrapMethod(method = "chat")
    private void commandTranslation$chat(String message, Operation<Void> original) {
        var queue = ((CommandQueueOwner) connection).commandTranslation$commandQueue();
        if (message.startsWith("/")) {
            queue.send(connection, message.substring(1), text -> original.call("/" + text));
        } else { queue.sendChat(connection, message, text -> original.call(text)); }
    }
}
