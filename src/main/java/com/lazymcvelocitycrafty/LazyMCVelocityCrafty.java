package com.example.lazymcvelocitycrafty;
//Holy shit, that is a lot of imports
import com.google.inject.Inject;
import com.lazymcvelocitycrafty.commands.ServerCommand;
import com.lazymcvelocitycrafty.config.ConfigLoader;
import com.lazymcvelocitycrafty.config.PluginConfig;
import com.lazymcvelocitycrafty.listeners.PlayerJoinListener;
import com.lazymcvelocitycrafty.server.ServerManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
  id = "lazymcvelocitycrafty",
  name = "LazyMCVelocityCrafty",
  version = "1.0.0",
  description = "Manages Velocity backend server startup/shutdown through Crafty Controller v2 API"
  authors = {"RealNV2k"}
)
public class LazyMCVelocityCrafty {
  
  private final ProxyServer proxy;
  private final Logger logger;
  private final Path dataDirectory;
  private PluginConfig config;
  private ServerManager serverManager;

  @Inject
  public LazyMCVelocityCrafty(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
    this.proxy = proxy;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    logger.info("Loading LazyMCVelocityCrafty...");

    config = ConfigLoader.loadConfig(dataDirectory, logger);
    serverManager = new ServerManager(proxy, logger, config, this);

    //register commands
    proxy.getCommandManager().register(
      proxy.getCommandManager().metaBuilder("server").build(),
      new ServerCommand(proxy, logger, config, serverManager)
    );

    //register listeners
    proxy.getEventManager().register(this, new PlayerJoinListener(proxy, config, serverManager, logger));
    
    logger.info("LazyMCVelocityCrafty initialized successfully.");
  }

  public ProxyServer getProxy() {
    return proxy;
  }

  public Logger getLogger() {
    return logger;
  }

  public PluginConfig getConfig() {
    return config;
  }

  public ServerManager getServerManager() {
    return serverManager;
  }
}
