package io.github.kltyton.commandtranslation.client.command.suggestion;

import com.mojang.brigadier.context.StringRange;
import io.github.kltyton.commandtranslation.client.command.CommandText;

/** Removes the canonical projection of one source fragment so the provider answers with an empty prefix. */
public final class QueryProjection {
    private final CommandText text;
    private final int sourceStart;
    private final int sourceEnd;
    private final int canonicalStart;
    private final int canonicalEnd;
    private final String input;
    private final int cursor;

    public QueryProjection(CommandText text, int sourceStart, int sourceEnd) {
        if (sourceStart < 0 || sourceEnd < sourceStart || sourceEnd > text.original().length()) {
            throw new IllegalArgumentException("Invalid source range: " + sourceStart + ".." + sourceEnd);
        }
        this.text = text;
        this.sourceStart = sourceStart;
        this.sourceEnd = sourceEnd;
        this.canonicalStart = text.canonicalOffset(sourceStart);
        this.canonicalEnd = text.canonicalOffset(sourceEnd);
        String canonical = text.canonical();
        this.cursor = canonicalStart;
        this.input = canonical.substring(0, canonicalStart) + canonical.substring(canonicalEnd);
    }

    public String input() {
        return input;
    }

    public int cursor() {
        return cursor;
    }

    public String original() {
        return text.original();
    }

    public StringRange toOriginal(StringRange range) {
        return StringRange.between(queryToOriginalStart(range.getStart()), queryToOriginalEnd(range.getEnd()));
    }

    public StringRange toQuery(StringRange range) {
        return StringRange.between(originalToQuery(range.getStart()), originalToQuery(range.getEnd()));
    }

    private int queryToOriginalStart(int queryOffset) {
        if (queryOffset == cursor) { return sourceStart; }
        if (queryOffset < cursor) {
            return text.originalOffset(queryOffset);
        }
        return text.originalOffset(queryOffset + canonicalEnd - canonicalStart);
    }

    private int queryToOriginalEnd(int queryOffset) {
        if (queryOffset == cursor) { return sourceEnd; }
        if (queryOffset < cursor) {
            return text.originalOffset(queryOffset);
        }
        return text.originalOffset(queryOffset + canonicalEnd - canonicalStart);
    }

    private int originalToQuery(int originalOffset) {
        if (originalOffset >= sourceStart && originalOffset <= sourceEnd) { return cursor; }
        int canonical = text.canonicalOffset(originalOffset);
        if (canonical <= canonicalStart) {
            return canonical;
        }
        if (canonical >= canonicalEnd) {
            return canonical - (canonicalEnd - canonicalStart);
        }
        return cursor;
    }
}
