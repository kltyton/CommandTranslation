package io.github.kltyton.commandtranslation.client.screen.tooltip;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.Identifier;
import org.joml.Vector2i;

public final class CommandTooltipDrawing {
    private CommandTooltipDrawing() {}

    public static void render(GuiGraphicsExtractor graphics, Font font, CommandTooltip.Document document, int mouseX, int mouseY, Identifier style) {
        for (TooltipPanels.Placement placement : TooltipPanels.arrange(document, font, graphics.guiWidth(), graphics.guiHeight(), mouseX, mouseY)) {
            var content = new Content(placement.panel(), placement.width());
            graphics.tooltip(font, List.of(content), placement.x(), placement.y(),
                    (screenWidth, screenHeight, x, y, width, height) -> new Vector2i(x, y), style);
        }
    }

    private record Content(TooltipPanels.Panel panel, int width) implements ClientTooltipComponent {
        @Override public int getWidth(Font font) { return width; }
        @Override public int getHeight(Font font) { return TooltipPanels.measure(panel, font, width).height() + 2; }

        @Override
        public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
            for (TooltipPanels.Run run : TooltipPanels.measure(panel, font, width).runs()) {
                graphics.pose().pushMatrix();
                try {
                    graphics.pose().translate(x + run.x(), y + run.y());
                    graphics.pose().scale(run.scale());
                    graphics.text(font, run.text(), 0, 0, run.color(), true);
                } finally { graphics.pose().popMatrix(); }
            }
        }
    }
}
