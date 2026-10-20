package io.github.kltyton.commandtranslation.client.command.help;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.kltyton.commandtranslation.client.ClientPlatform;
import io.github.kltyton.commandtranslation.client.command.CommandHelp;
import io.github.kltyton.commandtranslation.client.config.ClientConfig;
import io.github.kltyton.commandtranslation.client.i18n.IdentifierNames;
import io.github.kltyton.commandtranslation.client.i18n.CommandLanguage;
import io.github.kltyton.commandtranslation.client.i18n.TagNames;
import io.github.kltyton.commandtranslation.client.i18n.NamespaceNames;
import java.util.List;
import net.minecraft.network.chat.Component;

/** Selects documentation for the token under the pointer, including fields inside one Brigadier argument. */
public final class ArgumentHover {
    private final boolean names;
    private final boolean usage;
    private final boolean identity;

    private ArgumentHover(boolean names, boolean usage, boolean identity) {
        this.names = names;
        this.usage = usage;
        this.identity = identity;
    }

    public static <S> Component describe(CommandDocumentation.Subject<S> subject, String text, int cursor, String fallbackKind) {
        ClientConfig settings = ClientConfig.current();
        Component result = new ArgumentHover(settings.identifierTranslation, settings.usageTooltips, false).select(subject, text, cursor, fallbackKind);
        if (result == null || NamespaceNames.isQualified(result)) { return result; }
        String owner = subject.entityType();
        String parser = ArgumentDocumentation.parser(subject.nodes().get(subject.nodes().size() - 1));
        int list = text.indexOf('[');
        if (("minecraft:item_stack".equals(parser) || "minecraft:item_predicate".equals(parser)) && list >= 0 && cursor > list) {
            var field = StructuredFieldLocator.at(text, list, cursor, StructuredFieldLocator.Format.ASSIGNMENTS);
            if (field != null && !field.path().isEmpty() && field.path().get(0).contains(":")) { owner = field.path().get(0); }
        }
        if (owner == null && !text.startsWith("{") && !text.startsWith("@")) { owner = ClientPlatform.identifierId(text); }
        if (owner == null) { return NamespaceNames.qualify(NamespaceNames.commandNamespace(subject.nodes().get(0).getName()), result); }
        String id = ClientPlatform.identifierId(owner);
        return NamespaceNames.qualify(id == null ? "minecraft" : id.substring(0, id.indexOf(':')), result);
    }

    public static <S> Component identity(CommandDocumentation.Subject<S> subject, String text, int cursor, String fallbackKind) {
        return new ArgumentHover(true, true, true).select(subject, text, cursor, fallbackKind);
    }

