package com.lazymcvelocitycrafty.tracker;

import com.lazymcvelocitycrafty.LazyMCVelocityCrafty;
import com.lazymcvelocitycrafty.config.PluginConfig;
import com.lazymcvelocitycrafty.mode.ModeManager;
import com.lazymcvelocitycrafty.server.ServerManager;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Tracks last activity timestamps and triggers Crafty stop when idle timeout reached.
 */
public class InactivityTracker {

  private final ProxyServer proxy;
  private final Logger logger;
  private final PluginConfig config;
  private final ModeManager modeManager;
  private final ServerManager serverManager;
  private final LazyMCVelocityCrafty plugin;

  // serverName -> last active instant
  private final Map<String, Instant> lastActive = new ConcurrentHashMap<>();

  public InactivityTracker(ProxyServer proxy, Logger logger, PluginConfig config,
                           ModeManager modeManager, ServerManager serverManager, LazyMCVelocityCrafty plugin) {
    this.proxy = proxy;
    this.logger = logger;
    this.config = config;
    this.modeManager = modeManager;
    this.serverManager = serverManager;
    this.plugin = plugin;
  }

  public void recordActivity(String serverName) {
    lastActive.put(serverName, Instant.now());
  }

  /**
   * Schedule the periodic checker. Call this once on plugin init.
   */
  public void scheduleChecker() {
    long period = config.getInactivityCheckIntervalSeconds();
    proxy.getScheduler().buildTask(plugin, this::checkAll).repeat(period, TimeUnit.SECONDS).schedule();
  }

  private void checkAll() {
    for (String server : config.getManagedServerNames()) {
      // skip non-managed or if mode prevents shutdown
      if (!modeManager.canShutdown(server)) continue;

      long idleSeconds = config.getIdleTimeoutSeconds(server);
      Instant last = lastActive.getOrDefault(server, Instant.now());
      if (Instant.now().minusSeconds(idleSeconds).isAfter(last)) {
        // double-check player count: if there are players, skip
        int players = serverManager.getPlayerCount(server);
        if (players > 0) {
          // players joined since lastActive
          recordActivity(server);
          continue;
        }

        // perform shutdown
        logger.info("Shutting down idle server {} after {}s", server, idleSeconds);
        serverManager.stopServer(server).whenComplete((v, ex) -> {
          if (ex != null) logger.warn("Failed to stop {}", server, ex);
          else {
            if (config.isHandleFallbackOnForcedOff()) {
              serverManager.movePlayersToLobby(server, config.getLobbyServer());
            }
          }
        });
      }
    }
  }
}
