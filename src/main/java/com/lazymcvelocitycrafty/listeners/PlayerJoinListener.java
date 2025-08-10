package com.lazymcvelocitycrafty.listeners;

import com.lazymcvelocitycrafty.config.PluginConfig;
import com.lazymcvelocitycrafty.server.ServerManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

public class PlayerJoinListener {

  private final ProxyServer proxy;
  private final PluginConfig config;
  private final ServerManager serverManager;
  private final Logger logger;

  public PlayerJoinListener(ProxyServer proxy, PluginConfig config, ServerManager serverManager, Logger logger) {
    this.proxy = proxy;
    this.config = config;
    this.serverManager = serverManager;
    this.logger = logger;
  }

  @Subscribe
  public void onPostLogin(PostLoginEvent event) {
    Player player = event.getPlayer();
    proxy.getServer(config.getLobbyServer()).ifPresent(server -> {
      player.createConnectionRequest(server).connect();
    });
  }
}
