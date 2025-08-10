package com.example.lazymcvelocitycrafty;
// Holy shit, that is a lot of imports
import com.lazymcvelocitycrafty.commands.ModeCommand;
import com.lazymcvelocitycrafty.commands.ServerCommand;
import com.lazymcvelocitycrafty.commands.StartStopCommand;
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

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.Optional;

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
  private ModeManager modeManager

  @Inject
  public LazyMCVelocityCrafty(ProxyServer proxy, Logger logger, Path dataDirectory) {
    this.proxy = proxy;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    logger.info("Loading LazyMCVelocityCrafty...");

    config = ConfigLoader.loadConfig(dataDirectory, logger);

    // After loading config and serverManager initialization

    // Initial server manager
    serverManager = new ServerManager(proxy, config, logger);
    
    // Initialization ModeManager
    modeManager = new ModeManager(dataDirectory, logger);
    modeManager.load();
    
    // Register commands
    // /server <server>
    proxy.getCommandManager().register(
      proxy.getCommandManager().metaBuilder("server").build(),
      new ServerCommand(proxy, logger, config, serverManager)
    );
    // /lvmode <server> <mode|view>
    proxy.getCommandManager().register(
      proxy.getCommandManager().metaBuilder("lvmode").permission("lazymc.setmode").build(),
      new ModeCommand(modeManager)
    );
    // /lvstart <server>
    proxy.getCommandManager().register(
      proxy.getCommandManager().metaBuilder("lvstart").permission("lazymc.start").build(),
      new StartStopCommand(serverManager, true)
    );
    // /lvstop <server>
    proxy.getCommandManager().register(
      proxy.getCommandManager().metaBuilder("lvstop").permission("lazymc.stop").build(),
      new StartStopCommand(serverManager, false)
    );

    // Register listener with ModeManager
    proxy.getEventManager().register(this, new PlayerServerConnectListener(this, serverManager, modeManager));
  
    // Schedule inactivity tracker
    InactivityTracker inactivityTracker = new InactivityTracker(proxy, logger, config, modeManager, serverManager, this);
    inactivityTracker.scheduleChecker();
  
    logger.info("LazyMCVelocityCrafty initialized successfully.");
  }

  @com.velocitypowered.api.event.Subscribe
  public void onProxyShutdown(ProxyShutdownEvent event) {
    logger.info("LazyMCVelocityCrafty shutting down...");
    // Add cleanup code if needed
  }

  public ProxyServer getProxy() {
    return proxy;
  }

  public PluginConfig getConfig() {
    return config;
  }

  public ServerManager getServerManager() {
    return serverManager;
  }

  public ModeManager getModeManager() {
    return modeManager;
  }
}
