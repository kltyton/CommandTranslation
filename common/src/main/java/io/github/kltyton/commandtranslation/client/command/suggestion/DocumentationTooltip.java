package io.github.kltyton.commandtranslation.client.command.suggestion;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;

public final class DocumentationTooltip {
    private DocumentationTooltip() {}

    public static List<FormattedCharSequence> wrap(Font font, Component component, int width, int height) {
        int availableWidth = Math.max(1, width - 16);
        int lineWidth = Math.min(300, availableWidth);
        List<FormattedCharSequence> lines = font.split(component, lineWidth);
        while (lineWidth < availableWidth && lines.size() * font.lineHeight + 2 > height - 16) {
            lineWidth = Math.min(availableWidth, lineWidth + 80);
            lines = font.split(component, lineWidth);
        }
        return lines;
    }

    public static boolean containsDocumentation(Component component) {
        if (component.getContents() instanceof TranslatableContents translation) {
            if (translation.getKey().startsWith("command_translation.")) { return true; }
            for (Object argument : translation.getArgs()) {
                if (argument instanceof Component nested && containsDocumentation(nested)) { return true; }
            }
        }
        return component.getSiblings().stream().anyMatch(DocumentationTooltip::containsDocumentation);
    }
}
