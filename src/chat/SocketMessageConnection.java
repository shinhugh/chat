package chat;

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
  private chat.Session session;
  private User user;

  @OnOpen
  public void onOpen(jakarta.websocket.Session wsSession) {
    try {
      this.wsSession = wsSession;
      connections.add(this);
      String sessionId = (String) this.wsSession.getUserProperties()
      .get("sessionId");
      session = PersistentModel.shared.getSessionById(sessionId);
      int userId = (int) this.wsSession.getUserProperties().get("userId");
      user = PersistentModel.shared.getUserById(userId);

      HashMap<Integer, String> userNameCache = new HashMap<Integer, String>();
      Message[] messages = PersistentModel.shared.getMessages();
      for (Message message : messages) {
        Gson gson = new Gson();
        String timestamp = Instant.ofEpochMilli(message.timestamp).toString();
        String outgoingMessageJson = null;
        if (message.user == user.id) {
          MessageToClient outgoingMessageOut = new MessageToClient();
          outgoingMessageOut.outgoing = true;
          outgoingMessageOut.timestamp = timestamp;
          outgoingMessageOut.content = message.content;
          outgoingMessageJson = gson.toJson(outgoingMessageOut);
        } else {
          MessageToClient outgoingMessageIn = new MessageToClient();
          if (userNameCache.containsKey(message.user)) {
            outgoingMessageIn.userName = userNameCache.get(message.user);
          } else {
            User messageUser = PersistentModel.shared.getUserById(message.user);
            userNameCache.put(message.user, messageUser.name);
            outgoingMessageIn.userName = messageUser.name;
          }
          outgoingMessageIn.timestamp = timestamp;
          outgoingMessageIn.content = message.content;
          outgoingMessageJson = gson.toJson(outgoingMessageIn);
        }
        try {
          synchronized (this) {
            this.wsSession.getBasicRemote().sendText(outgoingMessageJson);
          }
        } catch (IOException error) {
          close();
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
      session = PersistentModel.shared.getSessionById(session.id);
      if (session == null) {
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
      message.user = user.id;
      message.timestamp = currMilli;
      message.content = incomingMessage.content;
      if(!PersistentModel.shared.createMessage(message)) {
        return;
      }

      MessageToClient outgoingMessageIn = new MessageToClient();
      outgoingMessageIn.userName = user.name;
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
        if (connection.user.id == user.id) {
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
}