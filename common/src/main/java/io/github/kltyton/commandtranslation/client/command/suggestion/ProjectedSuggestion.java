package io.github.kltyton.commandtranslation.client.command.suggestion;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import io.github.kltyton.commandtranslation.client.integration.nbt.NbtAutocompleteIntegration;

/** Wraps a native suggestion computed against a deletion query so it displays and edits the source command. */
public final class ProjectedSuggestion extends Suggestion implements DelegatingSuggestion {
    private final Suggestion original;
    private final QueryProjection projection;
    private final String displayText;
    private final String localizedInsertion;
    private final java.util.function.UnaryOperator<Message> tooltips;
    private final Suggestion metadataSource;

    public ProjectedSuggestion(Suggestion original, QueryProjection projection, String displayText, String localizedInsertion, java.util.function.UnaryOperator<Message> tooltips) {
        this(original, projection, displayText, localizedInsertion, tooltips, original);
    }

    private ProjectedSuggestion(Suggestion original, QueryProjection projection, String displayText, String localizedInsertion, java.util.function.UnaryOperator<Message> tooltips, Suggestion metadataSource) {
        super(projection.toOriginal(original.getRange()), displayText, tooltips.apply(original.getTooltip()));
        this.original = original;
        this.projection = projection;
        this.displayText = displayText;
        this.localizedInsertion = localizedInsertion;
        this.tooltips = tooltips;
        this.metadataSource = metadataSource;
        NbtAutocompleteIntegration.preserve(metadataSource, this);
    }

    @Override
    public Suggestion commandTranslation$original() {
        return original;
    }

    @Override
    public String getText() {
        return displayText;
    }

    @Override
    public Message getTooltip() {
        return tooltips.apply(original.getTooltip());
    }

    @Override
    public String apply(String input) {
        String query = projection.input();
        StringRange range = original.getRange();
        String applied = original.apply(query);
        String prefix = query.substring(0, range.getStart());
        String suffix = query.substring(range.getEnd());
        if (!applied.startsWith(prefix) || !applied.endsWith(suffix) || applied.length() < prefix.length() + suffix.length()) {
            return applied;
        }
        String replacement = applied.substring(prefix.length(), applied.length() - suffix.length());
        String insertion = replacement.equals(original.getText()) ? localizedInsertion : replacement;
        StringRange source = projection.toOriginal(range);
        return input.substring(0, source.getStart()) + insertion + input.substring(source.getEnd());
    }

    @Override
    public int compareTo(Suggestion other) {
        int order = original.compareTo(DelegatingSuggestion.unwrap(other));
        return order != 0 ? order : variantOrder(other, false);
    }

    @Override
    public int compareToIgnoreCase(Suggestion other) {
        int order = original.compareToIgnoreCase(DelegatingSuggestion.unwrap(other));
        return order != 0 ? order : variantOrder(other, true);
    }

    private int variantOrder(Suggestion other, boolean ignoreCase) {
        if (!(other instanceof ProjectedSuggestion projected)) { return 0; }
        boolean canonical = displayText.equals(original.getText());
        boolean otherCanonical = projected.displayText.equals(projected.original.getText());
        if (canonical != otherCanonical) { return canonical ? -1 : 1; }
        return ignoreCase ? displayText.compareToIgnoreCase(projected.displayText) : displayText.compareTo(projected.displayText);
    }

    @Override
    public Suggestion expand(String command, StringRange sourceRange) {
        Suggestion expanded = original.expand(projection.input(), projection.toQuery(sourceRange));
        if (sourceRange.equals(getRange()) && expanded == original) { return this; }
        StringRange actual = projection.toOriginal(expanded.getRange());
        String prefix = actual.getStart() < getRange().getStart()
                ? command.substring(actual.getStart(), getRange().getStart()) : "";
        String suffix = actual.getEnd() > getRange().getEnd()
                ? command.substring(getRange().getEnd(), actual.getEnd()) : "";
        return new ProjectedSuggestion(expanded, projection, prefix + displayText + suffix, prefix + localizedInsertion + suffix, tooltips, metadataSource);
    }
}