    private <S> Component select(CommandDocumentation.Subject<S> subject, String text, int cursor, String fallbackKind) {
        CommandNode<S> node = subject.nodes().get(subject.nodes().size() - 1);
        String parser = ArgumentDocumentation.parser(node);
        if (parser != null) {
            boolean item = parser.equals("minecraft:item_stack") || parser.equals("minecraft:item_predicate");
            boolean block = parser.equals("minecraft:block_state") || parser.equals("minecraft:block_predicate");
            int list = text.indexOf('[');
            int compound = text.indexOf('{');
            if (text.startsWith("@") && text.length() >= 2 && cursor < 2
                    && (parser.equals("minecraft:entity") || parser.equals("minecraft:game_profile") || parser.equals("minecraft:score_holder"))) {
                return usage && (identity || ClientConfig.current().valueTooltips) ? SelectorDocumentation.name(text.substring(0, 2)) : null;
            }
            if (item && StructuredFieldLocator.contains(text, list, cursor) && (compound < 0 || list < compound)) {
                if (!identity && !ClientConfig.current().structuredTooltips) { return null; }
                var field = StructuredFieldLocator.at(text, list, cursor, StructuredFieldLocator.Format.ASSIGNMENTS);
                return usage ? withFieldName(field, component(text, field)) : null;
            }
            if (block && StructuredFieldLocator.contains(text, list, cursor) && (compound < 0 || list < compound)) {
                if (!identity && !ClientConfig.current().structuredTooltips) { return null; }
                var field = StructuredFieldLocator.at(text, list, cursor, StructuredFieldLocator.Format.ASSIGNMENTS);
                return usage && field != null ? withFieldName(field, blockState(text, field)) : null;
            }
            if (text.startsWith("@") && StructuredFieldLocator.contains(text, list, cursor)) {
                if (!identity && !ClientConfig.current().structuredTooltips) { return null; }
                var field = StructuredFieldLocator.at(text, list, cursor, StructuredFieldLocator.Format.SELECTOR);
                return withFieldName(field, selector(field));
            }
            if (StructuredFieldLocator.contains(text, compound, cursor)) {
                if (!identity && !ClientConfig.current().structuredTooltips) { return null; }
                var field = StructuredFieldLocator.at(text, compound, cursor, StructuredFieldLocator.Format.COMPOUND);
                if (!usage || field == null) { return null; }
                if (item) { return withFieldName(field, field("nbt.item", field)); }
                if (block) { return withFieldName(field, field("nbt.block_entity", field)); }
                if (parser.equals("minecraft:component") || parser.equals("minecraft:style")) { return withFieldName(field, text(field)); }
                if (parser.equals("minecraft:particle")) { return withFieldName(field, field("particle." + ClientPlatform.identifierId(text), field)); }
                if (parser.equals("minecraft:dialog")) { return withFieldName(field, field("dialog", field)); }
                if (parser.equals("minecraft:nbt_compound_tag") || parser.equals("minecraft:nbt_tag")) {
                    String family = nbtFamily(subject);
                    return family == null ? null : withFieldName(field, nbt(family, subject.entityType(), field));
                }
                return null;
            }
            if (usage && !identity && (parser.equals("minecraft:vec3") || parser.equals("minecraft:block_pos")
                    || parser.equals("minecraft:vec2") || parser.equals("minecraft:column_pos") || parser.equals("minecraft:rotation"))) {
                Component axis = coordinate(parser, text, cursor);
                Component purpose = CommandDocumentation.describe(subject);
                return axis == null ? null : purpose == null ? axis : purpose.copy().append("\n").append(axis);
            }
            if (usage && parser.equals("minecraft:nbt_path")) {
                if (!identity && !ClientConfig.current().structuredTooltips) { return null; }
                var field = StructuredFieldLocator.pathAt(text, cursor);
                String family = nbtFamily(subject);
                return field == null || family == null ? null : withFieldName(field, nbt(family, subject.entityType(), field));
            }
        }
        if (subject.value().startsWith("#") && (fallbackKind != null || ArgumentDocumentation.registry(node) != null
                || "minecraft:item_predicate".equals(parser) || "minecraft:block_predicate".equals(parser)
                || "minecraft:function".equals(parser))) {
            return (!identity && !ClientConfig.current().tagTooltips) ? null : names || usage
                    ? IdentifierNames.display(node, fallbackKind, subject.value()) : null;
        }
        if (usage && !identity && ClientConfig.current().valueTooltips) {
            Component value = CommandDocumentation.value(subject);
            if (value != null) { return value; }
        }
        if (usage && !identity) {
            String registry = ArgumentDocumentation.registry(node);
            if (registry == null) {
                registry = switch (parser == null ? "" : parser) {
                    case "minecraft:item_stack", "minecraft:item_predicate" -> "minecraft:item";
                    case "minecraft:block_state", "minecraft:block_predicate" -> "minecraft:block";
                    default -> fallbackKind == null ? null : "minecraft:" + switch (fallbackKind) {
                        case "entity" -> "entity_type";
                        case "effect" -> "mob_effect";
                        default -> fallbackKind;
                    };
                };
            }
            Component resource = ValueDocumentation.registry(registry, subject.value(), names, true);
            if (resource != null) { return resource; }
        }
        if (names) {
            Component name = IdentifierNames.display(node, fallbackKind, subject.value());
            if (name != null) { return name; }
        }
        if (identity) { return null; }
        Component documentation = usage && (ClientConfig.current().valueTooltips || node instanceof LiteralCommandNode<?>)
                ? CommandDocumentation.describe(subject) : null;
        if (usage && !identity && node instanceof LiteralCommandNode<?>
                && (subject.nodes().size() == 1 || !node.getChildren().isEmpty() || node.getRedirect() != null)) {
            return CommandSyntax.describe(subject, documentation);
        }
        return documentation;
    }

