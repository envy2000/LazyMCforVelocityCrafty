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
    var serverOpt = event.getResult().getServer();
    if (serverOpt.isEmpty()) return;
    String target = serverOpt.get().getServerInfo().getName();

    if (!serverManager.hasServer(target)) {
      // Not a managed server, let Velocity handle normally
      return;
    }

    ServerMode mode = modeManager.getMode(target);
    boolean online = serverManager.isServerOnline(target);

    if (!online) {
      if (!mode.allowsAutoStart()) {
        // mode disallows auto start: send to lobby (if configured) and deny connect
        player.sendMessage(Component.text(target + " is currently disabled. Redirecting to lobby.").color(NamedTextColor.RED));
        var lobby = plugin.getProxy().getServer(plugin.getConfig().getLobbyServer());
        lobby.ifPresent(r -> player.createConnectionRequest(r).connect());
        event.setResult(ServerPreConnectEvent.ServerResult.denied());
        return;
      } 

      // mode allows auto-start: request start and handle queueing
      player.sendMessage(Component.text(target + " is starting. You will be connected automatically when it is ready.").color(NamedTextColor.YELLOW));
        
      // queue player for auto-connect
      serverManager.addPendingPlayer(target, player.getUniqueId());

      // trigger start (async)
      serverManager.startServer(target).exceptionally(ex -> {
        player.sendMessage(Component.text("Failed to request server start: " + ex.getMessage()).color(NamedTextColor.RED));
        return null;
      });

      // redirect player to lobby if not already there (we keep players on backend per earlier rule only for /server from backend;
      // ServerPreConnectEvent cannot tell whether this was a /server from backend easily in all cases - for simplicity,
      // we send to lobby non-online targets.)
      var lobby = plugin.getProxy().getServer(plugin.getConfig().getLobbyServer());
      lobby.ifPresent(r -> player.createConnectionRequest(r).connect());
      event.setResult(ServerPreConnectEvent.ServerResult.denied());
      return;
    }

    // server is online - let Velocity handle the connection normally
  }
}
