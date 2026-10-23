package io.github.kltyton.commandtranslation.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.kltyton.commandtranslation.client.screen.tooltip.PanelTooltipLine;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;

/** Keeps already ordered panel runs intact on Forge's native text tooltip path. */
@Mixin(ClientLanguage.class)
public abstract class TooltipLanguageMixin {
    @WrapMethod(method = "getVisualOrder(Lnet/minecraft/network/chat/FormattedText;)Lnet/minecraft/util/FormattedCharSequence;")
    private FormattedCharSequence commandTranslation$panelOrder(FormattedText text, Operation<FormattedCharSequence> original) {
        return text instanceof PanelTooltipLine line ? line : original.call(text);
    }
}
