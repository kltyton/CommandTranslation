package io.github.kltyton.commandtranslation.client.mixin;

import io.github.kltyton.commandtranslation.client.ClientPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws input-token help after the native suggestion list. */
@Mixin(CommandSuggestions.class)
public abstract class InputHelpMixin {
    @Shadow @Final private EditBox input;
    @Inject(method = "render", at = @At("HEAD"))
    private void commandTranslation$refresh(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo callback) {
        ((io.github.kltyton.commandtranslation.client.command.suggestion.CommandInput) this).commandTranslation$refresh();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void commandTranslation$hover(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo callback) {
        int textX = input.getX() + (input.getWidth() - input.getInnerWidth()) / 2;
        var tooltip = ClientPlatform.inputTooltip(input, mouseX, mouseY, textX);
        if (tooltip != null) {
            var font = Minecraft.getInstance().font;
            graphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }
    }
}
