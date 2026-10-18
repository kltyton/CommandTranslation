package io.github.kltyton.commandtranslation.client.i18n;

import io.github.kltyton.commandtranslation.client.ClientPlatform;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

/** Tracks equal translated names within the actual registry and current resource language. */
final class IdentifierCollisions {
    private record Domain(String registry, boolean tags) {}
    private record Index(Registry<?> registry, Map<String, Integer> counts) {}
    private static final Map<Domain, Index> INDEXES = new HashMap<>();
    private static final Map<Domain, Map<String, java.util.Set<String>>> PROVIDED = new HashMap<>();
    private static Language language;
    private static Object connection;

    private IdentifierCollisions() {}

    static Component qualify(String registryId, String raw, Component name, Function<String, Component> names) {
        if (registryId == null || name == null) { return name; }
        context();
        Registry<?> registry = ClientPlatform.registry(registryId);
        Domain domain = new Domain(registryId, raw.startsWith("#"));
        int count;
        if (registry == null) {
            count = PROVIDED.getOrDefault(domain, Map.of()).getOrDefault(name.getString(), java.util.Set.of()).size();
        } else {
            Index index = INDEXES.get(domain);
            if (index == null || index.registry() != registry) {
                Map<String, Integer> counts = new HashMap<>();
                for (String id : ClientPlatform.registryIds(registry, domain.tags())) {
                    Component translated = names.apply(id);
                    if (translated != null) { counts.merge(translated.getString(), 1, Integer::sum); }
                }
                index = new Index(registry, Map.copyOf(counts));
                INDEXES.put(domain, index);
            }
            count = index.counts().getOrDefault(name.getString(), 0);
        }
        if (count <= 1) { return name; }
        boolean marked = raw.startsWith("#") || raw.startsWith("!");
        String id = ClientPlatform.identifierId(marked ? raw.substring(1) : raw);
        return name.copy().append("(" + (marked ? raw.substring(0, 1) : "") + id + ")");
    }

    static void observe(String registry, String raw, Component name) {
        if (name == null || ClientPlatform.registry(registry) != null) { return; }
        context();
        boolean marked = raw.startsWith("#") || raw.startsWith("!");
        String id = (marked ? raw.substring(0, 1) : "") + ClientPlatform.identifierId(marked ? raw.substring(1) : raw);
        PROVIDED.computeIfAbsent(new Domain(registry, raw.startsWith("#")), ignored -> new HashMap<>())
                .computeIfAbsent(name.getString(), ignored -> new java.util.HashSet<>()).add(id);
    }

    private static void context() {
        Object current = Minecraft.getInstance().getConnection();
        if (language != Language.getInstance() || connection != current) {
            clear();
            language = Language.getInstance();
            connection = current;
        }
    }

    static void clear() { INDEXES.clear(); PROVIDED.clear(); }
}
