package com.lazymcvelocitycrafty.tracker;

import com.lazymcvelocitycrafty.LazyMCVelocityCrafty;
import com.lazymcvelocitycrafty.config.PluginConfig;
import com.lazymcvelocitycrafty.mode.ModeManager;
import com.lazymcvelocitycrafty.server.ServerManager;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Tracks last activity timestamps and triggers Crafty stop when idle timeout reached.
 */
public class InactivityTracker {

  private final LazyMCVelocityCrafty plugin;
  private final PluginConfig config;
  private final ModeManager modeManager;
  private final ServerManager serverManager;
  private final Logger logger;

  // serverName -> last active instant
  private final Map<String, Instant> lastActivity = new ConcurrentHashMap<>();

  public InactivityTracker(LazyMCVelocityCrafty plugin, PluginConfig config,
                           ModeManager modeManager, ServerManager serverManager, Logger logger) {
    this.plugin = plugin;
    this.config = config;
    this.modeManager = modeManager;
    this.serverManager = serverManager;
    this.logger = logger;
  }

  public void recordActivity(String serverName) {
    lastActivity.put(serverName, Instant.now());
  }

  /**
   * Schedule the periodic checker. Call this once on plugin init.
   */
  public void scheduleChecker() {
    long period = config.getCheckIntervalSeconds();
    plugin.getProxy().getScheduler().buildTask(plugin, this::checkAll).repeat(period, TimeUnit.SECONDS).schedule();
  }

  private void checkAll() {
    for (String server : config.getManagedServers()) {
      // skip non-managed or if mode prevents shutdown
      if (!modeManager.canShutdown(server)) continue;

      int idleTimeout = config.getIdleTimeoutSeconds(server);
      Instant last = lastActivity.getOrDefault(server, Instant.now());
      if (Instant.now().minusSeconds(idleTimeout).isAfter(last)) {
        // double-check player count: if there are players, skip
        int players = serverManager.getPlayerCount(server);
        if (players > 0) {
          // players joined since lastActive
          lastActivity.put(server, Instant.now());
          continue;
        }

        // perform shutdown
        logger.info("Server {} idle for {}s; initiating shutdown.", server, idleTimeout);
        serverManager.stopServer(server).whenComplete((v, ex) -> {
          if (ex != null) {
            logger.warn("Failed to stop {}: {}", server, ex.getMessage());
          } else {
            if (config.isHandleFallbackOnForcedOff()) {
              serverManager.movePlayersToLobby(server, config.getLobbyServer());
            }
          }
        });
      }
    }
  }
}
