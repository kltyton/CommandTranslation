package io.github.kltyton.commandtranslation.client.mixin;

import io.github.kltyton.commandtranslation.client.i18n.IdentifierNames;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Localizes structured command feedback while keeping links and message styles. */
@Mixin(ChatComponent.class)
public abstract class ClientFeedbackMixin {
    @ModifyVariable(method = "addServerSystemMessage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Component commandTranslation$feedback(Component message) { return IdentifierNames.feedback(message); }
}
