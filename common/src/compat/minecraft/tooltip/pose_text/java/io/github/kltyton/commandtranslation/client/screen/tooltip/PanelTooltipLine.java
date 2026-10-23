package io.github.kltyton.commandtranslation.client.screen.tooltip;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

/** Carries measured runs through the native 1.16 text-only tooltip renderer. */
public final class PanelTooltipLine extends TextComponent implements FormattedCharSequence {
    private final List<TooltipPanels.Run> runs;
    private final int width;

    private PanelTooltipLine(List<TooltipPanels.Run> runs, int width) {
        super(text(runs));
        this.runs = List.copyOf(runs);
        this.width = width;
    }

    public List<TooltipPanels.Run> runs() { return runs; }
    public int width() { return width; }
    @Override public PanelTooltipLine plainCopy() { return new PanelTooltipLine(runs, width); }
    @Override public FormattedCharSequence getVisualOrderText() { return this; }

    @Override
    public boolean accept(FormattedCharSink sink) {
        for (var run : runs) { if (!run.text().accept(sink)) { return false; } }
        return true;
    }

    public static List<PanelTooltipLine> rows(TooltipPanels.Layout layout) {
        int count = Math.max(1, (layout.height() + 9) / 10);
        List<PanelTooltipLine> lines = new ArrayList<>(count);
        for (int row = 0; row < count; row++) {
            List<TooltipPanels.Run> runs = new ArrayList<>();
            int baseline = row * 10 + (row == 0 ? 0 : 2);
            for (var run : layout.runs()) {
                if (run.y() / 10 == row) {
                    runs.add(new TooltipPanels.Run(run.text(), run.x(), run.y() - baseline, run.scale(), run.color()));
                }
            }
            lines.add(new PanelTooltipLine(runs, layout.width()));
        }
        return lines;
    }

    private static String text(List<TooltipPanels.Run> runs) {
        StringBuilder text = new StringBuilder();
        for (var run : runs) {
            if (!text.isEmpty()) { text.append(' '); }
            run.text().accept((index, style, codePoint) -> { text.appendCodePoint(codePoint); return true; });
        }
        return text.toString();
    }
}
