package com.lazymcvelocitycrafty.server;

import com.lazymcvelocitycrafty.LazyMCVelocityCrafty;
import com.lazymcvelocitycrafty.config.PluginConfig;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * Controls start/stop requests to Crafty v2 API and tracks pending connection queues.
 */
public class ServerManager {
  
  private final LazyMCVelocityCrafty plugin;
  private final PluginConfig config;
  private final Logger logger;
  private final HttpClient http;

  // pending players per server - plugin may use this to auto-connect when ready
  private final ConcurrentMap<String, CopyOnWriteArrayList<java.util.UUID>> pendingPlayers = new ConcurrentHashMap<>();
  private final ScheduledExecutorService poller = Executors.newScheduledThreadPool(1);

  //Your guess is as good as mine. (Maybe I just need to actually learn java)
  public ServerManager(LazyMCVelocityCrafty plugin, PluginConfig config, Logger logger) {
    this.plugin = plugin;
    this.config = config;
    this.logger = logger;
    this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  }
  
  /**
   * Whether this backend is managed by LazyMCVelocityCrafty.
   */
  public boolean hasServer(String name) {
    return config.getManagedServers().contains(name);
  }

  /**
   * Non-blocking start via Crafty v2 API. Returns a CompletableFuture that completes when the POST
   * request has been sent and responded (not when the Minecraft server is fully online).
   */
  public CompletableFuture<Void> startServer(String name) {
    Optional<String> uuidOpt = config.getServerUuid(name);
    if (uuidOpt.isEmpty) {
      return CompletableFuture.failedFuture(new IllegalArgumentException("Unknown server: " + name));
    }
    String uuid = uuidOpt.get();
    String url = String.format("%s/api/v2/servers/%s/action/start_server", trimSlash(config.getCraftyHost()), uuid);

    HttpRequest req = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .timeout(Duration.ofSeconds(20))
      .header("Content-Type", "application/json")
      .header("Authorization", bearerHeader(config.getCraftyApiKey()))
      .POST(HttpRequest.BodyPublishers.ofString("{}"))
      .build();
    
    logger.info("Sending Crafty start request for {} -> {}", name, url);
    return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
      .thenAccept(resp -> {
        if (resp.statusCode() / 100 == 2) {
          logger.info("Crafty accepted start for {}", name);
        } else {
          logger.warn("Crafty start for {} returned status {}: {}", name, resp.statusCode(), resp.body());
        }
      });
  }

  /**
   * Stops a backend server via Crafty API.
   */
  public CompletableFuture<Void> stopServer(String name) {
    Optional<String> uuidOpt = config.getServerUuid(name);
    if (uuid.isEmpty()) {
      return CompletableFuture.failedFuture(new IllegalArgumentException("Unknown server: " + name));
    }
    String uuid = uuidOpt.get();
    String url = String.format("%s/api/v2/servers/%s/action/stop_server", trimSlash(config.getCraftyHost()), uuid);

    HttpRequest req = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .timeout(Duration.ofSeconds(20))
      .header("Content-Type", "application/json")
      .header("Authorization", bearerHeader(config.getCraftyApiKey()))
      .POST(HttpRequest.BodyPublishers.ofString("{}"))
      .build();

    logger.info("Sending Crafty stop request for {} -> {}", name, url);
    return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
      .thenAccept(resp -> {
        if (resp.statusCode() / 100 == 2) {
          logger.info("Crafty accepted stop for {}", name);
        } else {
          logger.warn("Crafty stop for {} returned status {}: {}", name, resp.statusCode(), resp.body());
        }
      });
  }
 
  /**
   * Returns true if Velocity registers the server and its ping succeeds.
   */
  public boolean isServerOnline(String name) {
    return plugin.getProxy().getServer(name)
      .map(rs -> {
        try {
          return rs.ping().join().isPresent();
        } catch (Exception e) {
          return false;
        }
      }).orElse(false);
  }

  /**
   * Waits for server to become online, up to timeout seconds (non-blocking).
   */
  public CompletableFuture<Boolean> waitForServerOnline(String name, int timeoutSeconds, int pollIntervalSeconds) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;

    Runnable check = new Runnable() {
      @Override
      public void run() {
        if (isServerOnline(name)) {
          future.complete(true);
        } else if (System.currentTimeMillis() > deadline) {
          future.complete(false);
        }
      }
    }

    ScheduledFuture<?> task = poller.scheduleAtFixedRate(check, 0, Math.max(1, pollIntervalSeconds), TimeUnit.SECONDS);
    future.whenComplete((ok, ex) -> task.cancel(true));
    return future;
  }

  /**
   * Helpers for pending players queue (we store UUIDs; plugin must map to Player when connecting)
   */
  public void addPendingPlayer(String serverName, java.util.UUID playerUuid) {
    pendingPlayers.computeIfAbsent(serverName, k -> new CopyOnWriteArrayList<>()).add(playerUuid);
  }
  
  public java.util.List<java.util.UUID> drainPendingPlayers(String serverName) {
    var list = pendingPlayers.remove(serverName);
    return (list == null) ? java.util.List.of() : java.util.List.copyOf(list);
  }
  
  // returns number of players on that backend (0 if not present)
  public int getPlayerCount(String serverName) {
    return (int) plugin.getProxy().getAllPlayers().stream()
      .filter(p -> p.getCurrentServer().map(c -> c.getServerInfo().getName().equals(serverName)).orElse(false))
      .count();
  }
  
  // move players to lobby - used when forcibly stopping with fallback
  public void movePlayersToLobby(String serverName, String lobbyName) {
    var lobby = plugin.getProxy().getServer(lobbyName);
    if (lobby.isEmpty()) return;
    var reg = lobby.get();
    plugin.getProxy().getAllPlayers().stream()
      .filter(p -> p.getCurrentServer().map(c -> c.getServerInfo().getName().equals(serverName)).orElse(false))
      .forEach(p -> p.createConnectionRequest(reg).connect());
  }
  
  private static String bearerHeader(String key) {
    if (key == null) return "";
    if (key.startsWith("Bearer ")) return key;
    return "Bearer " + key;
  }

  private static String trimSlash(String s) {
    if (s == null) return "";
    if (s.endsWith("/")) return s.substring(0, s.length() - 1);
    return s;
  }

  public void shutdown() {
    poller.shutdownNow();
  }
}
