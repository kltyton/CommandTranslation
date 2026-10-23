package io.github.kltyton.commandtranslation.client.screen.tooltip;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.fml.client.gui.GuiUtils;

final class LegacyTooltipBridge {
    private LegacyTooltipBridge() {}

    static void render(Screen screen, PoseStack pose, Font font, List<PanelTooltipLine> lines, int x, int y) {
        GuiUtils.drawHoveringText(pose, lines, x, y, screen.width, screen.height, -1, font);
    }
}