    private Component component(String item, StructuredFieldLocator.Field field) {
        if (field == null || field.path().isEmpty()) { return null; }
        if (!identity && !field.key() && !ClientConfig.current().valueTooltips) { return null; }
        String raw = field.path().get(0);
        String id = ClientPlatform.identifierId(raw.startsWith("!") ? raw.substring(1) : raw);
        if (id == null) { return null; }
        String family = "component." + id;
        List<String> members = field.path().subList(1, field.path().size());
        int components = switch (id) {
            case "minecraft:container" -> members.size() > 2 && members.get(0).equals("item") && members.get(1).equals("components") ? 2 : -1;
            case "minecraft:bundle_contents", "minecraft:charged_projectiles", "minecraft:use_remainder" -> members.size() > 1 && members.get(0).equals("components") ? 1 : -1;
            default -> -1;
        };
        if (components >= 0) {
            return component(item, new StructuredFieldLocator.Field(members.subList(components, members.size()), field.value(), field.key()));
        }
        if (field.path().size() == 1) {
            if (!field.key()) {
                String value = unquote(field.value());
                if (!identity && value.startsWith("#") && !ClientConfig.current().tagTooltips) { return null; }
                Component specific = identity ? ValueDocumentation.name(family, List.of(), value)
                        : ValueDocumentation.value(family, List.of(), value);
                if (specific != null) { return specific; }
                Component resource = ValueDocumentation.registry(ValueDocumentation.registry(family, List.of(), false),
                        value, names, usage && !identity);
                if (resource != null) { return resource; }
                if (id.equals("minecraft:provides_banner_patterns")) {
                    Component tag = TagNames.name("minecraft:banner_pattern", value);
                    if (tag != null) { return tag; }
                }
                String kind = switch (id) {
                    case "minecraft:painting/variant" -> "painting";
                    case "minecraft:tropical_fish/pattern" -> "tropical_fish_pattern";
                    default -> null;
                };
                if (kind != null && names) {
                    Component name = IdentifierNames.display(kind, value);
                    if (name != null) { return name; }
                }
            }
            if (field.key()) {
                Component purpose = identity ? null : translated(family);
                return purpose != null ? purpose : names ? IdentifierNames.displayRegistry("minecraft:data_component_type", id) : null;
            }
            return identity ? null : contextValue(field.value(), translated(family));
        }
        var member = new StructuredFieldLocator.Field(field.path().subList(1, field.path().size()), field.value(), field.key());
        Component shared = switch (id) {
            case "minecraft:block_state" -> blockState(item, member);
            case "minecraft:entity_data", "minecraft:bucket_entity_data" -> field("nbt.entity", member);
            case "minecraft:block_entity_data" -> field("nbt.block_entity", member);
            case "minecraft:custom_name", "minecraft:item_name", "minecraft:lore" -> text(member);
            default -> null;
        };
        return shared == null ? field(family, member) : shared;
    }

    private Component blockState(String block, StructuredFieldLocator.Field field) {
        Component specific = field("block_state." + ClientPlatform.identifierId(block), field);
        if (specific != null) { return specific; }
        String values = ClientPlatform.blockPropertyValues(block, field.path().get(0));
        Component variant = values == null ? null : field("block_state.values." + values, field);
        return variant == null ? field("block_state", field) : variant;
    }

