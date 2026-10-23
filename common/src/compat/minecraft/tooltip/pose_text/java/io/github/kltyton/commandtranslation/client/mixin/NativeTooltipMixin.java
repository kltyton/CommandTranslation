package io.github.kltyton.commandtranslation.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import io.github.kltyton.commandtranslation.client.screen.tooltip.PanelTooltipLine;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;

/** Adds measured text runs while leaving native tooltip backgrounds, buffers and events in control. */
@Mixin(Font.class)
public abstract class NativeTooltipMixin {
    @WrapMethod(method = "width(Lnet/minecraft/network/chat/FormattedText;)I")
    private int commandTranslation$componentWidth(FormattedText text, Operation<Integer> original) {
        return text instanceof PanelTooltipLine line ? line.width() : original.call(text);
    }

    @WrapMethod(method = "width(Lnet/minecraft/util/FormattedCharSequence;)I")
    private int commandTranslation$sequenceWidth(FormattedCharSequence text, Operation<Integer> original) {
        return text instanceof PanelTooltipLine line ? line.width() : original.call(text);
    }

    @WrapMethod(method = "drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;ZII)I")
    private int commandTranslation$drawRuns(FormattedCharSequence text, float x, float y, int color, boolean shadow,
            Matrix4f matrix, MultiBufferSource buffers, boolean seeThrough, int background, int light, Operation<Integer> original) {
        if (!(text instanceof PanelTooltipLine line)) {
            return original.call(text, x, y, color, shadow, matrix, buffers, seeThrough, background, light);
        }
        for (var run : line.runs()) {
            PoseStack transform = new PoseStack();
            transform.last().pose().multiply(matrix);
            transform.translate(x + run.x(), y + run.y(), 0);
            transform.scale(run.scale(), run.scale(), 1);
            original.call(run.text(), 0F, 0F, run.color(), shadow, transform.last().pose(), buffers, seeThrough, background, light);
        }
        return (int) x + line.width();
    }
}
