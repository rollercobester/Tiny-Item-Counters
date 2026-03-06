package qoby.tiny_item_counters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class TinyItemCountersConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("tiny-item-counters.json");

    public static boolean shrinkItemCount = true;
    public static boolean shrinkDurabilityBar = true;

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("shrinkItemCount"))
                shrinkItemCount = json.get("shrinkItemCount").getAsBoolean();
            if (json.has("shrinkDurabilityBar"))
                shrinkDurabilityBar = json.get("shrinkDurabilityBar").getAsBoolean();
        } catch (IOException e) {
            save();
        }
    }

    public static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("shrinkItemCount", shrinkItemCount);
        json.addProperty("shrinkDurabilityBar", shrinkDurabilityBar);
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(json, writer);
        } catch (IOException ignored) {
        }
    }
}
