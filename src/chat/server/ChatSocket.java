package chat.server;

import chat.app.*;
import chat.app.structs.*;
import chat.util.*;
import com.google.gson.*;
import jakarta.websocket.*;
import jakarta.websocket.server.*;
import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;

@ServerEndpoint(value = "/api/messages", configurator = ChatSocket.Configurator.class)
public class ChatSocket
implements App.NewMessageCallback {
  private static Set<ChatSocket> connections = new CopyOnWriteArraySet<>();

  private jakarta.websocket.Session wsSession;
  private String sessionToken;

  @OnOpen
  public void onOpen(jakarta.websocket.Session wsSession) {
    this.wsSession = wsSession;
    connections.add(this);
    sessionToken = (String) this.wsSession.getUserProperties().get("sessionToken");

    App.shared.registerCallbackForNewMessage(sessionToken, this);
  }

  @OnClose
  public void onClose() {
    close();
  }

  @OnMessage
  public void onMessage(String json) {
    MessageToServer messageToServer = null;
    Gson gson = new Gson();
    try {
      messageToServer = gson.fromJson(json, MessageToServer.class);
    } catch (JsonSyntaxException error) { }
    if (messageToServer == null) {
      return;
    }

    handleFetchMessages(messageToServer.fetchMessagesData);
    handleSendMessage(messageToServer.sendMessageData);
  }

  @OnError
  public void onError(Throwable error) {
    close();
  }

  public void call(Message message) {
    Message[] wrapperArray = {message};
    sendMessagesToClient(wrapperArray);
  }

  private void close() {
    connections.remove(this);
    App.shared.unregisterCallbackForNewMessage(sessionToken);
    try {
      wsSession.close();
    } catch (IOException error) { }
  }

  private void handleFetchMessages(MessageToServer.FetchMessagesData fetchMessagesData) {
    if (!wsSession.isOpen()) {
      return;
    }

    if (fetchMessagesData == null) {
      return;
    }

    App.Result<Message[]> result = null;
    if (fetchMessagesData.messageId < 1) {
      result = App.shared.getMessagesMostRecent(sessionToken, fetchMessagesData.limit);
    } else {
      result = App.shared.getMessagesBeforeMessage(sessionToken, fetchMessagesData.messageId, fetchMessagesData.limit);
    }
    if (!result.success) {
      switch (result.failureReason) {
        case IllegalArgument:
        case NotFound:
          return;
        default:
          close();
          return;
      }
    }

    sendMessagesToClient(result.successValue);
  }

  private void handleSendMessage(MessageToServer.SendMessageData sendMessageData) {
    if (!wsSession.isOpen()) {
      return;
    }

    if (sendMessageData == null) {
      return;
    }

    Message message = new Message();
    message.content = sendMessageData.content;

    App.Result<Object> createMessageResult = App.shared.createMessage(sessionToken, message);
    if (!createMessageResult.success) {
      switch (createMessageResult.failureReason) {
        case IllegalArgument:
          return;
        default:
          close();
          return;
      }
    }
  }

  private void sendMessagesToClient(Message[] messages) {
    MessageToClient messageToClient = new MessageToClient();
    messageToClient.messagesData = new MessageToClient.MessagesData();
    messageToClient.messagesData.messages = new MessageToClient.MessagesData.Message[messages.length];
    for (int i = 0; i < messages.length; i++) {
      messageToClient.messagesData.messages[i] = new MessageToClient.MessagesData.Message();
      messageToClient.messagesData.messages[i].id = messages[i].id;
      messageToClient.messagesData.messages[i].outgoing = messages[i].outgoing;
      messageToClient.messagesData.messages[i].userName = messages[i].userName;
      messageToClient.messagesData.messages[i].timestamp = Instant.ofEpochMilli(messages[i].timestamp).toString();
      messageToClient.messagesData.messages[i].content = messages[i].content;
    }
    Gson gson = new Gson();
    String json = gson.toJson(messageToClient);
    try {
      synchronized (this) {
        this.wsSession.getBasicRemote().sendText(json);
      }
    } catch (IOException error) {
      close();
    }
  }

  public static class Configurator
  extends ServerEndpointConfig.Configurator {
    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
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
      response.getHeaders().put(HandshakeResponse.SEC_WEBSOCKET_ACCEPT, new ArrayList<String>());
    }
  }

  private static class MessageToServer {
    public FetchMessagesData fetchMessagesData;
    public SendMessageData sendMessageData;

    public static class FetchMessagesData {
      public int messageId;
      public int limit;
    }

    public static class SendMessageData {
      public String content;
    }
  }

  private static class MessageToClient {
    public MessagesData messagesData;

    public static class MessagesData {
      public Message[] messages;

      public static class Message {
        public int id;
        public boolean outgoing;
        public String userName;
        public String timestamp;
        public String content;
      }
    }
  }
}