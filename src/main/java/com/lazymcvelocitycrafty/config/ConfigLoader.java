package com.lazymcvelocitycrafty.config;

import com.google.gson.Gson;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigLoader {
  private static final Gson gson = new Gson();

  public static PluginConfig loadConfig(Path dataDirectory, Logger logger) {
    Path configPath = dataDirectory.resolve("config.toml");

    try {
      if (!Files.exists(configPath)) {
        logger.error("config.toml not found in {}", dataDirectory);
        throw new RuntimeException("Missing config.toml");
      }
      String content = Files.readString(configPath);
      return gson.fromJson(content, PluginConfig.class);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load config.toml", e);
    }
  }
}
