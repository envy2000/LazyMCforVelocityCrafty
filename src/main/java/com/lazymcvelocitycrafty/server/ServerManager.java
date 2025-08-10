package com.lazymcvelocitycrafty.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lazymcvelocitycrafty.LazyMCVelocityCrafty;
import com.lazymcvelocitycrafty.config.PluginConfig;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles backend server start/stop logic and state tracking.
 */
public class ServerManager {
  
  private final ProxyServer proxy;
  private final Logger logger;
  private final PluginConfig config;
  private final LazyMCVelocityCrafty plugin;
  private final Gson gson = new Gson();

  // Maps server name -> Crafty UUID (from config)
  private final Map<String, String> serverUuids;

  //Your guess is as good as mine. (Maybe I just need to actually learn java)
  public ServerManager(Proxyserver proxy, Logger logger, PluginConfig config, LazyMCVelocityCrafty plugin) {
    this.proxy = proxy;
    this.logger = logger;
    this.config = config;
    this.plugin = plugin;
    this.serverUuids = config.getServerUuidMap(); // { "survival" -> "uuid", ... }
  }

  /**
   * Checks if Velocity knows the server AND it is responsive.
   */
  public boolean isServerOnline(String name) {
    return proxy.getServer(name)
      .map(server -> {
        try {
          return server.ping().join().isPresent();
        } catch (Exception e) {
          return false;
        }
      })
      .orElse(false);
  }

  /**
   * Whether this backend is managed by LazyMCVelocityCrafty.
   */
  public boolean hasServer(String name) {
    return serverUuids.containsKey(name);
  }
  
  /**
   * Starts a backend server via Crafty API.
   */
  public CompletableFuture<Void> startServer(String name) {
    if (!hasServer(name)) {
      return CompletableFuture.failedFuture(new IllegalArgumentException("Unknown server: " + name));
    }

    return CompletableFuture.runAsync(() -> {
      String uuid = serverUuids.get(name);
      String endpoint = config.getCraftyBaseUrl() + "/api/v2/servers/" + uuid + "/action/start_server";
      sendCraftyPost(endpoint);
      logger.info("Triggered start for backend '{}'", name);
    });
  }
  
  /**
   * Stops a backend server via Crafty API.
   */
  public CompletableFuture<Void> stopServer(String name) {
    if (!hasServer(name)) {
      return CompletableFuture.failedFuture(new IllegalArgumentException("Unknown server: " + name));
    }

    return CompletableFuture.runAsync(() -> {
      String uuid = serverUuids.get(name);
      String endpoint = config.getCraftyBaseUrl() + "/api/v2/servers/" + uuid + "/action/stop_server";
      sendCraftyPost(endpoint);
      logger.info("Triggered stop for backend '{}'", name);
    });
  }

  /**
   * Waits for server to be online up to the configured timeout.
   */
  public CompletableFuture<Boolean> waitForServer(String name) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    long timeout = config.getMaxStartWaitSeconds();
    long interval = config.getCheckIntervalSeconds();

    proxy.getScheduler().buildTask(plugin, () -> {
      if (isServerOnline(name)) {
        future.complete(true);
      }
    }).repeat(interval, TimeUnit.SECONDS)
      .delay(0, TimeUnit.SECONDS)
      .schedule();

    // Timeout task
    proxy.getScheduler().buildTask(plugin,  () -> {
      if (!future.isDone()) {
        future.complete(false);
      }
    }).delay(timeout, TimeUnit.SECONDS).schedule();

    return future;
  }

  /**
   * Sends POST request to Crafty API endpoint.
   */
  private void sendCraftyPost(String endpoint) {
    try {
      URL url = new URL(endpoint);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST")
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setRequestProperty("Authorization", config.getCraftyApiKey());
      conn.setDoOutput(true);
      conn.getOutputStream().write("{}".getBytes(StandardCharsets.UTF_8));

      int code = conn.getResponseCode();
      if (code != 200 && code != 204) {
        logger.warn("Crafty API returned status {} for {}", code, endpoint);
      }
      conn.disconnect();
    } catch (Exception e) {
      logger.error("Failed to send POST to Crafty API: {}", e.getMessage(), e);
    }
  }
}
