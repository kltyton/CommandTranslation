package io.github.kltyton.commandtranslation.client.screen.tooltip;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;

final class LegacyTooltipBridge {
    private LegacyTooltipBridge() {}

    static void render(Screen screen, PoseStack pose, Font font, List<PanelTooltipLine> lines, int x, int y) {
        screen.renderTooltip(pose, lines, x, y);
    }
}
