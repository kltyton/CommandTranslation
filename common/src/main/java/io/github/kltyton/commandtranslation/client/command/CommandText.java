package io.github.kltyton.commandtranslation.client.command;

import com.mojang.brigadier.context.StringRange;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Maps command offsets across localized literals of different lengths. */
public final class CommandText {
    public record Replacement(int start, int end, String value) {}

    private final String original;
    private final String canonical;
    private final List<Replacement> replacements;
    private final int[] toCanonical;
    private final int[] toOriginal;

    public CommandText(String original, List<Replacement> replacements) {
        this.original = original;
        List<Replacement> ordered = new ArrayList<>(replacements);
        ordered.sort(Comparator.comparingInt(Replacement::start));
        this.replacements = List.copyOf(ordered);
        StringBuilder result = new StringBuilder();
        toCanonical = new int[original.length() + 1];
        List<Integer> reverse = new ArrayList<>();
        int position = 0;
        for (Replacement replacement : ordered) {
            if (replacement.start() < position) {
                throw new IllegalArgumentException("Overlapping command literals");
            }
            while (position < replacement.start()) {
                toCanonical[position] = result.length();
                reverse.add(position++);
                result.append(original.charAt(position - 1));
            }
            int targetStart = result.length();
            int sourceLength = replacement.end() - replacement.start();
            int targetLength = replacement.value().length();
            for (int index = 0; index < sourceLength; index++) {
                toCanonical[position + index] = targetStart + (int) ((long) index * targetLength / sourceLength);
            }
            for (int index = 0; index < targetLength; index++) {
                reverse.add(position + (int) ((long) index * sourceLength / targetLength));
            }
            result.append(replacement.value());
            position = replacement.end();
        }
        while (position < original.length()) {
            toCanonical[position] = result.length();
            reverse.add(position);
            result.append(original.charAt(position++));
        }
        toCanonical[position] = result.length();
        reverse.add(position);
        canonical = result.toString();
        toOriginal = reverse.stream().mapToInt(Integer::intValue).toArray();
    }

    public String original() { return original; }
    public String canonical() { return canonical; }
    public List<Replacement> replacements() { return replacements; }
    public int canonicalOffset(int offset) { return toCanonical[Math.max(0, Math.min(offset, original.length()))]; }
    public int originalOffset(int offset) { return toOriginal[Math.max(0, Math.min(offset, canonical.length()))]; }
    public StringRange originalRange(StringRange range) {
        return StringRange.between(originalOffset(range.getStart()), originalOffset(range.getEnd()));
    }
    public StringRange canonicalRange(StringRange range) {
        return StringRange.between(canonicalOffset(range.getStart()), canonicalOffset(range.getEnd()));
    }
}
