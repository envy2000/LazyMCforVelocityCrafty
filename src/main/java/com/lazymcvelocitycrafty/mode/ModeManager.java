package com.lazymcvelocitycrafty.mode;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and persists server modes in modes.json under plugin data directory.
 * Auto-converts soft modes to STANDARD on plugin startup when they are non-persistent.
 */
public class ModeManager {
  private final Path modeFile;
  private final Logger logger;
  private final Gson gson = new Gson();
  private final Map<String, ServerMode> modes = new ConcurrentHashMap<>();

  public ModeManager(Path dataDirectory, Logger logger) {
    this.modeFile = dataDirectory.resolve("modes.json");
    this.logger = logger;
  }

  /**
   * Load existing modes.json. Non-persistent soft modes are converted to STANDARD.
   */
  public void load() {
    try {
      if (!Files.exists(modeFile)) {
        save(); // create empty file
        return;
      }
      try (Reader r = Files.newBufferedReader(modeFile)) {
        Type t = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> raw = gson.fromJson(r, t);
        if (raw == null) return;
        for (Map.Entry<String, String> e : raw.entrySet()) {
          try {
            ServerMode m = ServerMode.valueOf(e.getValue());
            // Soft modes should be reset to Standard on restart if non-persistent
            if(!m.isPersistent() && (m == ServerMode.SOFT_FORCE_ON || m == ServerMode.SOFT_FORCE_OFF)) {
              modes.put(e.getKey(), ServerMode.STANDARD);
            } else {
              modes.put(e.getKey(), m);
            }
          } catch (IllegalArgumentException iae) {
            logger.warn("Unknown mode '{}' for server {}, defaulting to STANDARD", e.getValue(), e.getKey());
            modes.put(e.getKey(), ServerMode.STANDARD);
          }
        }
      }
    } catch (Exception ex) {
      logger.error("Failed to load modes.json", ex);
    }
  }

  /**
   * Persist modes map to disk (stringified)
   */
  public synchronized void save() {
    try (Writer w = Files.newBufferedWriter(modeFile)) {
      // serialize as Map<String,String>
      Map<String, String> out = new ConcurrentHashMap<>();
      for (Map.Entry<String, ServerMode> e : modes.entrySet()) out.put(e.getKey(), e.getValue().name());
      gson.toJson(out, w);
    } catch (Exception ex) {
      logger.error("Failed to save modes.json", ex);
    }
  }

  public ServerMode getMode(String server) {
    return modes.getOrDefault(server, ServerMode.STANDARD);
  }

  public void setMode(String server, ServerMode mode) {
    modes.put(server, mode);
    save();
  }

  public boolean canAutoStart(String server) { return getMode(server).allowsAutoStart(); }
  public boolean canShutdown(String server) { return getMode(server).allowsShutdown(); }
}
