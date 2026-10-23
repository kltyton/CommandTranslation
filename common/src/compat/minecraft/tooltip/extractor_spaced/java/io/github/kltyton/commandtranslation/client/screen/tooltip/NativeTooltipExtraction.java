package io.github.kltyton.commandtranslation.client.screen.tooltip;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.Identifier;
import org.joml.Vector2i;

final class NativeTooltipExtraction {
    private NativeTooltipExtraction() {}

    static void draw(GuiGraphicsExtractor graphics, Font font, ClientTooltipComponent content, int left, int top, Identifier style) {
        graphics.tooltip(font, List.of(content), left, top,
                (screenWidth, screenHeight, x, y, width, height) -> new Vector2i(x, y), style, false);
    }
}
