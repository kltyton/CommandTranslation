package io.github.kltyton.commandtranslation.client.i18n;

import io.github.kltyton.commandtranslation.client.ClientPlatform;
import io.github.kltyton.commandtranslation.client.command.help.ArgumentDocumentation;
import io.github.kltyton.commandtranslation.client.config.ClientConfig;
import java.util.Map;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class IdentifierNames {
    private static final Map<String, String> FEEDBACK = Map.ofEntries(
            Map.entry("commands.locate.success", "structure"),
            Map.entry("commands.locatebiome.success", "biome"),
            Map.entry("commands.placefeature.success", "configured_feature"),
            Map.entry("commands.locate.biome.success", "biome"),
            Map.entry("commands.locate.structure.success", "structure"),
            Map.entry("commands.locate.poi.success", "poi"),
            Map.entry("commands.locate.biome.not_found", "biome"),
            Map.entry("commands.locate.structure.not_found", "structure"),
            Map.entry("commands.locate.poi.not_found", "poi"),
            Map.entry("commands.locate.structure.invalid", "structure"),
            Map.entry("commands.place.feature.success", "configured_feature"),
            Map.entry("commands.place.structure.success", "structure"));

    private IdentifierNames() {}

    public static Component display(CommandNode<?> node, String fallbackKind, String raw) {
        String parser = ArgumentDocumentation.parser(node);
        String kind = parser == null ? fallbackKind : switch (parser) {
            case "minecraft:item_stack", "minecraft:item_predicate" -> "item";
            case "minecraft:block_state", "minecraft:block_predicate" -> "block";
            case "minecraft:function" -> "function";
            default -> fallbackKind;
        };
        String registry = ArgumentDocumentation.registry(node);
        if (registry != null) {
            kind = switch (registry) {
                case "minecraft:item" -> "item";
                case "minecraft:block" -> "block";
                case "minecraft:entity_type" -> "entity";
                case "minecraft:attribute" -> "attribute";
                case "minecraft:mob_effect" -> "effect";
                case "minecraft:enchantment" -> "enchantment";
                case "minecraft:worldgen/biome" -> "biome";
                case "minecraft:worldgen/structure", "minecraft:worldgen/structure_feature", "minecraft:worldgen/configured_structure_feature" -> "structure";
                case "minecraft:worldgen/configured_feature" -> "configured_feature";
                case "minecraft:point_of_interest_type" -> "poi";
                default -> kind;
            };
        }
        return kind == null && !(raw.startsWith("#") && registry != null) ? null
                : display(kind, registry == null ? registry(kind) : registry, raw);
    }

    public static Component display(String kind, String raw) {
        return display(kind, registry(kind), raw);
    }

    public static String registry(String kind) {
        if (kind == null) { return null; }
        if (kind.equals("structure")) { return StructureIdentifiers.REGISTRY; }
        return "minecraft:" + switch (kind) {
            case "biome" -> "worldgen/biome";
            case "configured_feature" -> "worldgen/configured_feature";
            case "poi" -> "point_of_interest_type";
            case "entity" -> "entity_type";
            case "effect" -> "mob_effect";
            case "painting" -> "painting_variant";
            default -> kind;
        };
    }

    public static String literalKind(String path, String literal) {
        if (!StructureIdentifiers.LITERAL_COMMANDS || !path.equals("locate." + literal)) { return null; }
        var id = IdentifierAccess.tryParse(literal);
        return id != null && ClientPlatform.registry(StructureIdentifiers.REGISTRY).keySet().contains(id) ? "structure" : null;
    }

    public static Component displayRegistry(String registry, String raw) {
        if (registry.equals("minecraft:data_component_type")) {
            return IdentifierCollisions.qualify(registry, raw, componentName(raw), IdentifierNames::componentName);
        }
        return display(registryKind(registry), registry, raw);
    }

    public static void observeRegistry(String registry, String raw) {
        IdentifierCollisions.observe(registry, raw, registry.equals("minecraft:data_component_type") ? componentName(raw)
                : nameOnly(registryKind(registry), registry, raw));
    }

    private static String registryKind(String registry) {
        return switch (registry) {
            case "minecraft:item" -> "item";
            case "minecraft:block" -> "block";
            case "minecraft:entity_type" -> "entity";
            case "minecraft:attribute" -> "attribute";
            case "minecraft:mob_effect" -> "effect";
            case "minecraft:enchantment" -> "enchantment";
            case "minecraft:worldgen/biome" -> "biome";
            case "minecraft:worldgen/structure", "minecraft:worldgen/structure_feature", "minecraft:worldgen/configured_structure_feature" -> "structure";
            case "minecraft:worldgen/configured_feature" -> "configured_feature";
            case "minecraft:point_of_interest_type" -> "poi";
            case "minecraft:painting_variant" -> "painting";
            default -> registry.substring(registry.indexOf(':') + 1).replace('/', '.');
        };
    }

    private static Component componentName(String raw) {
        String id = ClientPlatform.identifierId(raw.startsWith("!") ? raw.substring(1) : raw);
        String name = id == null ? null : CommandLanguage.PREFIX + "registry.minecraft:data_component_type." + id + ".name";
        if (name != null && CommandLanguage.present(name) != null) { return NamespaceNames.identifier(id, MinecraftText.translatable(name)); }
        String key = id == null ? null : CommandLanguage.PREFIX + "name.component." + id;
        return key == null || CommandLanguage.present(key) == null ? null
                : NamespaceNames.identifier(id, MinecraftText.translatable(key));
    }

    private static Component display(String kind, String registry, String raw) {
        int memberStart = raw.indexOf(" (");
        if (raw.startsWith("#") && memberStart > 0 && raw.endsWith(")")) {
            String tagId = raw.substring(0, memberStart);
            String memberId = raw.substring(memberStart + 2, raw.length() - 1);
            Component tagName = display(kind, registry, tagId);
            Component memberName = kind == null ? null : display(kind, registry, memberId);
            if (tagName == null && memberName == null) { return null; }
            return MinecraftText.empty().append(tagName == null ? MinecraftText.literal(tagId) : tagName).append(" (")
                    .append(memberName == null ? MinecraftText.literal(memberId) : memberName).append(")");
        }
        boolean tag = raw.startsWith("#");
        Component name = nameOnly(kind, registry, raw);
        if (name == null) { return null; }
        name = IdentifierCollisions.qualify(registry, raw, name, id -> nameOnly(kind, registry, id));
        ClientConfig.TextStyle settings = tag ? ClientConfig.current().tagStyle : ClientConfig.current().identifierStyle;
        MutableComponent result = name.copy().setStyle(settings.style());
        String id = ClientPlatform.identifierId(tag ? raw.substring(1) : raw);
        return settings.showIdentifierOnHover ? ClientPlatform.withIdentifierHover(result, (tag ? "#" : "") + id) : result;
    }

    private static Component nameOnly(String kind, String registry, String raw) {
        boolean tag = raw.startsWith("#");
        Component name = tag ? TagNames.nameOnly(registry, raw) : ClientPlatform.identifierName(kind, raw);
        if (!tag) {
            String id = ClientPlatform.identifierId(raw);
            String key = id == null || registry == null ? null : CommandLanguage.PREFIX + "registry." + registry + "." + id + ".name";
            if (key != null && CommandLanguage.present(key) != null) { name = MinecraftText.translatable(key); }
        }
        if (!tag && name == null) {
            String key = ClientPlatform.identifierKey(kind, raw);
            if (key == null) { return null; }
            if (kind.equals("painting")) { key += ".title"; }
            if (kind.equals("tropical_fish_pattern") && key.startsWith("tropical_fish_pattern.minecraft.")) {
                key = "entity.minecraft.tropical_fish.type." + key.substring("tropical_fish_pattern.minecraft.".length());
            }
            String translation = translationKey(key);
            if (translation != null) {
                String indirect = CommandLanguage.present(translation);
                name = kind.equals("poi") && indirect != null && CommandLanguage.present(indirect) != null
                        ? MinecraftText.translatable(indirect) : MinecraftText.translatable(translation);
            } else {
                name = structureVariant(kind, key);
            }
        }
        if (name == null) { return null; }
        if (!tag) { name = NamespaceNames.identifier(raw, name); }
        return name;
    }

    public static void clearNameIndex() { IdentifierCollisions.clear(); }

    private static Component structureVariant(String kind, String key) {
        if (!kind.equals("structure") || !key.startsWith("structure.minecraft.")) { return null; }
        String id = key.substring("structure.minecraft.".length());
        for (String base : new String[]{"mineshaft", "ocean_ruin", "ruined_portal", "shipwreck", "village"}) {
            String variant;
            if (id.startsWith(base + "_")) { variant = id.substring(base.length() + 1); }
            else if (id.endsWith("_" + base)) { variant = id.substring(0, id.length() - base.length() - 1); }
            else { continue; }
            String baseName = translationKey("structure.minecraft." + base);
            String biome = switch (variant) {
                case "beached" -> "beach";
                case "cold", "warm" -> variant + "_ocean";
                case "mesa" -> "badlands";
                case "snowy" -> "snowy_plains";
                default -> variant;
            };
            String variantName = translationKey("biome.minecraft." + biome);
            if (variant.equals("nether")) { variantName = translationKey("dimension.minecraft.nether"); }
            if (variant.equals("mountain")) { variantName = translationKey("biomes.minecraft.mountains"); }
            if (baseName != null && variantName != null) {
                return MinecraftText.translatable("command_translation.identifier.variant",
                        MinecraftText.translatable(baseName), MinecraftText.translatable(variantName));
            }
        }
        return null;
    }

    private static String translationKey(String key) {
        if (CommandLanguage.present(key) != null) { return key; }
        String own = CommandLanguage.PREFIX + key;
        return CommandLanguage.present(own) != null ? own : null;
    }

    public static Component feedback(Component message) {
        ClientConfig settings = ClientConfig.current();
        if (!settings.enabled || !settings.identifierTranslation) { return message; }
        MutableComponent result;
        TranslationContent contents = MinecraftText.translation(message);
        if (contents != null) {
            Object[] args = contents.args().clone();
            String kind = FEEDBACK.get(contents.key());
            for (int i = 0; i < args.length; i++) {
                if (i == 0 && kind != null && args[i] instanceof String id) {
                    Component name = display(kind, id);
                    if (name != null) { args[i] = name.getString().endsWith("(" + id + ")") ? name
                            : MinecraftText.empty().append(name).append("(" + id + ")"); }
                } else if (args[i] instanceof Component component) {
                    args[i] = feedback(component);
                }
            }
            result = MinecraftText.rebuild(contents, args).setStyle(message.getStyle());
        } else {
            result = message.copy();
            result.getSiblings().clear();
        }
        message.getSiblings().forEach(sibling -> result.append(feedback(sibling)));
        return result;
    }
}
