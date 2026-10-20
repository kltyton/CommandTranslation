package io.github.kltyton.commandtranslation.client.i18n;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.PackType;

public final class ExportPackMetadata {
    private ExportPackMetadata() {}

    public static JsonObject create(String description) {
        var version = SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES);
        JsonArray exact = new JsonArray();
        exact.add(version.major());
        exact.add(version.minor());
        JsonObject pack = new JsonObject();
        pack.add("min_format", exact);
        pack.add("max_format", exact.deepCopy());
        pack.addProperty("description", description);
        JsonObject metadata = new JsonObject();
        metadata.add("pack", pack);
        return metadata;
    }
}
