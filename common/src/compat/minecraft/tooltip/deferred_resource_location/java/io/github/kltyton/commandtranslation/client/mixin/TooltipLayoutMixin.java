package io.github.kltyton.commandtranslation.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.kltyton.commandtranslation.client.command.suggestion.DocumentationTooltip;
import io.github.kltyton.commandtranslation.client.screen.tooltip.CommandTooltip;
import io.github.kltyton.commandtranslation.client.screen.tooltip.CommandTooltipDrawing;
import io.github.kltyton.commandtranslation.client.screen.tooltip.ResourceTooltip;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Schedules command panels through native rendering and wraps ordinary documentation. */
@Mixin(GuiGraphics.class)
public abstract class TooltipLayoutMixin {
    @ModifyVariable(method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IILnet/minecraft/resources/ResourceLocation;)V", at = @At("HEAD"), argsOnly = true)
    private Component commandTranslation$itemDetails(Component component) {
        return deferredTooltip == null ? ResourceTooltip.resolve(component) : component;
    }

    @Shadow private Runnable deferredTooltip;

    @Inject(method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IILnet/minecraft/resources/ResourceLocation;)V", at = @At("HEAD"), cancellable = true)
    private void commandTranslation$panels(Font font, Component component, int mouseX, int mouseY, ResourceLocation style, CallbackInfo callback) {
        var document = CommandTooltip.read(component);
        if (document == null) { return; }
        if (deferredTooltip == null) {
            deferredTooltip = () -> CommandTooltipDrawing.render((GuiGraphics) (Object) this, font, document, mouseX, mouseY, style == null ? null : style.toString());
        }
        callback.cancel();
    }
    @ModifyExpressionValue(method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IILnet/minecraft/resources/ResourceLocation;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/List;of(Ljava/lang/Object;)Ljava/util/List;", remap = false))
    private List<FormattedCharSequence> commandTranslation$wrap(List<FormattedCharSequence> original,
            @Local(argsOnly = true) Font font, @Local(argsOnly = true) Component component) {
        if (!DocumentationTooltip.containsDocumentation(component)) { return original; }
        GuiGraphics graphics = (GuiGraphics) (Object) this;
        return DocumentationTooltip.wrap(font, component, graphics.guiWidth(), graphics.guiHeight());
    }
}
