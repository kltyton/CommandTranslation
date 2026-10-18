package io.github.kltyton.commandtranslation.client.command.suggestion;

import net.minecraft.client.gui.components.EditBox;

public interface CommandInput {
    EditBox commandTranslation$input();
    void commandTranslation$refresh();
}
