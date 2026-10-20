package io.github.kltyton.commandtranslation.client.screen.configuration;

import io.github.kltyton.commandtranslation.CommandTranslation;
import io.github.kltyton.commandtranslation.client.config.ClientConfig;
import io.github.kltyton.commandtranslation.client.i18n.CommandLanguage;
import io.github.kltyton.commandtranslation.client.screen.ClientScreenAccess;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

/** Edits a private draft; saving publishes one complete configuration. */
public abstract class CommandConfigScreen extends Screen {
    protected enum Page { GENERAL, TOOLTIPS, ENTITY_ATTRIBUTES, IDENTIFIER, TAG }
    private record Binding(Checkbox checkbox, Consumer<Boolean> update) {}
    private record Swatch(Button button, int color) {}
    @FunctionalInterface
    protected interface RectangleFill { void fill(int x0, int y0, int x1, int y1, int color); }

    private final Screen parent;
    private final ClientConfig draft;
    private final Page page;
    private final List<Binding> bindings = new ArrayList<>();
    private final List<AbstractWidget> controls = new ArrayList<>();
    private final List<Swatch> swatches = new ArrayList<>();
    private StringWidget preview;
    private StringWidget status;
    private Button saveButton;
    private boolean saving;
    private Component error = Component.empty();

    protected CommandConfigScreen(Screen parent, ClientConfig draft, Page page) {
        super(label(switch (page) {
            case GENERAL -> "config.title";
            case TOOLTIPS -> "config.tooltips";
            case ENTITY_ATTRIBUTES -> "config.entity_attributes";
            case IDENTIFIER -> "config.identifier_style";
            case TAG -> "config.tag_style";
        }));
        this.parent = parent;
        this.draft = draft;
        this.page = page;
    }

    public static Screen create(Screen parent) {
        return new CommandConfigScreenView(parent, ClientConfig.current().copy(), Page.GENERAL);
    }

    @Override
    protected void init() {
        applyBindings();
        bindings.clear();
        controls.clear();
        swatches.clear();
        preview = null;
        int panelWidth = Math.min(336, width - 32);
        int left = (width - panelWidth) / 2;
        int column = (panelWidth - 8) / 2;
        centeredLabel(title, 12, 20);
        if (page == Page.GENERAL) {
            check(left, 46, column, "enabled", draft.enabled, value -> draft.enabled = value);
            check(left + column + 8, 46, column, "localized_input", draft.localizedInput, value -> draft.localizedInput = value);
            check(left, 72, column, "identifier_translation", draft.identifierTranslation, value -> draft.identifierTranslation = value);
            check(left + column + 8, 72, column, "usage_tooltips", draft.usageTooltips, value -> draft.usageTooltips = value);
            check(left, 98, column, "description_search", draft.descriptionSearch, value -> draft.descriptionSearch = value);
            check(left + column + 8, 98, column, "translation_export", draft.translationExport, value -> draft.translationExport = value);
            button(left, 130, column, label("config.identifier_style"), () -> openStyle(Page.IDENTIFIER));
            button(left + column + 8, 130, column, label("config.tag_style"), () -> openStyle(Page.TAG));
            button(left, 156, panelWidth, label("config.tooltips"), () -> openStyle(Page.TOOLTIPS));
            status = centeredLabel(error, height - 58, 20);
            saveButton = button(width / 2 - 114, height - 28, 110, label("config.save"), this::save);
            button(width / 2 + 4, height - 28, 110, Component.translatable("gui.cancel"), this::onClose);
        } else if (page == Page.TOOLTIPS) {
            check(left, 42, column, "suggestion_tooltips", draft.suggestionTooltips, value -> draft.suggestionTooltips = value);
            check(left + column + 8, 42, column, "input_tooltips", draft.inputTooltips, value -> draft.inputTooltips = value);
            check(left, 64, column, "command_syntax", draft.commandSyntax, value -> draft.commandSyntax = value);
            check(left + column + 8, 64, column, "syntax_annotations", draft.syntaxAnnotations, value -> draft.syntaxAnnotations = value);
            check(left, 86, column, "parameter_tooltips", draft.parameterTooltips, value -> draft.parameterTooltips = value);
            check(left + column + 8, 86, column, "compact_parameter_tooltips", draft.compactParameterTooltips, value -> draft.compactParameterTooltips = value);
            check(left, 108, column, "structured_tooltips", draft.structuredTooltips, value -> draft.structuredTooltips = value);
            check(left + column + 8, 108, column, "value_tooltips", draft.valueTooltips, value -> draft.valueTooltips = value);
            check(left, 130, column, "tag_tooltips", draft.tagTooltips, value -> draft.tagTooltips = value);
            check(left + column + 8, 130, column, "item_tooltips", draft.itemTooltips, value -> draft.itemTooltips = value);
            check(left, 152, column, "entity_tooltips", draft.entityTooltips, value -> draft.entityTooltips = value);
            check(left + column + 8, 152, column, "effect_tooltips", draft.effectTooltips, value -> draft.effectTooltips = value);
            button(left, 182, panelWidth, label("config.entity_attributes"), () -> openStyle(Page.ENTITY_ATTRIBUTES));
            button(width / 2 - 55, height - 28, 110, Component.translatable("gui.back"), this::onClose);
        } else if (page == Page.ENTITY_ATTRIBUTES) {
            for (int i = 0; i < ClientConfig.ENTITY_ATTRIBUTE_KEYS.size(); i++) {
                String key = ClientConfig.ENTITY_ATTRIBUTE_KEYS.get(i);
                check(left + (i % 2) * (column + 8), 42 + (i / 2) * 22, column, "entity_attribute." + key,
                        draft.entityAttributes.getOrDefault(key, true), value -> draft.entityAttributes.put(key, value));
            }
            check(left, 152, panelWidth, "other_entity_attributes", draft.otherEntityAttributes, value -> draft.otherEntityAttributes = value);
            button(width / 2 - 55, height - 28, 110, Component.translatable("gui.back"), this::onClose);
        } else {
            ClientConfig.TextStyle style = selectedStyle();
            centeredLabel(label("config.color"), 34, 10);
            int index = 0;
            int paletteLeft = width / 2 - 110;
            for (ChatFormatting color : ChatFormatting.values()) {
                TextColor textColor = TextColor.fromLegacyFormat(color);
                if (textColor == null) { continue; }
                int rgb = textColor.getValue();
                Button swatch = Button.builder(Component.empty(), ignored -> style.color = rgb)
                        .bounds(paletteLeft + (index % 8) * 28, 50 + (index / 8) * 24, 24, 20)
                        .tooltip(Tooltip.create(label("color." + textColor.serialize()))).build();
                control(swatch);
                swatches.add(new Swatch(swatch, rgb));
                index++;
            }
            check(left, 106, column, "bold", style.bold, value -> style.bold = value);
            check(left + column + 8, 106, column, "italic", style.italic, value -> style.italic = value);
            check(left, 130, column, "underlined", style.underlined, value -> style.underlined = value);
            check(left + column + 8, 130, column, "strikethrough", style.strikethrough, value -> style.strikethrough = value);
            check(left, 154, column, "obfuscated", style.obfuscated, value -> style.obfuscated = value);
            check(left + column + 8, 154, column, "show_identifier_on_hover", style.showIdentifierOnHover,
                    value -> style.showIdentifierOnHover = value);
            preview = centeredLabel(previewText(), height - 62, 18);
            button(width / 2 - 55, height - 28, 110, Component.translatable("gui.back"), this::onClose);
        }
        if (saving) {
            controls.forEach(widget -> widget.active = false);
            saveButton.setMessage(label("config.saving"));
        }
    }

