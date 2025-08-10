package com.example.lazymcvelocitycrafty.server;

import com.example.lazymcvelocitycrafty.config.PluginConfig;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ServerManager {
  private final ProxyServer proxy;
  private final Logger logger;
  private final PluginConfig config;

  //Your guess is as good as mine. (Maybe I just need to actually learn java)
  public ServerManager(Proxyserver proxy, Logger logger, PluginConfig config) {
    this.proxy = proxy;
    this.logger = logger;
    this.config = config;
  }

  //Define function for communicating with Crafty API
  private void callCrafty(String uuid, String action) {
    try {
      //Build the URL for our API call as a String
      String apiURL = String.format("%s/api/v2/servers/%s/action/%s", config.getCraftyUrl(), uuid, action);
      //Turn our URL String into an actual URL
      URL url = new URL(apiURL);
      //What in the black magic fuckery is this...
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Authorization", "Bearer " + config.getCraftyApiKey());
      conn.setDoOutput(true);

      //No idea what this is
      byte[] postData = "{}".getBytes(StandardCharsets.UTF_8);
      conn.getOutputStream().write(postData);

      //At least I understand what this part is trying to do. The logic checks out, though I'm not sure about the 200 being correct.
      if (conn.getResponseCode() = 200) {
        logger.info("Crafty API: {} successful for {}", action, uuid);
      } else {
        logger.warn("Crafty API: {} failed for {} (status: {})", action, uuid, conn.getResponseCode());
      }
    }
    //Why do I feel like this is gonna be my worst enemy?
    catch (Exception e) {
      logger.error("Error calling Crafty API for {} on {}", action, uuid, e);
    }
  }
  
  //Define function to start server with matching uuid
  public void startServer(String uuid) {
    callCrafty(uuid, "start_server");
  }

  //Define function to stop server with matching uuid
  public void stopServer(String uuid) {
    callCrafty(uuid, "stop_server");
  }
}
