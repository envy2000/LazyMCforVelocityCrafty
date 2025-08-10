package com.example.lazymcvelocitycrafty;
//Holy shit, that is a lot of imports
import com.example.lazymcvelocitycrafty.commands.BackendControlCommand;
import com.example.lazymcvelocitycrafty.commands.ServerCommand;
import com.example.lazymcvelocitycrafty.config.ConfigManager;
import com.example.lazymcvelocitycrafty.config.PluginConfig;
import com.example.lazymcvelocitycrafty.server.ServerManager;
import com.google.inject.Inject;
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
)
public class LazyMCVelocityCrafty {
  private final ProxyServer server;
  private final Logger logger;
  private final Path dataDirectory;

  private PluginConfig config;
  private ServerManager serverManager;

  @Inject
  public LazyMCVelocityCrafty(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
    this.server = server;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
  }

  @Subscribe
  public void onProxyInitialize(ProxyInitializeEvent event) {
    logger.info("Loading LazyMCVelocityCrafty...");

    this.config = ConfigManager.loadConfig(dataDirectory.resolve("config.toml"), logger);
    this.serverManager = new ServerManager(server, logger, config);

    server.getCommandManager().register("server", new ServerCommand(serverManager, config), "servers");
    server.getCommandManager().register("backend", new BackendControlCommand(serverManager), "backends");

    logger.info("LazyMCVelocityCrafty initialized successfully.");
  }
}
