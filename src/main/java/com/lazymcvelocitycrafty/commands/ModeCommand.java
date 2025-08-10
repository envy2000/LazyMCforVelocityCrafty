package com.lazymcvelocitycrafty.commands;

import com.lazymcvelocity.mode.ModeManager;
import com.lazymcvelocity.mode.ServerMode;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * /lvmode <server> <mode>  (also supports "view")
 */
public class ModeCommand implements SimpleCommand {

  private final ModeManager modeManager;

  public ModeCommand(ModeManager modeManager) {
    this.modeManager = modeManager;
  }

  @Override
  public void execute(Invocation invocation) {
    CommandSource src = invocation.source();
    String[] args = invocation.arguments();

    if (args.length == 0) {
      src.sendMessage(Component.text("Usage: /lvmode <server> <mode|view>").color(NamedTextColor.YELLOW));
      return;
    }

    String server = args[0];
    if (args.length == 1 || "view".equalsIgnoreCase(args[1])) {
      ServerMode cur = modeManager.getMode(server);
      src.sendMessage(Component.text(server + " mode: " + cur.name()).color(NamedTextColor.GREEN));
      return;
    }

    String modeName = args[1].toUpperCase();
    try {
      ServerMode newMode = ServerMode.valueOf(modeName);
      modeManager.setMode(server, newMode);
      src.sendMessage(Component.text("Set " + server + " -> " + newMode.name()).color(NamedTextColor.GREEN));
      // Immediate actions for hard modes are handled elsewhere (caller may trigger)
    } catch (IllegalArgumentException iae) {
      src.sendMessage(Component.text("Unknown mode: " + args[1]).color(NamedTextColor.RED));
    }
  }
}
