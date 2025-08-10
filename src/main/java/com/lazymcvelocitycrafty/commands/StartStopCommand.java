package com.lazymcvelocitycrafty.commands;

import com.lazymcvelocitycrafty.server.ServerManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.concurrent.CompletableFuture;

/**
 * /lvstart <server>  and  /lvstop <server>
 */
public class StartStopCommand implements SimpleCommand {

  private final ServerManager serverManager;
  private final boolean startAction;

  public StartStopCommand(ServerManager serverManager, boolean startAction) {
    this.serverManager = serverManager;
    this.startAction = startAction;
  }

  @Override
  public void execute(Invocation invocation) {
    CommandSource src = invocation.source();
    String[] args = invocation.arguments();
    if (args.length != 1) {
      src.sendMessage(Component.text("Usage: /" + (startAction ? "lvstart" : "lvstop") + " <server>").color(NamedTextColor.YELLOW));
      return;
    }
    String server = args[0];
    if (!serverManager.hasServer(server)) {
      src.sendMessage(Component.text("Unknown managed server: " + server).color(NamedTextColor.RED));
      return;
    }

    CompletableFuture<Void> fut = startAction ? serverManager.startServer(server) : serverManager.stopServer(server);
    fut.whenComplete((v, ex) -> {
      if (ex != null) {
        src.sendMessage(Component.text("Operation failed: " + ex.getMessage()).color(NamedTextColor.RED));
      } else {
        src.sendMessage(Component.text((startAction ? "Start" : "Stop") + " requested for " + server).color(NamedTextColor.GREEN));
      }
    });
  }
}
