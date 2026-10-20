package io.github.kltyton.commandtranslation.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Style;

public final class ClientConfig {
    public static final List<String> ENTITY_ATTRIBUTE_KEYS = List.of("max_health", "attack_damage", "movement_speed", "attack_speed",
            "armor", "armor_toughness", "knockback_resistance", "attack_knockback", "follow_range", "flying_speed");
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile ClientConfig current = new ClientConfig();

    public boolean enabled = true;
    public boolean localizedInput = true;
    public boolean identifierTranslation = true;
    public boolean usageTooltips = true;
    public boolean suggestionTooltips = true;
    public boolean inputTooltips = true;
    public boolean commandSyntax = true;
    public boolean syntaxAnnotations = true;
    public boolean parameterTooltips = true;
    public boolean compactParameterTooltips = true;
    public boolean structuredTooltips = true;
    public boolean valueTooltips = true;
    public boolean tagTooltips = true;
    public boolean itemTooltips = true;
    public boolean entityTooltips = true;
    public Map<String, Boolean> entityAttributes = defaultEntityAttributes();
    public boolean otherEntityAttributes = false;
    public boolean effectTooltips = true;
    public boolean descriptionSearch = true;
    public boolean translationExport = true;
    public TextStyle identifierStyle = new TextStyle(0x55FFFF);
    public TextStyle tagStyle = new TextStyle(0xFFAA00);

    public static ClientConfig current() { return current; }

    public ClientConfig copy() {
        ClientConfig result = new ClientConfig();
        result.enabled = enabled;
        result.localizedInput = localizedInput;
        result.identifierTranslation = identifierTranslation;
        result.usageTooltips = usageTooltips;
        result.suggestionTooltips = suggestionTooltips;
        result.inputTooltips = inputTooltips;
        result.commandSyntax = commandSyntax;
        result.syntaxAnnotations = syntaxAnnotations;
        result.parameterTooltips = parameterTooltips;
        result.compactParameterTooltips = compactParameterTooltips;
        result.structuredTooltips = structuredTooltips;
        result.valueTooltips = valueTooltips;
        result.tagTooltips = tagTooltips;
        result.itemTooltips = itemTooltips;
        result.entityTooltips = entityTooltips;
        result.entityAttributes = new LinkedHashMap<>(entityAttributes);
        result.otherEntityAttributes = otherEntityAttributes;
        result.effectTooltips = effectTooltips;
        result.descriptionSearch = descriptionSearch;
        result.translationExport = translationExport;
        result.identifierStyle = identifierStyle.copy();
        result.tagStyle = tagStyle.copy();
        return result;
    }

    public static void load(Path directory) throws IOException {
        Path file = directory.resolve("command_translation.json");
        if (!Files.exists(file)) {
            save(directory, new ClientConfig());
            return;
        }
        JsonObject defaults = JSON.toJsonTree(new ClientConfig()).getAsJsonObject();
        merge(defaults, read(file));
        ClientConfig settings = JSON.fromJson(defaults, ClientConfig.class);
        validate(settings, file);
        current = settings;
    }

    public static void save(Path directory, ClientConfig settings) throws IOException {
        Path file = directory.resolve("command_translation.json");
        validate(settings, file);
        JsonObject document = Files.exists(file) ? read(file) : new JsonObject();
        merge(document, JSON.toJsonTree(settings).getAsJsonObject());
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "command_translation-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) { JSON.toJson(document, writer); }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException failure) {
            try { Files.deleteIfExists(temporary); }
            catch (IOException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
        current = settings;
    }

    private static JsonObject read(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject document = JSON.fromJson(reader, JsonObject.class);
            if (document == null) { throw new IOException("Invalid command translation configuration: " + file); }
            return document;
        } catch (JsonParseException failure) {
            throw new IOException("Invalid command translation configuration: " + file, failure);
        }
    }

    private static void validate(ClientConfig settings, Path file) throws IOException {
        if (settings == null || settings.identifierStyle == null || settings.tagStyle == null
                || settings.entityAttributes == null || settings.entityAttributes.values().stream().anyMatch(java.util.Objects::isNull)) {
            throw new IOException("Invalid command translation configuration: " + file);
        }
        try {
            settings.identifierStyle.validate();
            settings.tagStyle.validate();
        } catch (IllegalArgumentException failure) {
            throw new IOException("Invalid command translation configuration: " + file, failure);
        }
    }

    private static void merge(JsonObject target, JsonObject values) {
        values.entrySet().forEach(entry -> {
            if (entry.getValue().isJsonObject() && target.has(entry.getKey()) && target.get(entry.getKey()).isJsonObject()) {
                merge(target.getAsJsonObject(entry.getKey()), entry.getValue().getAsJsonObject());
            } else { target.add(entry.getKey(), entry.getValue()); }
        });
    }

    private static Map<String, Boolean> defaultEntityAttributes() {
        Map<String, Boolean> values = new LinkedHashMap<>();
        var visible = java.util.Set.of("max_health", "attack_damage", "movement_speed", "armor");
        ENTITY_ATTRIBUTE_KEYS.forEach(key -> values.put(key, visible.contains(key)));
        return values;
    }

    public boolean entityAttributeEnabled(String id) {
        Boolean exact = entityAttributes.get(id);
        if (exact != null) { return exact; }
        if (id.startsWith("minecraft:")) {
            String path = id.substring("minecraft:".length());
            if (path.startsWith("generic.")) { path = path.substring("generic.".length()); }
            Boolean builtin = entityAttributes.get(path);
            if (builtin != null) { return builtin; }
        }
        return otherEntityAttributes;
    }

    public static final class TextStyle {
        public int color;
        public boolean obfuscated;
        public boolean bold;
        public boolean strikethrough;
        public boolean underlined;
        public boolean italic;
        public boolean showIdentifierOnHover = true;

        public TextStyle() { this(0x55FFFF); }
        public TextStyle(int color) { this.color = color; }

        private TextStyle copy() {
            TextStyle result = new TextStyle(color);
            result.obfuscated = obfuscated;
            result.bold = bold;
            result.strikethrough = strikethrough;
            result.underlined = underlined;
            result.italic = italic;
            result.showIdentifierOnHover = showIdentifierOnHover;
            return result;
        }

        private void validate() {
            if (color < 0 || color > 0xFFFFFF) { throw new IllegalArgumentException("Text color must be a 24-bit RGB value"); }
        }

        public Style style() {
            return Style.EMPTY.withColor(color).withObfuscated(obfuscated).withBold(bold)
                    .withStrikethrough(strikethrough).withUnderlined(underlined).withItalic(italic);
        }
    }
}
