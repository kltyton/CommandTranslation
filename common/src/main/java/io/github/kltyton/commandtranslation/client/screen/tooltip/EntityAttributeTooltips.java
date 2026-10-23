package io.github.kltyton.commandtranslation.client.screen.tooltip;

import io.github.kltyton.commandtranslation.client.i18n.MinecraftText;
import io.github.kltyton.commandtranslation.client.config.ClientConfig;
import java.math.BigDecimal;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

/** Formats registered defaults; absent attributes are not replaced with invented values. */
public final class EntityAttributeTooltips {
    public record Value(String id, Component name, double value) {}
    private EntityAttributeTooltips() {}

    public static Component describe(EntityType<?> type, Component title, Component description) {
        var values = EntityAttributeValues.read(type).stream()
                .filter(value -> ClientConfig.current().entityAttributeEnabled(value.id())).toList();
        if (values.isEmpty()) { return null; }
        var result = title.copy();
        for (Value value : values) {
            Component amount = MinecraftText.literal(BigDecimal.valueOf(value.value()).round(java.math.MathContext.DECIMAL32).stripTrailingZeros().toPlainString());
            Component line = value.id().equals("minecraft:max_health") || value.id().equals("minecraft:generic.max_health")
                    ? MinecraftText.translatable("command_translation.help.entity.maximum_health", amount)
                    : MinecraftText.translatable("command_translation.help.entity.default_attribute", value.name(), amount);
            result.append("\n").append(line.copy().withStyle(ChatFormatting.GRAY));
        }
        if (description != null) { result.append("\n").append(description); }
        return result;
    }
}
