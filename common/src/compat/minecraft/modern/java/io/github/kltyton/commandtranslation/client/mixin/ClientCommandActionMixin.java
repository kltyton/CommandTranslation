package io.github.kltyton.commandtranslation.client.mixin;

import io.github.kltyton.commandtranslation.client.ClientPlatform;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Preserves the vanilla confirmation and restricted-command checks. */
@Mixin(ClientPacketListener.class)
public abstract class ClientCommandActionMixin {
    @ModifyVariable(method = "sendUnattendedCommand", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String commandTranslation$unattended(String command) { return ClientPlatform.outgoing(command); }
}
