package io.github.kltyton.commandtranslation.client.screen.tooltip;

import io.github.kltyton.commandtranslation.client.i18n.MinecraftText;
import io.github.kltyton.commandtranslation.client.config.ClientConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Keeps command syntax prominent and packs smaller parameter panels beside it. */
public final class TooltipPanels {
    private static final int GAP = 10;
    private static final int SYNTAX_GAP = 3;
    private static final float ANNOTATION_SCALE = 0.65F;
    public record Run(FormattedCharSequence text, int x, int y, float scale, int color) {}
    public record Layout(int width, int height, List<Run> runs) {
        public Layout { runs = List.copyOf(runs); }
    }
    public record Panel(Component title, Component description, List<CommandTooltip.Token> syntax, float scale) {}
    public record Placement(Panel panel, int width, int x, int y) {}
    private record Cell(List<FormattedCharSequence> original, List<FormattedCharSequence> annotation, int width) {}
    private record Packing(List<Placement> placements, int width, int height) {}

    private TooltipPanels() {}

    public static List<Placement> arrange(CommandTooltip.Document document, Font font, int width, int height, int mouseX, int mouseY) {
        int availableWidth = Math.max(1, width - 16);
        int availableHeight = Math.max(1, height - 16);
        Panel main = new Panel(document.title(), document.description(), document.syntax(), 1.0F);
        float secondaryScale = ClientConfig.current().compactParameterTooltips ? 0.8F : 1.0F;
        List<Panel> parameters = document.parameters().stream()
                .map(parameter -> new Panel(parameter.title(), parameter.description(), List.of(), secondaryScale)).toList();
        int mainWidth = contentWidth(main, font, Math.min(288, availableWidth));
        int secondaryWidth = parameters.stream().mapToInt(panel -> contentWidth(panel, font,
                Math.min(ClientConfig.current().compactParameterTooltips ? 168 : 240, availableWidth))).max().orElse(0);
        Packing best = pack(main, parameters, font, mainWidth, secondaryWidth, availableWidth, availableHeight);
        int preferredX = mouseX + 12 + best.width() <= width - 8 ? mouseX + 12 : mouseX - best.width() - 12;
        int preferredY = mouseY + 12 + best.height() <= height - 8 ? mouseY + 12 : mouseY - best.height() - 12;
        int originX = Math.max(8, Math.min(preferredX, width - best.width() - 8));
        int originY = Math.max(8, Math.min(preferredY, height - best.height() - 8));
        List<Placement> placements = new ArrayList<>();
        for (Placement placement : best.placements()) {
            placements.add(new Placement(placement.panel(), placement.width(), originX + placement.x(), originY + placement.y()));
        }
        return placements;
    }

    private static int contentWidth(Panel panel, Font font, int maximum) {
        int textWidth = (int) Math.ceil(Math.max(font.width(panel.title()), font.width(panel.description())) * panel.scale());
        List<CommandTooltip.Token> syntax = syntaxTokens(panel.syntax());
        int syntaxWidth = syntax.stream().mapToInt(token -> Math.max(font.width(token.original()),
                (int) Math.ceil(font.width(token.annotation()) * ANNOTATION_SCALE))).sum()
                + Math.max(0, syntax.size() - 1) * SYNTAX_GAP;
        return Math.max(1, Math.min(maximum, Math.max(80, Math.max(textWidth, syntaxWidth))));
    }

    private static Packing pack(Panel main, List<Panel> parameters, Font font, int mainWidth, int secondaryWidth,
            int availableWidth, int availableHeight) {
        List<Placement> placements = new ArrayList<>();
        placements.add(new Placement(main, mainWidth, 0, 0));
        int mainHeight = measure(main, font, mainWidth).height();
        int x = mainWidth + GAP;
        int y = 0;
        if (x + secondaryWidth > availableWidth) { x = 0; y = mainHeight + GAP; }
        int width = mainWidth;
        int height = mainHeight;
        for (Panel panel : parameters) {
            int panelHeight = measure(panel, font, secondaryWidth).height();
            if (y > 0 && y + panelHeight > availableHeight && x + secondaryWidth * 2 + GAP <= availableWidth) {
                x += secondaryWidth + GAP;
                y = 0;
            }
            placements.add(new Placement(panel, secondaryWidth, x, y));
            width = Math.max(width, x + secondaryWidth);
            height = Math.max(height, y + panelHeight);
            y += panelHeight + GAP;
        }
        return new Packing(placements, width, height);
    }

