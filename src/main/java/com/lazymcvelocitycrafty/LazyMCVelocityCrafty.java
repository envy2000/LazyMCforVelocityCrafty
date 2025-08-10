package com.example.lazymcvelocitycrafty;
// Holy shit, that is a lot of imports
import com.google.inject.Inject;
import com.lazymcvelocitycrafty.commands.ServerCommand;
import com.lazymcvelocitycrafty.config.ConfigLoader;
import com.lazymcvelocitycrafty.config.PluginConfig;
import com.lazymcvelocitycrafty.listeners.PlayerServerConnectListener;
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

    // create ModeManager
    ModeManager modeManager = new ModeManager(dataDirectory, logger);
    modeManager.load();
    this.modeManager = modeManager;

    // register commands
    proxy.getCommandManager().register(
      proxy.getCommandManager().metaBuilder("server").build(),
      new ServerCommand(proxy, logger, config, serverManager)
    );
    proxy.getCommandManager().register(proxy.getCommandManager().metaBuilder("lvmode").build(), new ModeCommand(modeManager));
    proxy.getCommandManager().register(proxy.getCommandManager().metaBuilder("lvstart").permission("lazymc.start").build(), new StartStopCommand(serverManager, true));
    proxy.getCommandManager().register(proxy.getCommandManager().metaBuilder("lvstop").permission("lazymc.stop").build(), new StartStopCommand(serverManager, false));

    // start inactivity tracker
    InactivityTracker inactivityTracker = new InactivityTracker(proxy, logger, config, modeManager, serverManager, this);
    inactivityTracker.scheduleChecker();

    // register listeners
    proxy.getEventManager().register(this, new PlayerServerConnectListener(proxy, config, serverManager, logger));
    
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
