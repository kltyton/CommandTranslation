package io.github.kltyton.commandtranslation.client.i18n.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.kltyton.commandtranslation.client.command.export.CommandExportCatalog;
import io.github.kltyton.commandtranslation.client.i18n.CommandLanguage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.minecraft.server.packs.resources.ResourceManager;

/** Builds a separate, inactive resource pack from real command data and untranslated language entries. */
public final class TranslationPackWriter {
    public record Result(Optional<Path> file, int keys, int commands, int missingDescriptions, int unknownNamespaces) {}
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();
    private static final String PREFIX = CommandLanguage.PREFIX;

    private TranslationPackWriter() {}

    public static Result write(ResourceManager resources, List<CommandExportCatalog.Entry> tree, String locale,
                               String filter, Path directory, JsonObject metadata, String guide) throws IOException {
        if (!locale.matches("[a-z0-9_.-]+")) { throw new IOException("Invalid language code: " + locale); }
        Map<String, String> english = language(resources, "en_us");
        Map<String, String> translated = locale.equals("en_us") ? english : language(resources, locale);
        Set<String> vanilla = vanillaCommands();
        Map<String, String> owners = new LinkedHashMap<>();
        Set<String> commandRootNamespaces = new LinkedHashSet<>();
        for (var node : tree) {
            if (node.path().size() != 1 || node.root().equals("command_translation")) { continue; }
            String root = node.root();
            int colon = root.indexOf(':');
            if (root.startsWith("minecraft:") || colon < 0 && vanilla.contains(root)) { continue; }
            String binding = PREFIX + "command." + root + ".namespace";
            String owner = colon >= 0 ? root.substring(0, colon)
                    : covered(translated, binding) ? translated.get(binding) : english.get(binding);
            if (owner == null || owner.isBlank()) { owner = root; commandRootNamespaces.add(root); }
            if (filter.isEmpty() || filter.equals(root) || filter.equals(owner)) { owners.put(root, owner); }
        }
        Map<String, String> output = new TreeMap<>();
        Set<String> protectedKeys = new LinkedHashSet<>();
        Set<String> missingDescriptions = new LinkedHashSet<>();
        List<CommandExportCatalog.Entry> selected = tree.stream().filter(node -> owners.containsKey(node.root())).toList();
        for (var node : selected) {
            String path = node.keyPath();
            if (!node.argument()) {
                String alias = PREFIX + (node.path().size() == 1 ? "command." : "literal.") + path + ".alias";
                String fallback = node.path().size() == 1 ? null : PREFIX + "literal." + node.name() + ".alias";
                String name = node.name().contains(":") ? node.name().substring(node.name().indexOf(':') + 1) : node.name();
                add(output, translated, english, alias, name, fallback);
            } else {
                add(output, translated, english, PREFIX + "syntax.argument." + path, node.name(),
                        PREFIX + "syntax.argument." + node.name());
                for (String value : node.values()) {
                    add(output, translated, english, PREFIX + "value." + value + ".alias", value, null);
                    description(output, translated, english, missingDescriptions,
                            PREFIX + "help.value." + path + "." + value,
                            PREFIX + "help.value." + node.root() + "." + node.name() + "." + value,
                            PREFIX + "help.value.type." + node.parser() + "." + value);
                }
            }
            String help = PREFIX + "help." + (node.path().size() == 1 ? "command." : "argument.") + path;
            description(output, translated, english, missingDescriptions, help,
                    node.path().size() == 1 ? help : PREFIX + "help.argument." + node.root() + "." + node.name());
        }
        for (var owner : owners.entrySet()) {
            if (owner.getValue() == null) { continue; }
            String binding = PREFIX + "command." + owner.getKey() + ".namespace";
            if (!covered(translated, binding)) { output.put(binding, owner.getValue()); protectedKeys.add(binding); }
            String namespace = owner.getValue();
            String nameKey = PREFIX + "namespace." + namespace;
            if (!namespaceNameCovered(translated, namespace)) {
                String name = english.getOrDefault(nameKey, english.getOrDefault("modmenu.nameTranslation." + namespace,
                        english.getOrDefault(namespace + ".ponder.mod_name", namespace)));
                output.putIfAbsent(nameKey, name);
            }
        }
        for (var entry : english.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(PREFIX) || entry.getValue().isBlank() || covered(translated, key)) { continue; }
            if (key.startsWith(PREFIX + "namespace.") && namespaceNameCovered(translated, key.substring((PREFIX + "namespace.").length()))) { continue; }
            boolean related = owners.keySet().stream().anyMatch(root -> key.startsWith(PREFIX + "command." + root + ".")
                    || key.startsWith(PREFIX + "literal." + root + ".") || key.startsWith(PREFIX + "syntax.argument." + root + ".")
                    || key.equals(PREFIX + "help.command." + root) || key.startsWith(PREFIX + "help.argument." + root + ".")
                    || key.startsWith(PREFIX + "help.value." + root + "."));
            if (!related) {
                related = owners.values().stream().filter(java.util.Objects::nonNull)
                        .anyMatch(owner -> key.contains("." + owner + ":") || key.equals(PREFIX + "namespace." + owner));
            }
            if (related) {
                output.putIfAbsent(key, entry.getValue());
                if (bindingKey(key)) { protectedKeys.add(key); }
            }
        }
        if (owners.isEmpty() || output.isEmpty() && missingDescriptions.isEmpty()) {
            return new Result(Optional.empty(), 0, owners.size(), 0, 0);
        }
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("target_language", locale);
        manifest.put("target_file", "assets/command_translation/lang/" + locale + ".json");
        manifest.put("namespaces", owners);
        manifest.put("command_root_namespaces", commandRootNamespaces.stream().filter(owners::containsKey).toList());
        manifest.put("protected_keys", protectedKeys);
        manifest.put("missing_descriptions", missingDescriptions);
        var commandData = JSON.toJsonTree(selected).getAsJsonArray();
        for (var command : commandData) {
            var object = command.getAsJsonObject();
            object.add("properties", new JsonParser().parse(object.get("properties").getAsString()));
        }
        manifest.put("commands", commandData);
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, ".command-translation-" + locale + "-", ".tmp");
        String name = temporary.getFileName().toString();
        Path file = temporary.resolveSibling(name.substring(1, name.length() - 4) + ".zip");
        try {
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(temporary), StandardCharsets.UTF_8)) {
                entry(zip, "pack.mcmeta", JSON.toJson(metadata));
                entry(zip, "assets/command_translation/lang/en_us.json", JSON.toJson(output));
                if (!locale.equals("en_us")) { entry(zip, "assets/command_translation/lang/" + locale + ".json", JSON.toJson(output)); }
                entry(zip, "translation-manifest.json", JSON.toJson(manifest));
                entry(zip, "TRANSLATING.md", guide);
            }
            Files.move(temporary, file);
        } catch (IOException | RuntimeException failure) {
            try { Files.deleteIfExists(temporary); }
            catch (IOException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
        return new Result(Optional.of(file), output.size(), owners.size(), missingDescriptions.size(),
                (int) commandRootNamespaces.stream().filter(owners::containsKey).count());
    }

    private static Map<String, String> language(ResourceManager resources, String locale) throws IOException {
        return LanguageResources.read(resources, locale);
    }

    private static Set<String> vanillaCommands() throws IOException {
        Set<String> commands = new LinkedHashSet<>();
        String path = "/assets/command_translation/documentation/vanilla-commands.json";
        try (var stream = TranslationPackWriter.class.getResourceAsStream(path)) {
            if (stream == null) { throw new IOException("Missing bundled command catalog: " + path); }
            for (var value : new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray()) {
                commands.add(value.getAsString());
            }
        }
        return commands;
    }

    private static boolean covered(Map<String, String> language, String key) {
        String value = key == null ? null : language.get(key);
        return value != null && !value.isBlank();
    }

    private static void add(Map<String, String> output, Map<String, String> translated, Map<String, String> english,
                            String key, String fallbackText, String fallbackKey) {
        if (covered(translated, key) || covered(translated, fallbackKey)) { return; }
        String text = covered(english, key) ? english.get(key) : covered(english, fallbackKey) ? english.get(fallbackKey) : fallbackText;
        output.putIfAbsent(key, text);
    }

    private static void description(Map<String, String> output, Map<String, String> translated, Map<String, String> english,
                                    Set<String> missing, String key, String... fallbacks) {
        if (covered(translated, key)) { return; }
        for (String fallback : fallbacks) { if (covered(translated, fallback)) { return; } }
        String text = english.get(key);
        if (text == null || text.isBlank()) {
            for (String fallback : fallbacks) {
                if (covered(english, fallback)) { text = english.get(fallback); break; }
            }
        }
        if (text == null || text.isBlank()) { missing.add(key); }
        else { output.putIfAbsent(key, text); }
    }

    private static boolean bindingKey(String key) {
        return key.endsWith(".namespace") || key.endsWith(".registry") || key.endsWith(".key_registry") || key.endsWith(".value_type");
    }

    private static boolean namespaceNameCovered(Map<String, String> language, String namespace) {
        return covered(language, PREFIX + "namespace." + namespace) || covered(language, "modmenu.nameTranslation." + namespace)
                || covered(language, namespace + ".ponder.mod_name");
    }

    private static void entry(ZipOutputStream zip, String path, String text) throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        zip.write((text + "\n").getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
