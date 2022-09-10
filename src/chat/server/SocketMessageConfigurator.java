package chat.server;

import chat.app.*;
import jakarta.websocket.*;
import jakarta.websocket.server.*;
import java.util.*;

public class SocketMessageConfigurator
extends ServerEndpointConfig.Configurator {
  @Override
  public void modifyHandshake(ServerEndpointConfig config,
  HandshakeRequest request, HandshakeResponse response) {
    super.modifyHandshake(config, request, response);

    try {
      String sessionToken = null;
      List<String> cookies = request.getHeaders().get("cookie");
      if (cookies != null) {
        for (String cookie : cookies) {
          if (cookie.startsWith("session") && cookie.length() > 8) {
            sessionToken = cookie.substring(8);
          }
        }
      }
      App.Result<Object> result = App.shared.verifySessionToken(sessionToken);
      if (!result.success) {
        dropConnection(response);
        return;
      }
      config.getUserProperties().put("sessionToken", sessionToken);
    }

    catch (Exception error) {
      dropConnection(response);
    }
  }

  private void dropConnection(HandshakeResponse response) {
    response.getHeaders()
    .put(HandshakeResponse.SEC_WEBSOCKET_ACCEPT, new ArrayList<String>());
  }
}