    private Component text(StructuredFieldLocator.Field field) {
        List<String> path = field.path();
        while (!path.isEmpty() && (path.get(0).equals("extra") || path.get(0).equals("with")) && path.size() > 1) {
            path = path.subList(1, path.size());
        }
        return field("text", new StructuredFieldLocator.Field(path, field.value(), field.key()));
    }

    private static Component coordinate(String parser, String text, int cursor) {
        int index = 0;
        boolean word = false;
        for (int i = 0; i < cursor; i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                if (word) { index++; word = false; }
            } else { word = true; }
        }
        List<String> axes = parser.equals("minecraft:rotation") ? List.of("yaw", "pitch")
                : parser.equals("minecraft:vec2") || parser.equals("minecraft:column_pos") ? List.of("x", "z")
                : text.indexOf('^') >= 0 ? List.of("left", "up", "forward") : List.of("x", "y", "z");
        return index < axes.size() ? translated("coordinate." + axes.get(index)) : null;
    }

    private static String nbtFamily(CommandDocumentation.Subject<?> subject) {
        List<String> literals = subject.nodes().stream().filter(LiteralCommandNode.class::isInstance).map(CommandNode::getName).toList();
        if (literals.isEmpty()) { return null; }
        String root = CommandHelp.canonicalRoot(literals.get(0));
        if (root.equals("summon")) { return "nbt.entity"; }
        if (!root.equals("data") && !(root.equals("execute") && (literals.contains("data") || literals.contains("store")))) { return null; }
        for (int i = literals.size() - 1; i >= 0; i--) {
            switch (literals.get(i)) {
                case "storage" -> { return null; }
                case "block" -> { return "nbt.block_entity"; }
                case "entity" -> { return "nbt.entity"; }
                default -> { }
            }
        }
        return null;
    }

    private Component nbt(String family, String entityType, StructuredFieldLocator.Field field) {
        Component specific = entityType == null ? null : field(family + "." + entityType, field);
        return specific == null ? field(family, field) : specific;
    }

    private Component selector(StructuredFieldLocator.Field field) {
        if (field == null || field.path().isEmpty()) { return null; }
        String option = field.path().get(0);
        if (field.path().size() > 1 && option.equals("nbt")) {
            return usage ? field("nbt.entity", new StructuredFieldLocator.Field(
                    field.path().subList(1, field.path().size()), field.value(), field.key())) : null;
        }
        if (!field.key() && field.path().size() == 1 && option.equals("type") && names) {
            String id = field.value().startsWith("!") ? field.value().substring(1) : field.value();
            Component name = ValueDocumentation.registry("minecraft:entity_type", id, names, usage && !identity);
            if (name != null) { return name; }
        }
        return usage ? field("selector", field) : null;
    }

    private Component field(String family, StructuredFieldLocator.Field field) {
        if (!identity && !field.key() && !ClientConfig.current().valueTooltips) { return null; }
        if (family.startsWith("nbt.entity") || family.equals("nbt.block_entity")) {
            List<String> members = field.path();
            boolean stack = !members.isEmpty() && List.of("Items", "Inventory", "HandItems", "ArmorItems", "SelectedItem", "Item", "RecordItem", "equipment").contains(members.get(0));
            int components = members.indexOf("components");
            if (stack && components > 0 && components + 1 < members.size()) {
                return component("", new StructuredFieldLocator.Field(members.subList(components + 1, members.size()), field.value(), field.key()));
            }
            int tag = members.indexOf("tag");
            if (stack && tag > 0 && tag + 1 < members.size()) {
                return field("nbt.item", new StructuredFieldLocator.Field(members.subList(tag + 1, members.size()), field.value(), field.key()));
            }
        }
        String path = String.join(".", field.path());
        if (field.key() && names && !field.path().isEmpty()) {
            Component name = ValueDocumentation.registry(ValueDocumentation.registry(family, field.path(), true),
                    field.path().get(field.path().size() - 1), true, usage && !identity);
            if (name != null) { return name; }
        }
        if (!field.key()) {
            String raw = unquote(field.value());
            if (!identity && raw.startsWith("#") && !ClientConfig.current().tagTooltips) { return null; }
            Component specific = identity ? ValueDocumentation.name(family, field.path(), raw)
                    : ValueDocumentation.value(family, field.path(), raw);
            if (specific != null) { return specific; }
            Component resource = ValueDocumentation.registry(ValueDocumentation.registry(family, field.path(), false),
                    raw, names, usage && !identity);
            if (resource != null) { return resource; }
            if (raw.startsWith("#")) {
                String registry = switch (family) {
                    case "component.minecraft:damage_resistant" -> path.equals("types") ? "minecraft:damage_type" : null;
                    case "component.minecraft:tool", "component.minecraft:can_break", "component.minecraft:can_place_on" -> path.equals("blocks") || path.endsWith(".blocks") ? "minecraft:block" : null;
                    case "component.minecraft:repairable" -> path.equals("items") ? "minecraft:item" : null;
                    case "component.minecraft:equippable" -> path.equals("allowed_entities") ? "minecraft:entity_type" : null;
                    default -> null;
                };
                if (registry != null) {
                    Component tag = TagNames.name(registry, raw);
                    if (tag != null) { return tag; }
                }
            }
            String kind = switch (family) {
                case "component.minecraft:attribute_modifiers" -> path.endsWith("type") ? "attribute" : null;
                case "component.minecraft:potion_contents", "component.minecraft:consumable", "component.minecraft:death_protection", "component.minecraft:food" -> path.endsWith(".id") ? "effect" : null;
                case "component.minecraft:container", "component.minecraft:bundle_contents", "component.minecraft:charged_projectiles", "particle.minecraft:item" -> path.equals("id") || path.endsWith(".id") ? "item" : null;
                default -> family.startsWith("nbt.entity") && path.equals("id") ? "entity"
                        : path.equals("Items.id") || path.equals("Inventory.id") || path.equals("Item.id") ? "item" : null;
            };
            if (kind != null && names) {
                Component name = IdentifierNames.display(kind, raw);
                if (name != null) { return name; }
            }
        }
        if (identity) {
            if (!field.key()) { return null; }
            String named = DocumentationKeys.fieldName(family, field.path(), "");
            return named == null ? null : Component.translatable(named);
        }
        String key = DocumentationKeys.field(family, field.path(), "");
        if (key == null) { return null; }
        Component purpose = Component.translatable(key);
        return field.key() ? purpose : contextValue(field.value(), purpose);
    }

    private static Component contextValue(String raw, Component purpose) {
        return purpose == null || raw.isEmpty() || raw.startsWith("{") || raw.startsWith("[") ? purpose
                : Component.translatable("command_translation.help.value.context", Component.literal(raw), purpose);
    }

    private static Component valueName(String raw) {
        String value = unquote(raw);
        List<String> names = CommandLanguage.aliases("value." + value, value);
        return names.stream().filter(name -> !name.equals(value)).findFirst().map(Component::literal).orElse(null);
    }

    private Component withFieldName(StructuredFieldLocator.Field field, Component selected) {
        if (!identity || selected != null || field == null || field.path().isEmpty()) { return selected; }
        if (!field.key()) { return valueName(field.value()); }
        String key = DocumentationKeys.presentName("field." + field.path().get(field.path().size() - 1));
        return key == null ? null : Component.translatable(key);
    }

    private static String unquote(String value) {
        value = value.strip();
        if (value.isEmpty() || value.charAt(0) != '"' && value.charAt(0) != '\'') { return value; }
        try { return new StringReader(value).readString(); }
        catch (CommandSyntaxException incomplete) { return value.substring(1); }
    }

    private static Component translated(String suffix) {
        return DocumentationKeys.translated(suffix);
    }
}
