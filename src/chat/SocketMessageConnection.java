package chat;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.sql.*;
import java.time.*;
import jakarta.websocket.*;
import jakarta.websocket.server.*;
import com.google.gson.*;

@ServerEndpoint(
  value = "/socket/message",
  configurator = SocketMessageConfigurator.class)
public class SocketMessageConnection {
  private static Set<SocketMessageConnection> connections
  = new CopyOnWriteArraySet<>();

  private Session session;
  private int userId;
  private String userName;

  @OnOpen
  public void onOpen(Session session) {
    this.session = session;
    connections.add(this);
    String sessionId = (String) this.session.getUserProperties().get("session");
    try {
      Class.forName("org.mariadb.jdbc.Driver");
    } catch (ClassNotFoundException error) {
      close();
      return;
    }
    Connection connection = null;
    PreparedStatement statement = null;
    ResultSet results = null;
    try {
      connection = DriverManager.getConnection
      ("jdbc:mariadb://localhost/chat", "root", "");
      String queryString = "SELECT user FROM sessions WHERE id = ?;";
      statement = connection.prepareStatement(queryString);
      statement.setString(1, sessionId);
      results = statement.executeQuery();
      if (!results.next()) {
        close();
        return;
      }
      userId = results.getInt(1);
      DbHelper.close(statement, results);
      queryString = "SELECT name FROM users WHERE id = ?;";
      statement = connection.prepareStatement(queryString);
      statement.setInt(1, userId);
      results = statement.executeQuery();
      if (!results.next()) {
        close();
        return;
      }
      userName = results.getString(1);
    } catch (SQLException error) {
      close();
    } finally {
      DbHelper.close(statement, results);
      DbHelper.close(connection);
    }
    // TODO: Send messages stored in database
  }

  @OnClose
  public void onClose() {
    close();
  }

  @OnMessage
  public void onMessage(String incomingMessageJson) {
    String sessionId = (String) this.session.getUserProperties().get("session");
    try {
      Class.forName("org.mariadb.jdbc.Driver");
    } catch (ClassNotFoundException error) {
      close();
      return;
    }
    Connection dbConnection = null;
    PreparedStatement statement = null;
    ResultSet results = null;
    try {
      dbConnection = DriverManager.getConnection
      ("jdbc:mariadb://localhost/chat", "root", "");
      String queryString = "SELECT * FROM sessions WHERE id = ?;";
      statement = dbConnection.prepareStatement(queryString);
      statement.setString(1, sessionId);
      results = statement.executeQuery();
      if (!results.next()) {
        close();
        return;
      }
    } catch (SQLException error) {
      close();
      return;
    } finally {
      DbHelper.close(statement, results);
      DbHelper.close(dbConnection);
    }

    Gson gson = new Gson();
    MessageToServer incomingMessage = null;
    try {
      incomingMessage
      = gson.fromJson(incomingMessageJson, MessageToServer.class);
      if (incomingMessage == null || incomingMessage.content == null) {
        return;
      }
    } catch (Exception error) {
      return;
    }

    Instant currInstant = Instant.now();
    long currMilli = currInstant.toEpochMilli();
    String timestamp = currInstant.toString();

    try {
      dbConnection = DriverManager.getConnection
      ("jdbc:mariadb://localhost/chat", "root", "");
      String queryString
      = "INSERT INTO messages (user, timestamp, content) VALUES (?, ?, ?);";
      statement = dbConnection.prepareStatement(queryString);
      statement.setInt(1, userId);
      statement.setLong(2, currMilli);
      statement.setString(3, incomingMessage.content);
      int affectedCount = statement.executeUpdate();
      if (affectedCount != 1) {
        return;
      }
    } catch (SQLException error) {
      return;
    } finally {
      DbHelper.close(statement, null);
      DbHelper.close(dbConnection);
    }

    MessageToClient outgoingMessageIn = new MessageToClient();
    outgoingMessageIn.userName = userName;
    outgoingMessageIn.timestamp = timestamp;
    outgoingMessageIn.content = incomingMessage.content;
    String outgoingMessageInJson = gson.toJson(outgoingMessageIn);
    MessageToClient outgoingMessageOut = new MessageToClient();
    outgoingMessageOut.outgoing = true;
    outgoingMessageOut.timestamp = timestamp;
    outgoingMessageOut.content = incomingMessage.content;
    String outgoingMessageOutJson = gson.toJson(outgoingMessageOut);

    for (SocketMessageConnection connection : connections) {
      String outgoingMessageJson = null;
      if (connection.userId == userId) {
        outgoingMessageJson = outgoingMessageOutJson;
      } else {
        outgoingMessageJson = outgoingMessageInJson;
      }
      try {
        synchronized (connection) {
          connection.session.getBasicRemote().sendText(outgoingMessageJson);
        }
      } catch (IOException error) {
        connection.close();
      }
    }
  }

  @OnError
  public void onError(Throwable error) {
    close();
  }

  private void close() {
    connections.remove(this);
    try {
      session.close();
    } catch (IOException error) { }
  }
}