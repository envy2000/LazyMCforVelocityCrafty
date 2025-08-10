package com.lazymcvelocitycrafty.commands;

import com.lazymcvelocitycrafty.config.PluginConfig;
import com.lazymcvelocitycrafty.server.ServerManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

public class ServerCommand implements SimpleCommand {

  private final ProxyServer proxy;
  private final Logger logger;
  private final PluginConfig config;
  private final ServerManager serverManager;

  public ServerCommand(ProxyServer proxy, Logger logger, PluginConfig config, ServerManager serverManager) {
    this.proxy = proxy;
    this.logger = logger;
    this.config = config;
    this.serverManager = serverManager;
  }

  @Override
  public void execute(Invocation invocation) {
    CommandSource source = invocation.source();
    String[] args = invocation.arguments();

    if (!(source instanceof Player player)) {
      source.sendMessage(Component.text("This command can only be used by players."));
      return;
    }

    if (args.length != 1) {
      player.sendMessage(Component.text("Usage: /server <name>"));
      return;
    }

    String target = args[0];

    if (!serverManager.hasServer(target)) {
      player.sendMessage(Component.text("Unknown server: " + target));
      return;
    }

    if (serverManager.isServerOnline(target)) {
      proxy.getServer(target).ifPresent(server -> player.createConnectionRequest(server).connect());
      return;
    }

    // Start server
    player.sendMessage(Component.text(target + " is currently starting. You will be connected automatically."));
    serverManager.startServer(target).thenRun(() -> {
      CompletableFuture<Boolean> ready = serverManager.waitForServer(target);
      ready.thenAccept(success -> {
        if (success) {
          proxy.getServer(target).ifPresent(server -> player.createConnectionRequest(server).connect());
        } else {
          player.sendMessage(Component.text("Failed to connect to " + target + " (startup timeout)."));
        }
      });
    });
  }
}
