package com.lazymcvelocitycrafty.config;

import java.util.Map;

public class PluginConfig {
  private String craftyBaseUrl;
  private String craftyApiKey;
  private String lobbyServer;
  private long maxStartWaitSeconds;
  private long checkIntervalSeconds;
  private Map<String, String> serverUuidMap;

  public String getCraftyBaseUrl() { return craftyBaseUrl; }
  public String getCraftyApiKey() { return craftyApiKey; }
  public String getLobbyServer() { return lobbyServer; }
  public long getMaxStartWaitSeconds() { return maxStartWaitSeconds; }
  public long getCheckIntervalSeconds() { return checkIntervalSeconds; }
  public Map<String, String> getServerUuidMap() { return serverUuidMap; }
}
