package io.github.kltyton.commandtranslation.client.screen.tooltip;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;

final class PoseTooltipContent implements ClientTooltipComponent {
    private final TooltipPanels.Panel panel;
    private final int width;
    private TooltipPanels.Layout layout;

    PoseTooltipContent(TooltipPanels.Panel panel, int width, Font font) {
        this.panel = panel;
        this.width = width;
        layout = TooltipPanels.measure(panel, font, width);
    }

    @Override public int getWidth(Font font) { layout = TooltipPanels.measure(panel, font, width); return layout.width(); }
    @Override public int getHeight() { return layout.height() + 2; }

    @Override
    public void renderText(Font font, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource buffers) {
        for (var run : layout.runs()) {
            PoseStack transform = new PoseStack();
            transform.last().pose().multiply(matrix);
            transform.translate(x + run.x(), y + run.y(), 0);
            transform.scale(run.scale(), run.scale(), 1);
            font.drawInBatch(run.text(), 0, 0, run.color(), true, transform.last().pose(), buffers, false, 0, 15728880);
        }
    }
}
