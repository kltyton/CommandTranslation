package io.github.kltyton.commandtranslation.client.screen.tooltip;

import io.github.kltyton.commandtranslation.client.i18n.MinecraftText;
import io.github.kltyton.commandtranslation.client.i18n.TranslationContent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

/** Carries structured command help in ordinary translatable components. */
public final class CommandTooltip {
    public static final String PANEL = "command_translation.help.panel.command";
    public static final String TOKEN = "command_translation.help.panel.token";
    public static final String PARAMETER = "command_translation.help.panel.parameter";

    public record Token(Component original, Component annotation) {}
    public record Parameter(Component title, Component description) {}
    public record Document(Component title, Component description, List<Token> syntax, List<Parameter> parameters) {
        public Document { syntax = List.copyOf(syntax); parameters = List.copyOf(parameters); }
    }

    private CommandTooltip() {}

    public static Component component(Document document) {
        var syntax = MinecraftText.empty();
        document.syntax().forEach(token -> syntax.append(MinecraftText.translatable(TOKEN, token.annotation(), token.original())));
        var parameters = MinecraftText.empty();
        document.parameters().forEach(parameter -> parameters.append(MinecraftText.translatable(PARAMETER, parameter.title(), parameter.description())));
        return MinecraftText.translatable(PANEL, document.title(), document.description(), syntax, parameters);
    }

    public static Document read(Component component) {
        TranslationContent text = MinecraftText.translation(component);
        if (text != null) {
            Object[] args = text.args();
            if (text.key().equals(PANEL) && args.length == 4 && args[0] instanceof Component title
                    && args[1] instanceof Component description && args[2] instanceof Component syntax && args[3] instanceof Component parameters) {
                List<Token> tokens = new ArrayList<>();
                for (Component part : syntax.getSiblings()) {
                    TranslationContent token = MinecraftText.translation(part);
                    if (token != null && token.key().equals(TOKEN)
                            && token.args().length == 2 && token.args()[0] instanceof Component annotation
                            && token.args()[1] instanceof Component original) { tokens.add(new Token(original, annotation)); }
                }
                List<Parameter> panels = new ArrayList<>();
                for (Component part : parameters.getSiblings()) {
                    TranslationContent parameter = MinecraftText.translation(part);
                    if (parameter != null && parameter.key().equals(PARAMETER)
                            && parameter.args().length == 2 && parameter.args()[0] instanceof Component heading
                            && parameter.args()[1] instanceof Component body) { panels.add(new Parameter(heading, body)); }
                }
                var body = description.copy();
                for (Component sibling : component.getSiblings()) { body.append("\n").append(sibling); }
                return new Document(title, body, tokens, panels);
            }
            for (Object arg : args) {
                if (arg instanceof Component nested) {
                    Document document = read(nested);
                    if (document != null) { return document; }
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            Document document = read(sibling);
            if (document != null) { return document; }
        }
        return null;
    }
}
