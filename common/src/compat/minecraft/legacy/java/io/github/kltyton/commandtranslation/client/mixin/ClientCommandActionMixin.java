package io.github.kltyton.commandtranslation.client.mixin;

import io.github.kltyton.commandtranslation.client.ClientPlatform;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Preserves the vanilla unsigned-command validation path. */
@Mixin(ClientPacketListener.class)
public abstract class ClientCommandActionMixin {
    @ModifyVariable(method = "sendUnsignedCommand", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String commandTranslation$unsigned(String command) { return ClientPlatform.outgoing(command); }
}
