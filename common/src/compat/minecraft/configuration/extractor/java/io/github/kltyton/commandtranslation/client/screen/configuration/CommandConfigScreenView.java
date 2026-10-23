package io.github.kltyton.commandtranslation.client.screen.configuration;

import io.github.kltyton.commandtranslation.client.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.AbstractWidget;

final class CommandConfigScreenView extends CommandConfigScreen {
    CommandConfigScreenView(Screen parent, ClientConfig draft, Page page) { super(parent, draft, page); }

    @Override protected void addControl(AbstractWidget widget) { addRenderableWidget(widget); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.nextStratum();
        paintSwatches(graphics::fill);
        paintLabels((text, x, y) -> graphics.text(font, text, x, y, 0xFFFFFFFF, false));
    }
}
