package io.github.kltyton.commandtranslation.client.mixin;

import java.util.Map;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Enumerates the server-owned registries before RegistryAccess exposed a public stream. */
@Mixin(RegistryAccess.RegistryHolder.class)
public interface ClientRegistryAccessAccessor {
    @Accessor("registries")
    Map<? extends ResourceKey<? extends Registry<?>>, ? extends MappedRegistry<?>> commandTranslation$registries();
}