    private StringWidget centeredLabel(Component text, int y, int height) {
        int textWidth = Math.min(width - 32, font.width(text));
        StringWidget widget = new StringWidget((width - textWidth) / 2, y, textWidth, height, text, font);
        addRenderableOnly(widget);
        return widget;
    }

    private void check(int x, int y, int width, String key, boolean selected, Consumer<Boolean> update) {
        Checkbox checkbox = ConfigCheckboxes.create(x, y, width, label("config." + key), selected, font);
        control(checkbox);
        bindings.add(new Binding(checkbox, update));
    }

    private Button button(int x, int y, int width, Component text, Runnable action) {
        Button button = Button.builder(text, ignored -> action.run()).bounds(x, y, width, 20).build();
        control(button);
        return button;
    }

    private void control(AbstractWidget widget) {
        addRenderableWidget(widget);
        controls.add(widget);
    }

    private void applyBindings() {
        bindings.forEach(binding -> binding.update().accept(binding.checkbox().selected()));
    }

    private ClientConfig.TextStyle selectedStyle() {
        return page == Page.TAG ? draft.tagStyle : draft.identifierStyle;
    }

    private Component previewText() {
        return label("config.preview").append(": ")
                .append(Component.translatable("biome.minecraft.plains").withStyle(selectedStyle().style()));
    }

    @Override
    public void tick() {
        super.tick();
        if (preview != null) {
            applyBindings();
            Component text = previewText();
            preview.setMessage(text);
            int textWidth = Math.min(width - 32, font.width(text));
            preview.setWidth(textWidth);
            preview.setX((width - textWidth) / 2);
        }
    }

    protected void paintSwatches(RectangleFill fill) {
        for (Swatch swatch : swatches) {
            Button button = swatch.button();
            int x = button.getX();
            int y = button.getY();
            int border = selectedStyle().color == swatch.color() ? 0xFFFFFFFF : 0xFF101010;
            fill.fill(x + 2, y + 2, x + button.getWidth() - 2, y + button.getHeight() - 2, border);
            fill.fill(x + 4, y + 4, x + button.getWidth() - 4, y + button.getHeight() - 4, 0xFF000000 | swatch.color());
        }
    }

    private void openStyle(Page page) {
        applyBindings();
        ClientScreenAccess.open(new CommandConfigScreenView(this, draft, page));
    }

    private void save() {
        applyBindings();
        ClientConfig snapshot = draft.copy();
        error = Component.empty();
        status.setMessage(error);
        saving = true;
        controls.forEach(widget -> widget.active = false);
        saveButton.setMessage(label("config.saving"));
        var directory = Minecraft.getInstance().gameDirectory.toPath().resolve("config");
        CompletableFuture.runAsync(() -> {
            try { ClientConfig.save(directory, snapshot); }
            catch (IOException failure) { throw new UncheckedIOException(failure); }
        }).whenCompleteAsync((ignored, failure) -> {
            if (failure == null) { ClientScreenAccess.open(parent); }
            else {
                CommandTranslation.LOGGER.error("Cannot save command translation configuration", failure);
                saving = false;
                controls.forEach(widget -> widget.active = true);
                saveButton.setMessage(label("config.save"));
                error = label("config.save_failed").withStyle(ChatFormatting.RED);
                status.setMessage(error);
                int textWidth = Math.min(width - 32, font.width(error));
                status.setWidth(textWidth);
                status.setX((width - textWidth) / 2);
            }
        }, Minecraft.getInstance());
    }

    @Override
    public void onClose() {
        if (saving) { return; }
        if (page != Page.GENERAL) { applyBindings(); }
        ClientScreenAccess.open(parent);
    }

    private static net.minecraft.network.chat.MutableComponent label(String key) {
        return Component.translatable(CommandLanguage.PREFIX + key);
    }
}
