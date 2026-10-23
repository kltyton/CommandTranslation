package io.github.kltyton.commandtranslation.client.screen.tooltip;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import org.joml.Vector2i;

public final class CommandTooltipDrawing {
    private CommandTooltipDrawing() {}

    public static void render(GuiGraphics graphics, Font font, CommandTooltip.Document document, int mouseX, int mouseY) {
        for (TooltipPanels.Placement placement : TooltipPanels.arrange(document, font, graphics.guiWidth(), graphics.guiHeight(), mouseX, mouseY)) {
            var content = new Content(placement.panel(), placement.width(), font);
            ((NativeTooltipAccess) graphics).commandTranslation$renderComponents(font, List.of(content), placement.x(), placement.y(),
                    (screenWidth, screenHeight, x, y, width, height) -> new Vector2i(x, y));
        }
    }

    private static final class Content implements ClientTooltipComponent {
        private final TooltipPanels.Panel panel;
        private final int width;
        private TooltipPanels.Layout layout;

        private Content(TooltipPanels.Panel panel, int width, Font font) {
            this.panel = panel;
            this.width = width;
            layout = TooltipPanels.measure(panel, font, width);
        }

        @Override public int getWidth(Font font) { layout = TooltipPanels.measure(panel, font, width); return layout.width(); }
        @Override public int getHeight() { return layout.height() + 2; }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
            for (TooltipPanels.Run run : layout.runs()) {
                graphics.pose().pushPose();
                try {
                    graphics.pose().translate(x + run.x(), y + run.y(), 0.0F);
                    graphics.pose().scale(run.scale(), run.scale(), 1.0F);
                    graphics.drawString(font, run.text(), 0, 0, run.color(), true);
                } finally { graphics.pose().popPose(); }
            }
        }
    }
}
