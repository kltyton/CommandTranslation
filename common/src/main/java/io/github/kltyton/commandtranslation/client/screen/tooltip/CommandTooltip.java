package io.github.kltyton.commandtranslation.client.screen.tooltip;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

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
        var syntax = Component.empty();
        document.syntax().forEach(token -> syntax.append(Component.translatable(TOKEN, token.annotation(), token.original())));
        var parameters = Component.empty();
        document.parameters().forEach(parameter -> parameters.append(Component.translatable(PARAMETER, parameter.title(), parameter.description())));
        return Component.translatable(PANEL, document.title(), document.description(), syntax, parameters);
    }

    public static Document read(Component component) {
        if (component.getContents() instanceof TranslatableContents text) {
            Object[] args = text.getArgs();
            if (text.getKey().equals(PANEL) && args.length == 4 && args[0] instanceof Component title
                    && args[1] instanceof Component description && args[2] instanceof Component syntax && args[3] instanceof Component parameters) {
                List<Token> tokens = new ArrayList<>();
                for (Component part : syntax.getSiblings()) {
                    if (part.getContents() instanceof TranslatableContents token && token.getKey().equals(TOKEN)
                            && token.getArgs().length == 2 && token.getArgs()[0] instanceof Component annotation
                            && token.getArgs()[1] instanceof Component original) { tokens.add(new Token(original, annotation)); }
                }
                List<Parameter> panels = new ArrayList<>();
                for (Component part : parameters.getSiblings()) {
                    if (part.getContents() instanceof TranslatableContents parameter && parameter.getKey().equals(PARAMETER)
                            && parameter.getArgs().length == 2 && parameter.getArgs()[0] instanceof Component heading
                            && parameter.getArgs()[1] instanceof Component body) { panels.add(new Parameter(heading, body)); }
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
