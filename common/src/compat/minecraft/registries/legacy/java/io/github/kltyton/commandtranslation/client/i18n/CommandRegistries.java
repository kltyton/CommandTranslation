package io.github.kltyton.commandtranslation.client.i18n;

import java.util.stream.Stream;
import io.github.kltyton.commandtranslation.client.ClientPlatform;
import net.minecraft.core.Registry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class CommandRegistries {
    public static final Registry<Item> ITEM = Registry.ITEM;
    public static final Registry<Block> BLOCK = Registry.BLOCK;
    public static final Registry<EntityType<?>> ENTITY_TYPE = Registry.ENTITY_TYPE;
    public static final Registry<Attribute> ATTRIBUTE = Registry.ATTRIBUTE;
    public static final Registry<MobEffect> MOB_EFFECT = Registry.MOB_EFFECT;
    public static final Registry<? extends Registry<?>> REGISTRY = Registry.REGISTRY;

    private CommandRegistries() {}

    public static Stream<Registry<?>> serverRegistries() {
        return ServerRegistries.stream();
    }

    public static String id(Registry<?> registry) { return ClientPlatform.registryId(registry.key()); }
}