    public static Layout measure(Panel panel, Font font, int width) {
        List<Run> runs = new ArrayList<>();
        int y = text(runs, font, panel.title(), width, 0, panel.scale(), 0xFFFFCC55);
        if (!panel.description().getString().isEmpty()) { y = text(runs, font, panel.description(), width, y + 2, panel.scale(), 0xFFF0F0F0); }
        if (!panel.syntax().isEmpty()) {
            y += 5;
            List<Cell> line = new ArrayList<>();
            int x = 0;
            for (CommandTooltip.Token token : syntaxTokens(panel.syntax())) {
                String original = token.original().getString();
                String annotation = token.annotation().getString();
                boolean annotated = !annotation.isBlank() && !annotation.equalsIgnoreCase(original.replace("/", "").replace("<", "").replace(">", ""));
                List<FormattedCharSequence> originals = original.isBlank() ? List.of(token.original().getVisualOrderText())
                        : font.split(token.original(), Math.max(1, width));
                List<FormattedCharSequence> annotations = annotated
                        ? font.split(token.annotation(), Math.max(1, (int) (width / ANNOTATION_SCALE))) : List.of();
                int originalWidth = originals.stream().mapToInt(font::width).max().orElse(0);
                int annotationWidth = (int) Math.ceil(annotations.stream().mapToInt(font::width).max().orElse(0) * ANNOTATION_SCALE);
                int cellWidth = Math.max(originalWidth, annotationWidth);
                int gap = line.isEmpty() ? 0 : SYNTAX_GAP;
                if (!line.isEmpty() && x + gap + cellWidth > width) { y = syntaxLine(runs, font, line, y); line.clear(); x = 0; gap = 0; }
                line.add(new Cell(originals, annotations, cellWidth));
                x += gap + cellWidth;
            }
            if (!line.isEmpty()) { y = syntaxLine(runs, font, line, y); }
        }
        return new Layout(width, Math.max(font.lineHeight, y), runs);
    }

    private static List<CommandTooltip.Token> syntaxTokens(List<CommandTooltip.Token> tokens) {
        List<CommandTooltip.Token> result = new ArrayList<>();
        StringBuilder prefix = new StringBuilder();
        for (CommandTooltip.Token token : tokens) {
            String raw = token.original().getString();
            if (raw.isBlank()) { continue; }
            if (raw.equals("[") || raw.equals("(")) { prefix.append(raw); continue; }
            if (raw.equals("|")) { prefix.append("| "); continue; }
            if ((raw.equals("]") || raw.equals(")")) && !result.isEmpty()) {
                var previous = result.remove(result.size() - 1);
                result.add(new CommandTooltip.Token(previous.original().copy().append(raw), previous.annotation()));
                continue;
            }
            String bare = raw.replace("/", "").replace("<", "").replace(">", "");
            Component annotation = token.annotation().getString().equalsIgnoreCase(bare) ? MinecraftText.empty() : token.annotation();
            result.add(new CommandTooltip.Token(MinecraftText.literal(prefix.toString()).append(token.original()), annotation));
            prefix.setLength(0);
        }
        if (!prefix.isEmpty()) {
            result.add(new CommandTooltip.Token(MinecraftText.literal(prefix.toString()), MinecraftText.empty()));
        }
        return result;
    }

    private static int text(List<Run> runs, Font font, Component component, int width, int y, float scale, int color) {
        for (FormattedCharSequence line : font.split(component, Math.max(1, (int) (width / scale)))) {
            runs.add(new Run(line, 0, y, scale, color));
            y += (int) Math.ceil(font.lineHeight * scale);
        }
        return y;
    }

    private static int syntaxLine(List<Run> runs, Font font, List<Cell> cells, int y) {
        int annotationHeight = cells.stream().mapToInt(cell -> (int) Math.ceil(cell.annotation().size() * font.lineHeight * ANNOTATION_SCALE)).max().orElse(0);
        if (annotationHeight > 0) { annotationHeight++; }
        int originalHeight = cells.stream().mapToInt(cell -> cell.original().size() * font.lineHeight).max().orElse(font.lineHeight);
        int x = 0;
        for (Cell cell : cells) {
            int row = 0;
            for (FormattedCharSequence annotation : cell.annotation()) {
                int offset = Math.max(0, (cell.width() - (int) Math.ceil(font.width(annotation) * ANNOTATION_SCALE)) / 2);
                runs.add(new Run(annotation, x + offset, y + (int) Math.ceil(row++ * font.lineHeight * ANNOTATION_SCALE), ANNOTATION_SCALE, 0xFFAAAAAA));
            }
            row = 0;
            for (FormattedCharSequence original : cell.original()) {
                int offset = Math.max(0, (cell.width() - font.width(original)) / 2);
                runs.add(new Run(original, x + offset, y + annotationHeight + row++ * font.lineHeight, 1.0F, 0xFFFFFFFF));
            }
            x += cell.width() + SYNTAX_GAP;
        }
        return y + annotationHeight + originalHeight + 2;
    }
}
