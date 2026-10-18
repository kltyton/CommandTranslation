package io.github.kltyton.commandtranslation.client.screen.configuration;

import io.github.kltyton.commandtranslation.client.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

final class CommandConfigScreenView extends CommandConfigScreen {
    CommandConfigScreenView(Screen parent, ClientConfig draft, Page page) { super(parent, draft, page); }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        paintSwatches(graphics::fill);
    }
}
