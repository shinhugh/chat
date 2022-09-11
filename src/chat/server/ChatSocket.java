package chat.server;

import chat.app.*;
import chat.app.structs.*;
import com.google.gson.*;
import jakarta.websocket.*;
import jakarta.websocket.server.*;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

@ServerEndpoint(value = "/chat", configurator = ChatSocket.Configurator.class)
public class ChatSocket {
  private static Set<ChatSocket> connections = new CopyOnWriteArraySet<>();

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

      App.Result<Message[]> messagesResult = App.shared
      .getMessages(sessionToken);
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

      Message message = new Message();

      Instant currInstant = Instant.now();
      message.timestamp = currInstant.toEpochMilli();

      Gson gson = new Gson();
      try {
        MessageToServer incomingMessage = gson.fromJson(incomingMessageJson,
        MessageToServer.class);
        if (incomingMessage != null) {
          message.content = incomingMessage.content;
        }
      } catch (JsonSyntaxException error) { }

      if(!App.shared.createMessage(sessionToken, message).success) {
        return;
      }

      String timestamp = currInstant.toString();
      MessageToClient outgoingMessageIn = new MessageToClient();
      outgoingMessageIn.userName = userName;
      outgoingMessageIn.timestamp = timestamp;
      outgoingMessageIn.content = message.content;
      String outgoingMessageInJson = gson.toJson(outgoingMessageIn);
      MessageToClient outgoingMessageOut = new MessageToClient();
      outgoingMessageOut.outgoing = true;
      outgoingMessageOut.timestamp = timestamp;
      outgoingMessageOut.content = message.content;
      String outgoingMessageOutJson = gson.toJson(outgoingMessageOut);

      for (ChatSocket connection : connections) {
        if (!App.shared.verifySessionToken(connection.sessionToken).success) {
          connection.close();
          continue;
        }

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

  public static class Configurator
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