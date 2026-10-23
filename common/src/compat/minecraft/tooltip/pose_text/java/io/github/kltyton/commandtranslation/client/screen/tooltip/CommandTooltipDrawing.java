package io.github.kltyton.commandtranslation.client.screen.tooltip;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;

public final class CommandTooltipDrawing {
    private CommandTooltipDrawing() {}

    public static void render(Screen screen, PoseStack pose, Font font, CommandTooltip.Document document, int mouseX, int mouseY) {
        for (var placement : TooltipPanels.arrange(document, font, screen.width, screen.height, mouseX, mouseY)) {
            var layout = TooltipPanels.measure(placement.panel(), font, placement.width());
            LegacyTooltipBridge.render(screen, pose, font, PanelTooltipLine.rows(layout),
                    placement.x() - 12, placement.y() + 12);
        }
    }
}
