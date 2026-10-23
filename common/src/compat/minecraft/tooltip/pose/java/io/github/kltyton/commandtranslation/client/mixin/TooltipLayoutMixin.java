package io.github.kltyton.commandtranslation.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kltyton.commandtranslation.client.command.suggestion.DocumentationTooltip;
import io.github.kltyton.commandtranslation.client.screen.tooltip.CommandTooltip;
import io.github.kltyton.commandtranslation.client.screen.tooltip.CommandTooltipDrawing;
import io.github.kltyton.commandtranslation.client.screen.tooltip.ResourceTooltip;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps structured help on the native screen tooltip path before GuiGraphics. */
@Mixin(Screen.class)
public abstract class TooltipLayoutMixin {
    @Shadow protected Font font;
    @Shadow public int width;
    @Shadow public int height;

    @ModifyVariable(method = "renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/network/chat/Component;II)V",
            at = @At("HEAD"), argsOnly = true)
    private Component commandTranslation$itemDetails(Component component) { return ResourceTooltip.resolve(component); }

    @Inject(method = "renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/network/chat/Component;II)V",
            at = @At("HEAD"), cancellable = true)
    private void commandTranslation$panels(PoseStack pose, Component component, int mouseX, int mouseY, CallbackInfo callback) {
        var document = CommandTooltip.read(component);
        if (document == null) { return; }
        CommandTooltipDrawing.render((Screen) (Object) this, pose, font, document, mouseX, mouseY);
        callback.cancel();
    }

    @ModifyExpressionValue(method = "renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/network/chat/Component;II)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Arrays;asList([Ljava/lang/Object;)Ljava/util/List;", remap = false))
    private List<FormattedCharSequence> commandTranslation$wrap(List<FormattedCharSequence> original,
            @Local(argsOnly = true) Component component) {
        return DocumentationTooltip.containsDocumentation(component)
                ? DocumentationTooltip.wrap(font, component, width, height) : original;
    }
}
