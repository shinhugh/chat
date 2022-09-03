package chat;

import java.util.*;
import jakarta.websocket.*;
import jakarta.websocket.server.*;

public class SocketMessageConfigurator
extends ServerEndpointConfig.Configurator {
  @Override
  public void modifyHandshake(ServerEndpointConfig config,
  HandshakeRequest request, HandshakeResponse response) {
    List<String> cookies = request.getHeaders().get("cookie");
    // TODO: Parse cookies for session token
    String session = "MOCKSESSION";
    // TODO: Reject connection if session is missing or invalid
    config.getUserProperties().put("session", session);
  }
}