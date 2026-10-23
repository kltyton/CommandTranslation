package io.github.kltyton.commandtranslation.client.mixin;

import io.github.kltyton.commandtranslation.client.integration.forge.ClientCommandRegistration;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the client command after the server has supplied its complete command tree. */
@Mixin(ClientPacketListener.class)
public abstract class ClientCommandBootstrapMixin {
    @Inject(method = "handleCommands", at = @At("RETURN"))
    private void commandTranslation$clientCommands(CallbackInfo callback) {
        ClientCommandRegistration.attach(((ClientPacketListener) (Object) this).getCommands());
    }
}
