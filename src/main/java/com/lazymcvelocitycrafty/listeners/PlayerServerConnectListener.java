package com.lazymcvelocitycrafty.listeners;

import com.lazymcvelocitycrafty.LazyMCVelocityCrafty;
import com.lazymcvelocitycrafty.mode.ModeManager;
import com.lazymcvelocitycrafty.mode.ServerMode;
import com.lazymcvelocitycrafty.server.ServerManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Optional;

public class PlayerServerConnectListener {

  private final LazyMCVelocityCrafty plugin;
  private final ServerManager serverManager;
  private final ModeManager modeManager;

  public PlayerServerConnectListener(LazyMCVelocityCrafty plugin, ServerManager serverManager, ModeManager modeManager) {
    this.plugin = plugin;
    this.serverManager = serverManager;
    this.modeManager = modeManager;
  }

  @Subscribe
  public void onServerPreConnect(ServerPreConnectEvent event) {
    Player player = event.getPlayer();
    String targetServer = event.getResult().getServer().map(s -> s.getServerInfo().getName()).orElse(null);
    if (targetServer == null) {
      // No target server, ignore
      return;
    }

    if (!serverManager.hasServer(targetServer)) {
      // Not a managed server, let Velocity handle normally
      return;
    }

    ServerMode mode = modeManager.getMode(targetServer);

    // Check if backend is online
    boolean isOnline = serverManager.isServerOnline(target);

    if (!isOnline) {
      if (!mode.allowsAutoStart()) {
        // Server is off and mode disallows auto start - redirect to lobby
        event.setResult(ServerPreConnectEvent.ServerResult.denied());
        Optional<com.velocitypowered.api.proxy.server.RegisteredServer> lobby = plugin.getProxy().getServer(plugin.getConfig().getLobbyServer());
        if (lobby.isPresent()) {
          player.sendMessage(Component.text(targetServer + " is currently disabled. Redirecting to lobby.").color(NamedTextColor.RED));
          player.createConnectionRequest(lobby.get()).fireAndForget();
        } else {
          player.sendMessage(Component.text("Lobby server is not configured or offline.".color(NamedTextColor.RED));
        }
        return;
      } else {
        // Allowed to auto start - start backend and notify player
        player.sendMessage(Component.text(targetServer + " is starting up. You will be connected automatically when ready.").color(NamedTextColor.YELLOW));
        serverManager.startServer(targetServer);
        // Redirect player to lobby if they aren't already there
        Optional<com.velocitypowered.api.proxy.server.RegisteredServer> currentServer = player.getCurrentServer();
        String lobbyName = plugin.getConfig().getLobbyServer();
        if (currentServer.isEmpty() || !currentServer.get().getServerInfo().getName().equals(lobbyName)) {
          plugin.getProxy().getServer(lobbyName).ifPresent(s -> {
            player.createConnectionRequest(s).fireAndForget();
          });
        }
        // Cancel this connect event because server is offline, player redirected
        event.setResult(ServerPreConnectEvent.ServerResult.denied());
        return;
      }
    }

    // server is online - let Velocity handle the connect normally
  }
}
