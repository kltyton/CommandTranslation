package io.github.kltyton.commandtranslation.client.screen.configuration;

import io.github.kltyton.commandtranslation.client.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

final class CommandConfigScreenView extends CommandConfigScreen {
    CommandConfigScreenView(Screen parent, ClientConfig draft, Page page) { super(parent, draft, page); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.nextStratum();
        paintSwatches(graphics::fill);
    }
}
