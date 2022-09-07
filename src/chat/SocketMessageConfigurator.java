package chat;

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
      List<String> cookies = request.getHeaders().get("cookie");
      String sessionId = null;
      for (String cookie : cookies) {
        if (cookie.startsWith("session") && cookie.length() > 8) {
          sessionId = cookie.substring(8);
        }
      }
      if (Utilities.nullOrEmpty(sessionId)) {
        dropConnection(response);
        return;
      }
      chat.Session session = PersistentModel.shared.getSessionById(sessionId);
      if (session == null) {
        dropConnection(response);
        return;
      }
      User user = PersistentModel.shared.getUserById(session.user);
      if (user == null) {
        dropConnection(response);
        return;
      }
      config.getUserProperties().put("userId", user.id);
      config.getUserProperties().put("sessionId", session.id);
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