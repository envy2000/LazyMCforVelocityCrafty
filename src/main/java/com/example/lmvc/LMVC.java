package com.example.lmvc;

import com.example.lmvc.commands.ModeCommand;
import com.example.lmvc.commands.StartStopCommand;
import com.example.lmvc.config.ConfigManager;
import com.example.lmvc.config.ModeManager;
import com.example.lmvc.config.VelocityConfigReader;
import com.example.lmvc.crafty.CraftyApiClient;
import com.example.lmvc.events.PlayerServerConnectListener;
import com.example.lmvc.events.ServerIdleTracker;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Main plugin entrypoint for LazyVelocity.
 */
@Plugin(
        id = "lmvc",
        name = "LazyMCVelocityCrafty",
        version = "0.1.0",
        description = "Auto-start/stop velocity backend servers via Crafty",
        authors = {"RealNV2k"}
)
public class LazyMCVelocityCraftyPlugin {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    public final ConfigManager configManager;
    public final ModeManager modeManager;
    public final VelocityConfigReader velocityConfigReader;
    public final CraftyApiClient craftyApiClient;

    /** pendingConnections: serverName -> list of players waiting to be connected automatically */
    public final ConcurrentMap<String, List<com.velocitypowered.api.proxy.Player>> pendingConnections = new ConcurrentHashMap<>();

    @Inject
    public LazyMCVelocityCraftyPlugin(ProxyServer proxy,
                              Logger logger,
                              @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;

        // Initialize managers (they will load their data in onProxyInitialization)
        this.configManager = new ConfigManager(dataDirectory, logger);
        this.modeManager = new ModeManager(dataDirectory, logger);
        this.velocityConfigReader = new VelocityConfigReader(dataDirectory, logger);
        this.craftyApiClient = new CraftyApiClient(configManager, logger);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("LazyMCVelocityCrafty initializing...");

        // Load configuration + modes + velocity forced-hosts
        configManager.load();
        modeManager.load();
        velocityConfigReader.loadForcedHosts();

        // Register listeners and commands
        proxy.getEventManager().register(this,
                new PlayerServerConnectListener(proxy, logger, configManager, modeManager, velocityConfigReader, craftyApiClient, this)
        );

        proxy.getEventManager().register(this,
                new ServerIdleTracker(proxy, logger, configManager, modeManager, craftyApiClient, this)
        );

        CommandManager cm = proxy.getCommandManager();
        cm.register(cm.metaBuilder("lmvc").build(), new ModeCommand(modeManager, logger, configManager));
        cm.register(cm.metaBuilder("lvstart").permission("lmvc.start").build(), new StartStopCommand(craftyApiClient, configManager, logger, true));
        cm.register(cm.metaBuilder("lvstop").permission("lmvc.stop").build(), new StartStopCommand(craftyApiClient, configManager, logger, false));

        // Schedule a background checker to process pendingConnections (start/complete)
        schedulePendingConnectionChecker();

        logger.info("LazyMCVelocityCrafty initialized.");
    }

    private void schedulePendingConnectionChecker() {
        // TODO: implement scheduled task that:
        //  - iterates pendingConnections map
        //  - polls CraftyAPI for server readiness
        //  - when ready, connects players via ProxyServer#createConnectionRequest(...)
        // Use proxy.getScheduler().buildTask(this, runnable).repeat(...)
    }

    // Helper to add a pending player to a server queue
    public void addPendingPlayer(String serverName, com.velocitypowered.api.proxy.Player player) {
        pendingConnections.computeIfAbsent(serverName, k -> new CopyOnWriteArrayList<>()).add(player);
    }

    // Helper to remove pending players after connecting
    public void removePendingPlayers(String serverName) {
        pendingConnections.remove(serverName);
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }
}
