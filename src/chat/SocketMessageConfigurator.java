package chat;

import java.util.*;
import java.sql.*;
import jakarta.websocket.*;
import jakarta.websocket.server.*;

public class SocketMessageConfigurator
extends ServerEndpointConfig.Configurator {
  @Override
  public void modifyHandshake(ServerEndpointConfig config,
  HandshakeRequest request, HandshakeResponse response) {
    super.modifyHandshake(config, request, response);
    List<String> cookies = request.getHeaders().get("cookie");
    String session = null;
    for (String cookie : cookies) {
      if (cookie.startsWith("session") && cookie.length() > 8) {
        session = cookie.substring(8);
      }
    }
    if (session == null) {
      response.getHeaders()
      .put(HandshakeResponse.SEC_WEBSOCKET_ACCEPT, new ArrayList<String>());
      return;
    }

    Connection connection = null;
    PreparedStatement statement = null;
    ResultSet results = null;
    try {
      Class.forName("org.mariadb.jdbc.Driver");
      connection = DriverManager.getConnection
      ("jdbc:mariadb://localhost/chat", "root", "");
      String queryString = "SELECT * FROM sessions WHERE id = ?;";
      statement = connection.prepareStatement(queryString);
      statement.setString(1, session);
      results = statement.executeQuery();
      if (!results.next()) {
        response.getHeaders()
        .put(HandshakeResponse.SEC_WEBSOCKET_ACCEPT, new ArrayList<String>());
        return;
      }
    } catch (Exception error) {
      response.getHeaders()
      .put(HandshakeResponse.SEC_WEBSOCKET_ACCEPT, new ArrayList<String>());
      return;
    } finally {
      DbHelper.close(statement, results);
      DbHelper.close(connection);
    }

    config.getUserProperties().put("session", session);
  }
}