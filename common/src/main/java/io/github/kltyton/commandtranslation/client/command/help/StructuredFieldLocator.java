package io.github.kltyton.commandtranslation.client.command.help;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Locates a hovered field in incomplete text without validating or rewriting its value. */
public final class StructuredFieldLocator {
    public enum Format { ASSIGNMENTS, SELECTOR, COMPOUND }
    public record Field(List<String> path, String value, boolean key) {
        public Field { path = List.copyOf(path); }
    }
    public record Located(Field field, int start, int end) {}

    private final String text;
    private final int cursor;
    private final Format format;

    private StructuredFieldLocator(String text, int cursor, Format format) {
        this.text = text;
        this.cursor = cursor;
        this.format = format;
    }

    public static Field at(String text, int opening, int cursor, Format format) {
        Located located = locate(text, opening, cursor, format);
        return located == null ? null : located.field();
    }

    public static Located locate(String text, int opening, int cursor, Format format) {
        if (opening < 0 || cursor <= opening || cursor >= text.length()) { return null; }
        char separator = switch (format) {
            case COMPOUND -> ':';
            case ASSIGNMENTS, SELECTOR -> '=';
        };
        return new StructuredFieldLocator(text, cursor, format).object(opening, List.of(), separator);
    }

    public static boolean contains(String text, int opening, int cursor) {
        return opening >= 0 && cursor > opening && cursor < new StructuredFieldLocator(text, cursor, Format.COMPOUND).valueEnd(opening);
    }

    public static Field pathAt(String text, int cursor) {
        Located located = locatePath(text, cursor);
        return located == null ? null : located.field();
    }

    public static Located locatePath(String text, int cursor) {
        var locator = new StructuredFieldLocator(text, cursor, Format.COMPOUND);
        List<String> path = new ArrayList<>();
        int position = 0;
        while (position < text.length()) {
            char first = text.charAt(position);
            if (first == '.') { position++; continue; }
            if (first == '[') {
                int end = locator.valueEnd(position);
                if (cursor >= position && cursor < end) { return null; }
                position = end;
                continue;
            }
            if (first == '{') { return locator.object(position, path, ':'); }
            int end = first == '"' || first == '\'' ? locator.quotedEnd(position) : position;
            if (end == position) {
                while (end < text.length() && text.charAt(end) != '.' && text.charAt(end) != '[' && text.charAt(end) != '{') { end++; }
            }
            String part = text.substring(position, end);
            if ((first == '"' || first == '\'') && part.length() > 1 && part.charAt(part.length() - 1) == first) {
                part = part.substring(1, part.length() - 1);
            }
            path.add(part);
            if (cursor >= position && cursor < end) { return new Located(new Field(path, "", true), position, end); }
            position = end;
        }
        return null;
    }

    private Located object(int opening, List<String> parent, char separator) {
        int position = space(opening + 1);
        while (position < text.length() && text.charAt(position) != '}' && text.charAt(position) != ']') {
            int start = position;
            int end = keyEnd(start, separator);
            if (end == start) { return null; }
            String key = text.substring(start, end);
            if (key.charAt(0) == '"' || key.charAt(0) == '\'') {
                char quote = key.charAt(0);
                key = key.substring(1, key.length() > 1 && key.charAt(key.length() - 1) == quote ? key.length() - 1 : key.length());
            }
            List<String> path = new ArrayList<>(parent);
            path.add(key);
            if (cursor >= start && cursor < end) { return new Located(new Field(path, "", true), start, end); }
            position = space(end);
            if (position >= text.length()) { return null; }
            char assignment = text.charAt(position);
            if (assignment != separator && !(separator == '=' && assignment == '~')) { return null; }
            int valueStart = space(position + 1);
            int valueEnd = valueEnd(valueStart);
            if (cursor >= valueStart && cursor < valueEnd) {
                Located nested = nested(valueStart, path);
                return nested != null ? nested : new Located(new Field(path, text.substring(valueStart, valueEnd), false), valueStart, valueEnd);
            }
            position = space(valueEnd);
            if (position >= text.length() || text.charAt(position) != ',') { return null; }
            position = space(position + 1);
        }
        return null;
    }

    private Located nested(int start, List<String> path) {
        if (start >= text.length()) { return null; }
        return switch (text.charAt(start)) {
            case '{' -> object(start, path, nestedSeparator(path));
            case '[' -> array(start, path);
            default -> null;
        };
    }

    private char nestedSeparator(List<String> path) {
        if (format == Format.SELECTOR && !path.isEmpty()
                && (path.get(0).equals("scores") || path.get(0).equals("advancements"))) { return '='; }
        return ':';
    }

    private Located array(int opening, List<String> path) {
        int position = space(opening + 1);
        while (position < text.length() && text.charAt(position) != ']') {
            int end = valueEnd(position);
            if (cursor >= position && cursor < end) {
                Located nested = nested(position, path);
                return nested != null ? nested : new Located(new Field(path, text.substring(position, end), false), position, end);
            }
            position = space(end);
            if (position >= text.length() || text.charAt(position) != ',') { return null; }
            position = space(position + 1);
        }
        return null;
    }

    private int keyEnd(int start, char separator) {
        char first = text.charAt(start);
        if (first == '"' || first == '\'') { return quotedEnd(start); }
        int end = start;
        while (end < text.length()) {
            char value = text.charAt(end);
            if (Character.isWhitespace(value) || value == separator || value == ',' || value == '}' || value == ']'
                    || separator == '=' && value == '~') { break; }
            end++;
        }
        return end;
    }

    private int valueEnd(int start) {
        var closing = new ArrayDeque<Character>();
        int position = start;
        while (position < text.length()) {
            char value = text.charAt(position);
            if (value == '"' || value == '\'') {
                position = quotedEnd(position);
                continue;
            }
            if (value == '{' || value == '[' || value == '(') {
                closing.push(value == '{' ? '}' : value == '[' ? ']' : ')');
            } else if (value == '}' || value == ']' || value == ')') {
                if (closing.isEmpty() || closing.pop() != value) { return position; }
                if (closing.isEmpty()) { return position + 1; }
            } else if (value == ',' && closing.isEmpty()) {
                return position;
            }
            position++;
        }
        return position;
    }

    private int quotedEnd(int start) {
        char quote = text.charAt(start);
        int position = start + 1;
        while (position < text.length()) {
            char value = text.charAt(position++);
            if (value == '\\' && position < text.length()) { position++; }
            else if (value == quote) { break; }
        }
        return position;
    }

    private int space(int start) {
        int position = start;
        while (position < text.length() && Character.isWhitespace(text.charAt(position))) { position++; }
        return position;
    }
}
