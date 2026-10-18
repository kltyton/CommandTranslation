package io.github.kltyton.commandtranslation;

import net.minecraft.SharedConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CommandTranslation {
    public static final Logger LOGGER = LoggerFactory.getLogger(BuildInfo.MOD_ID);

    private CommandTranslation() {
    }

    public static void initialize() {
        LOGGER.info("Initializing {} for Minecraft {}",
                BuildInfo.MOD_ID, SharedConstants.getCurrentVersion());
    }
}
