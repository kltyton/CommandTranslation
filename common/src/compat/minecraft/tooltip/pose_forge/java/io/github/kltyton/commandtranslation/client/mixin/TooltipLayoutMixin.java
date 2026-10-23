package io.github.kltyton.commandtranslation.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kltyton.commandtranslation.client.command.suggestion.DocumentationTooltip;
import io.github.kltyton.commandtranslation.client.i18n.MinecraftText;
import io.github.kltyton.commandtranslation.client.screen.tooltip.CommandTooltip;
import io.github.kltyton.commandtranslation.client.screen.tooltip.CommandTooltipDrawing;
import io.github.kltyton.commandtranslation.client.screen.tooltip.ResourceTooltip;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserves Forge 1.16's component list and GuiUtils event path. */
@Mixin(Screen.class)
public abstract class TooltipLayoutMixin {
    @Shadow protected Font font;
    @Shadow public int width;

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
    private List<Component> commandTranslation$wrap(List<Component> original, @Local(argsOnly = true) Component component) {
        if (!DocumentationTooltip.containsDocumentation(component)) { return original; }
        List<Component> lines = new ArrayList<>();
        for (var split : font.getSplitter().splitLines(component, Math.max(1, Math.min(300, width - 16)), Style.EMPTY)) {
            var line = MinecraftText.empty();
            split.visit((style, text) -> { line.append(MinecraftText.literal(text).setStyle(style)); return Optional.empty(); }, Style.EMPTY);
            lines.add(line);
        }
        return lines;
    }
}
