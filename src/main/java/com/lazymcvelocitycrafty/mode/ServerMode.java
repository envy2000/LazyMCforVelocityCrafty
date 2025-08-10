package com.lazymcvelocitycrafty.mode;

/**
 * Mode definitions for backends.
 *
 * Behavior encoded:
 *  - allowAutoStart: whether plugin should auto-start the backend on join
 *  - allowShutdown: whether inactivity shutdown is allowed
 *  - persistent: whether mode persists across proxy restarts (true = persists)
 */
public enum ServerMode {
  STANDARD(true, true, true),         // default: auto-start + shutdown allowed + persisted
  SOFT_FORCE_ON(true, false, false),  // auto-start, no shutdown while running, resets on restart
  HARD_FORCE_ON(true, false, true),   // auto-start, never shut down, persists
  SOFT_FORCE_OFF(false, true, false), // no auto-start, may shutdown, resets on restart
  HARD_FORCE_OFF(false, false, true); // never auto-start, ensure stopped, persists

  private final boolean allowAutoStart;
  private final boolean allowShutdown;
  private final boolean persistent;

  ServerMode(boolean allowAutoStart, boolean allowShutdown, boolean persistent) {
    this.allowAutoStart = allowAutoStart;
    this.allowShutdown = allowShutdown;
    this.persistent = persistent;
  }

  public boolean allowsAutoStart() { return allowAutoStart; }
  public boolean allowsShutdown() { return allowShutdown; }
  public boolean isPersistent() { return persistent; }
}
