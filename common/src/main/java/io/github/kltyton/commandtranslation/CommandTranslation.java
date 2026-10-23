package io.github.kltyton.commandtranslation;

import net.minecraft.SharedConstants;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public final class CommandTranslation {
    public static final Logger LOGGER = LogManager.getLogger(BuildInfo.MOD_ID);

    private CommandTranslation() {
    }

    public static void initialize() {
        LOGGER.info("Initializing {} for Minecraft {}",
                BuildInfo.MOD_ID, SharedConstants.getCurrentVersion());
    }
}
