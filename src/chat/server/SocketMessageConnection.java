package chat.server;

import chat.app.*;
import chat.app.structs.*;
import chat.util.*;
import com.google.gson.*;
import jakarta.websocket.*;
import jakarta.websocket.server.*;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

@ServerEndpoint(value = "/socket/message",
configurator = SocketMessageConfigurator.class)
public class SocketMessageConnection {
  private static Set<SocketMessageConnection> connections
  = new CopyOnWriteArraySet<>();

  private jakarta.websocket.Session wsSession;
  private String sessionToken;
  private String userName;

  @OnOpen
  public void onOpen(jakarta.websocket.Session wsSession) {
    try {
      this.wsSession = wsSession;
      connections.add(this);
      sessionToken = (String) this.wsSession.getUserProperties()
      .get("sessionToken");
      App.Result<User> userResult = App.shared.getUser(sessionToken);
      if (!userResult.success) {
        close();
        return;
      }
      userName = userResult.successValue.name;

      App.Result<Message[]> messagesResult = App.shared.getMessages(sessionToken);
      if (!messagesResult.success) {
        close();
        return;
      }
      for (Message message : messagesResult.successValue) {
        Gson gson = new Gson();
        MessageToClient messageToClient = new MessageToClient();
        messageToClient.outgoing = message.outgoing;
        messageToClient.userName = message.userName;
        messageToClient.timestamp = Instant.ofEpochMilli(message.timestamp)
        .toString();
        messageToClient.content = message.content;
        String outgoingMessageJson = gson.toJson(messageToClient);
        try {
          synchronized (this) {
            this.wsSession.getBasicRemote().sendText(outgoingMessageJson);
          }
        } catch (IOException error) {
          close();
          return;
        }
      }
    }

    catch (Exception error) {
      close();
    }
  }

  @OnClose
  public void onClose() {
    close();
  }

  @OnMessage
  public void onMessage(String incomingMessageJson) {
    try {
      if (!App.shared.verifySessionToken(sessionToken).success) {
        close();
        return;
      }

      Gson gson = new Gson();
      MessageToServer incomingMessage = null;
      try {
        incomingMessage = gson.fromJson(incomingMessageJson,
        MessageToServer.class);
        if (incomingMessage == null
        || Utilities.nullOrEmpty(incomingMessage.content)) {
          return;
        }
      } catch (JsonSyntaxException error) {
        return;
      }

      Instant currInstant = Instant.now();
      long currMilli = currInstant.toEpochMilli();
      String timestamp = currInstant.toString();

      Message message = new Message();
      message.timestamp = currMilli;
      message.content = incomingMessage.content;
      if(!App.shared.createMessage(sessionToken, message).success) {
        return;
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
        if (connection.userName.equals(userName)) {
          outgoingMessageJson = outgoingMessageOutJson;
        } else {
          outgoingMessageJson = outgoingMessageInJson;
        }
        try {
          synchronized (connection) {
            connection.wsSession.getBasicRemote().sendText(outgoingMessageJson);
          }
        } catch (IOException error) {
          connection.close();
        }
      }
    }

    catch (Exception error) {
      close();
      return;
    }
  }

  @OnError
  public void onError(Throwable error) {
    close();
  }

  private void close() {
    connections.remove(this);
    try {
      wsSession.close();
    } catch (IOException error) { }
  }

  private static class MessageToServer {
    public String content;
  }

  private static class MessageToClient {
    public boolean outgoing;
    public String userName;
    public String timestamp;
    public String content;
  }
}