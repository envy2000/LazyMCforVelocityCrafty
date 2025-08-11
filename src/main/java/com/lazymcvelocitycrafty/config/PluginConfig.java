package com.lazymcvelocitycrafty.config;

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Minimal representation of plugin config loaded from config.toml
 *
 * Expected keys:
 *   crafty.host
 *   crafty.api_key
 *   lobby_server
 *   max_start_wait_seconds
 *   check_interval_seconds
 *   handle_fallback_on_forced_off
 *   [servers.<name>] { uuid = "...", idle_timeout_seconds = 300 }
 */
public class PluginConfig {

  private final Path dataDirectory;
  private final Logger logger;
  
  private String craftyHost;
  private String craftyApiKey;
  private String lobbyServer;
  private int maxStartWaitSeconds = 120;
  private int checkIntervalSeconds = 5;
  private boolean handleFallbackOnForcedOff = true;
  // serverName -> (uuid, idle_timeout_seconds)
  private final Map<String, Map<String, Object>> servers = new HashMap<>();

  private PluginConfig(Path dataDirectory, Logger logger) {
    this.dataDirectory = dataDirectory;
    this.logger = logger;
  }

  /**
   * Load config.toml from plugin data directory.
   * Uses org.tomlj (add to dependencies).
   */
  public static PluginConfig load(Path dataDirectory, Logger logger) throws IOException {
    PluginConfig cfg = new PluginConfig(dataDirectory, logger);
    Path cfgPath = dataDirectory.resolve("config.toml");
    if (!Files.exists(cfgPath)) {
      throw new IOException("config.toml not found: " + cfgPath);
    }
    String raw = Files.readString(cfgPath);
    TomlParseResult res = Toml.parse(raw);

    // Crafty
    cfg.craftyHost = res.getString("crafty.host", "https://127.0.0.1:8443");
    cfg.craftyApiKey = res.getString("crafty.api_key", "");
    // Other
    cfg.lobbyServer = res.getString("lobby_server", "lobby");
    cfg.maxStartWaitSeconds = res.getLong("max_start_wait_seconds", (long) cfg.maxStartWaitSeconds).intValue();
    cfg.checkIntervalSeconds = res.getLong("check_interval_seconds', (long) cfg.checkIntervalSeconds).intValue();
    cfg.handleFallbackOnForcedOff = res.getBoolean("handle_fallback_on_forced_off", true);

    // Per-server tables: toml library returns table for "servers"
    if (res.contains("servers")) {
      var serversTable = res.getTable("servers");
      for (String key : serversTable.keySet()) {
        try {
          var tbl = serversTable.getTable(key);
          if (tbl != null) {
            Map<String, Object> serverCfg = new HashMap<>();
            String uuid = tbl.getString("uuid");
            long idle = tbl.getLong("idle_timeout_seconds", 300L);
            serverCfg.put("uuid", uuid);
            serverCfg.put("idle_timeout_seconds", (int) idle);
            cfg.servers.put(key, serverCfg);
          }
        } catch (Exception ex) {
          logger.warn("Failed to read server config for '{}': {}", key, ex.getMessage());
        }
      }
    }

    logger.info("Loaded config: {} servers", cfg.servers.size());
    return cfg;
  }
      
  // getters
  public String getCraftyHost() { return craftyHost; }
  public String getCraftyApiKey() { return craftyApiKey; }
  public String getLobbyServer() { return lobbyServer; }
  public int getMaxStartWaitSeconds() { return maxStartWaitSeconds; }
  public int getCheckIntervalSeconds() { return checkIntervalSeconds; }
  public boolean isHandleFallbackOnForcedOff() { return handleFallbackOnForcedOff; }

  public Set<String> getManagedServers() { return Collections.unmodifiableSet(servers.keySet()): }

  public Optional<String> getServerUuid(String serverName) {
    var m = servers.get(serverName);
    return (m == null) ? Optional.empty() : Optional.ofNullable((String) m.get("uuid"));
  }

  public int getIdleTimeoutSeconds(String serverName) {
    var m = servers.get(serverName);
    if (m == null) return 300;
    return (int) m.getOrDefault("idle_timeout_seconds", 300);
  }
}
