package io.github.kltyton.commandtranslation.client.i18n;

import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;

public final class ExportPackMetadata {
    private ExportPackMetadata() {}

    public static JsonObject create(String description) {
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", SharedConstants.getCurrentVersion().getPackVersion());
        pack.addProperty("description", description);
        JsonObject metadata = new JsonObject();
        metadata.add("pack", pack);
        return metadata;
    }
}
