package io.github.kltyton.commandtranslation.client.mixin;

import io.github.kltyton.commandtranslation.client.integration.forge.ClientCommandRegistration;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Executes only this mod's registered client command before the chat packet is sent. */
@Mixin(LocalPlayer.class)
public abstract class LegacyClientCommandMixin {
    @Shadow @Final public ClientPacketListener connection;

    @Inject(method = "chat", at = @At("HEAD"), cancellable = true)
    private void commandTranslation$localCommand(String message, CallbackInfo callback) {
        if (ClientCommandRegistration.execute(connection, message)) { callback.cancel(); }
    }
}
