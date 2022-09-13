package chat.app;

import chat.app.structs.*;
import chat.state.*;
import chat.state.structs.*;
import chat.util.*;
import java.time.*;
import java.util.*;

public class App {
  public static App shared = new App(State.shared);

  private static final String userNameAllowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890-_.";
  private static final String userPwAllowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890-=`!@#$%^&*()_+~,./<>?;':\"[]\\{}|";
  private static final long sessionDuration = 86400000;

  private final State state;
  private Map<String, NewMessageCallback> newMessageCallbacks;

  /*
   * Possible FailureReason values:
   * - Unknown
   * - Unauthorized
   */
  public Result<chat.app.structs.Session> logIn(Credentials credentials) {
    try {
      Result<chat.app.structs.Session> result = new Result<chat.app.structs.Session>();

      if (credentials == null || Utilities.nullOrEmpty(credentials.name) || Utilities.nullOrEmpty(credentials.pw)) {
        result.failureReason = Result.FailureReason.Unauthorized;
        return result;
      }

      chat.state.structs.User user = state.getUserByName(credentials.name);
      if (user == null) {
        result.failureReason = Result.FailureReason.Unauthorized;
        return result;
      }

      String hash = Utilities.generateHash(credentials.pw, user.salt);
      if (!hash.equals(user.hash)) {
        result.failureReason = Result.FailureReason.Unauthorized;
        return result;
      }

      chat.state.structs.Session session = new chat.state.structs.Session();
      session.id = Utilities.generateRandomString(32);
      chat.state.structs.Session duplicateSession = state.getSessionById(session.id);
      while (duplicateSession != null) {
        session.id = Utilities.generateRandomString(32);
        duplicateSession = state.getSessionById(session.id);
      }
      session.userId = user.id;
      session.expiration = System.currentTimeMillis() + sessionDuration;
      if (state.createSession(session)) {
        chat.app.structs.Session appSession = new chat.app.structs.Session();
        appSession.token = session.id;
        appSession.expiration = session.expiration;
        result.success = true;
        result.successValue = appSession;
        return result;
      }
      result.failureReason = Result.FailureReason.Unknown;
      return result;
    }

    catch (Exception error) {
      Result<chat.app.structs.Session> result = new Result<chat.app.structs.Session>();
      result.failureReason = Result.FailureReason.Unknown;
      return result;
    }
  }

  /*
   * Possible FailureReason values:
   * - Unknown
   * - Unauthorized
   */
  public Result<Object> logOut(String sessionToken) {
    try {
      Result<Object> result = new Result<Object>();

      if (Utilities.nullOrEmpty(sessionToken)) {
        result.failureReason = Result.FailureReason.Unauthorized;
        return result;
      }

      if (getUserBySessionToken(sessionToken) == null) {
        result.failureReason = Result.FailureReason.Unauthorized;
        return result;
      }

      result.success = state.deleteSessionById(sessionToken);
      if (!result.success) {
        result.failureReason = Result.FailureReason.Unknown;
      }
      return result;
    }

    catch (Exception error) {
      Result<Object> result = new Result<Object>();
      result.failureReason = Result.FailureReason.Unknown;
      return result;
    }
  }

  /*
   * Possible FailureReason values:
   * - Unknown
   * - Unauthorized
   */
  public Result<Object> verifySessionToken(String sessionToken) {
    try {
      Result<Object> result = new Result<Object>();

      if (Utilities.nullOrEmpty(sessionToken)) {
        result.failureReason = Result.FailureReason.Unauthorized;
        return result;
      }

      result.success = getUserBySessionToken(sessionToken) != null;
      if (!result.success) {
        result.failureReason = Result.FailureReason.Unauthorized;
      }
      return result;
    }

    catch (Exception error) {
      Result<Object> result = new Result<Object>();
      result.failureReason = Result.FailureReason.Unknown;
      return result;
    }
  }

  /*
   * Possible FailureReason values:
   * - Unknown
   * - Unauthorized
   */
  public Result<chat.app.structs.User> getUser(String sessionToken) {
    try {
      Result<chat.app.structs.User> result = new Result<chat.app.structs.User>();

      if (Utilities.nullOrEmpty(sessionToken)) {
        result.failureReason = Result.FailureReason.Unauthorized;
        return result;
      }

      chat.state.structs.User user = getUserBySessionToken(sessionToken);
      if (user == null) {
        result.failureReason = Result.FailureReason.Unauthorized;
        return result;
      }

      chat.app.structs.User appUser = new chat.app.structs.User();
      appUser.name = user.name;
      result.success = true;
      result.successValue = appUser;
      return result;
    }

    catch (Exception error) {
      Result<chat.app.structs.User> result = new Result<chat.app.structs.User>();
      result.failureReason = Result.FailureReason.Unknown;
      return result;
    }
  }

  /*
   * Possible FailureReason values:
   * - Unknown
   * - IllegalArgument
   * - Conflict
   */
  public Result<Object> createUser(Credentials credentials) {
    try {
      Result<Object> result = new Result<Object>();

      if (credentials == null || Utilities.nullOrEmpty(credentials.name) || Utilities.nullOrEmpty(credentials.pw)) {
        result.failureReason = Result.FailureReason.IllegalArgument;
        return result;
      }

      for (char c : credentials.name.toCharArray()) {
        if (userNameAllowedChars.indexOf(c) < 0) {
          result.failureReason = Result.FailureReason.IllegalArgument;
          return result;
        }
      }
      for (char c : credentials.pw.toCharArray()) {
        if (userPwAllowedChars.indexOf(c) < 0) {
          result.failureReason = Result.FailureReason.IllegalArgument;
          return result;
        }
      }

      if (state.getUserByName(credentials.name) != null) {
        result.failureReason = Result.FailureReason.Conflict;
        return result;
      }

      chat.state.structs.User user = new chat.state.structs.User();
      user.name = credentials.name;
      user.salt = Utilities.generateRandomString((short) 16);
      user.hash = Utilities.generateHash(credentials.pw, user.salt);
      result.success = state.createUser(user);
      if (!result.success) {
        result.failureReason = Result.FailureReason.Unknown;
      }
      return result;
    }

    catch (Exception error) {
      Result<Object> result = new Result<Object>();
      result.failureReason = Result.FailureReason.Unknown;
      return result;
    }
  }

  /*
   * Possible FailureReason values:
   * - Unknown
   * - Unauthorized
   * - IllegalArgument
   * - Conflict
   */
  public Result<Object> updateUser(String sessionToken, Credentials credentials) {
    try {
      Result<Object> result = new Result<Object>();

      if (Utilities.nullOrEmpty(sessionToken)) {
        result.failureReason = Result.FailureReason.Unauthorized;
        return result;
      }

      chat.state.structs.User user = getUserBySessionToken(sessionToken);
      if (user == null) {
        result.failureReason = Result.FailureReason.Unauthorized;
        return result;
      }

      if (credentials == null || (Utilities.nullOrEmpty(credentials.name) && Utilities.nullOrEmpty(credentials.pw))) {
        result.failureReason = Result.FailureReason.IllegalArgument;
        return result;
      }

      int userId = user.id;
      user = new chat.state.structs.User();
      user.id = userId;
      if (!Utilities.nullOrEmpty(credentials.name)) {
        if (state.getUserByName(credentials.name) != null) {
          result.failureReason = Result.FailureReason.Conflict;
          return result;
        }
        user.name = credentials.name;
      }
      if (!Utilities.nullOrEmpty(credentials.pw)) {
        user.salt = Utilities.generateRandomString((short) 16);
        user.hash = Utilities.generateHash(credentials.pw, user.salt);
      }
      result.success = state.updateUser(user);
      if (!result.success) {
        result.failureReason = Result.FailureReason.Unknown;
      }
      return result;
    }

    catch (Exception error) {
      Result<Object> result = new Result<Object>();
      result.failureReason = Result.FailureReason.Unknown;
      return result;
    }
  }

  /*
   * Possible FailureReason values:
   * - Unknown
   * - Unauthorized
   */
  public Result<Object> deleteUser(String sessionToken) {
    try {
      Result<Object> result = new Result<Object>();

      if (Utilities.nullOrEmpty(sessionToken)) {
        result.failureReason = Result.FailureReason.Unauthorized;
        return result;
      }

      chat.state.structs.User user = getUserBySessionToken(sessionToken);
      if (user == null) {
        result.failureReason = Result.FailureReason.Unauthorized;
        return result;
      }

      state.deleteSessionsByUserId(user.id);
      result.success = state.deleteUserById(user.id);
      if (!result.success) {
        result.failureReason = Result.FailureReason.Unknown;
      }
      return result;
    }

    catch (Exception error) {
      Result<Object> result = new Result<Object>();
      result.failureReason = Result.FailureReason.Unknown;
      return result;
    }
  }

  /*
   * Possible FailureReason values:
   * - Unknown
   * - Unauthorized
   * - IllegalArgument
   * - NotFound
   */
  public Result<chat.app.structs.Message[]> getMessagesMostRecent(String sessionToken, int limit) {
    try {
      Result<chat.app.structs.Message[]> result = new Result<chat.app.structs.Message[]>();

      if (Utilities.nullOrEmpty(sessionToken)) {
        result.failureReason = Result.FailureReason.Unauthorized;
        return result;
      }

      chat.state.structs.User user = getUserBySessionToken(sessionToken);
      if (user == null) {
        result.failureReason = Result.FailureReason.Unauthorized;
        return result;
      }

      if (limit < 0) {
        result.failureReason = Result.FailureReason.IllegalArgument;
        return result;
      }

      chat.state.structs.Message[] stateMessages = state.getMessagesUntilTimestamp(Instant.now().toEpochMilli(), limit);
      if (stateMessages == null || stateMessages.length == 0) {
        result.failureReason = Result.FailureReason.NotFound;
        return result;
      }

      Map<Integer, String> userNameCache = new HashMap<Integer, String>();
      chat.app.structs.Message[] appMessages = new chat.app.structs.Message[stateMessages.length];
      for (int i = 0; i < stateMessages.length; i++) {
        appMessages[i] = new chat.app.structs.Message();
        appMessages[i].id = stateMessages[i].id;
        appMessages[i].outgoing = stateMessages[i].userId == user.id;
        if (!appMessages[i].outgoing) {
          if (!userNameCache.containsKey(stateMessages[i].userId)) {
            chat.state.structs.User messageUser = state.getUserById(stateMessages[i].userId);
            if (messageUser != null) {
              userNameCache.put(stateMessages[i].userId, messageUser.name);
            } else {
              userNameCache.put(stateMessages[i].userId, "[Deleted user]");
            }
          }
          appMessages[i].userName = userNameCache.get(stateMessages[i].userId);
        }
        appMessages[i].timestamp = stateMessages[i].timestamp;
        appMessages[i].content = stateMessages[i].content;
      }
      result.success = true;
      result.successValue = appMessages;
      return result;
    }

    catch (Exception error) {
      Result<chat.app.structs.Message[]> result = new Result<chat.app.structs.Message[]>();
      result.failureReason = Result.FailureReason.Unknown;
      return result;
    }
  }

  /*
   * Possible FailureReason values:
   * - Unknown
   * - Unauthorized
   * - IllegalArgument
   * - NotFound
   */
  public Result<chat.app.structs.Message[]> getMessagesBeforeMessage(String sessionToken, int messageId, int limit) {
    try {
      Result<chat.app.structs.Message[]> result = new Result<chat.app.structs.Message[]>();

      if (Utilities.nullOrEmpty(sessionToken)) {
        result.failureReason = Result.FailureReason.Unauthorized;
        return result;
      }

      chat.state.structs.User user = getUserBySessionToken(sessionToken);
      if (user == null) {
        result.failureReason = Result.FailureReason.Unauthorized;
        return result;
      }

      if (messageId < 1 || limit < 0) {
        result.failureReason = Result.FailureReason.IllegalArgument;
        return result;
      }

      chat.state.structs.Message targetMessage = state.getMessageById(messageId);
      if (targetMessage == null) {
        result.failureReason = Result.FailureReason.NotFound;
        return result;
      }

      chat.state.structs.Message[] stateMessages = state.getMessagesUntilTimestamp(targetMessage.timestamp, limit + 1);
      if (stateMessages == null || stateMessages.length == 0) {
        result.failureReason = Result.FailureReason.NotFound;
        return result;
      }

      int duplicateIndex = stateMessages.length - 1;
      for (; duplicateIndex >= 0; duplicateIndex--) {
        if (stateMessages[duplicateIndex].id == targetMessage.id) {
          break;
        }
      }
      chat.state.structs.Message[] scratch = new chat.state.structs.Message[stateMessages.length - 1];
      for (int i = 0; i < duplicateIndex; i++) {
        scratch[i] = stateMessages[i];
      }
      for (int i = duplicateIndex >= 0 ? duplicateIndex : 0; i < scratch.length; i++) {
        scratch[i] = stateMessages[i + 1];
      }
      stateMessages = scratch;

      Map<Integer, String> userNameCache = new HashMap<Integer, String>();
      chat.app.structs.Message[] appMessages = new chat.app.structs.Message[stateMessages.length];
      for (int i = 0; i < stateMessages.length; i++) {
        appMessages[i] = new chat.app.structs.Message();
        appMessages[i].id = stateMessages[i].id;
        appMessages[i].outgoing = stateMessages[i].userId == user.id;
        if (!appMessages[i].outgoing) {
          if (!userNameCache.containsKey(stateMessages[i].userId)) {
            chat.state.structs.User messageUser = state.getUserById(stateMessages[i].userId);
            if (messageUser != null) {
              userNameCache.put(stateMessages[i].userId, messageUser.name);
            } else {
              userNameCache.put(stateMessages[i].userId, "[Deleted user]");
            }
          }
          appMessages[i].userName = userNameCache.get(stateMessages[i].userId);
        }
        appMessages[i].timestamp = stateMessages[i].timestamp;
        appMessages[i].content = stateMessages[i].content;
      }
      result.success = true;
      result.successValue = appMessages;
      return result;
    }

    catch (Exception error) {
      Result<chat.app.structs.Message[]> result = new Result<chat.app.structs.Message[]>();
      result.failureReason = Result.FailureReason.Unknown;
      return result;
    }
  }

  /*
   * Possible FailureReason values:
   * - Unknown
   * - Unauthorized
   * - IllegalArgument
   */
  public Result<Object> createMessage(String sessionToken, chat.app.structs.Message message) {
    try {
      Result<Object> result = new Result<Object>();

      if (Utilities.nullOrEmpty(sessionToken)) {
        result.failureReason = Result.FailureReason.Unauthorized;
        return result;
      }

      chat.state.structs.User user = getUserBySessionToken(sessionToken);
      if (user == null) {
        result.failureReason = Result.FailureReason.Unauthorized;
        return result;
      }

      if (message == null || Utilities.nullOrEmpty(message.content)) {
        result.failureReason = Result.FailureReason.IllegalArgument;
        return result;
      }

      message.timestamp = Instant.now().toEpochMilli();

      chat.state.structs.Message stateMessage = new chat.state.structs.Message();
      stateMessage.userId = user.id;
      stateMessage.timestamp = message.timestamp;
      stateMessage.content = message.content;
      result.success = state.createMessage(stateMessage);
      if (!result.success) {
        result.failureReason = Result.FailureReason.Unknown;
        return result;
      }

      for (Map.Entry<String, NewMessageCallback> entry : newMessageCallbacks.entrySet()) {
        chat.state.structs.User recipientUser = null;
        try {
          recipientUser = getUserBySessionToken(entry.getKey());
        } catch (Exception error) {
          continue;
        }
        if (recipientUser == null) {
          continue;
        }
        boolean sameUser = recipientUser.id == user.id;
        chat.app.structs.Message tailoredMessage = new chat.app.structs.Message();
        tailoredMessage.id = -1; // TODO: Generate String ID and pass into State method
        tailoredMessage.outgoing = sameUser;
        tailoredMessage.userName = sameUser ? null : user.name;
        tailoredMessage.timestamp = message.timestamp;
        tailoredMessage.content = message.content;
        entry.getValue().call(tailoredMessage);
      }

      return result;
    }

    catch (Exception error) {
      Result<Object> result = new Result<Object>();
      result.failureReason = Result.FailureReason.Unknown;
      return result;
    }
  }

  public void registerCallbackForNewMessage(String sessionToken, NewMessageCallback callback) {
    newMessageCallbacks.put(sessionToken, callback);
  }

  public void unregisterCallbackForNewMessage(String sessionToken) {
    newMessageCallbacks.remove(sessionToken);
  }

  private App(State state) {
    this.state = state;
    this.newMessageCallbacks = new HashMap<String, NewMessageCallback>();
  }

  private chat.state.structs.User getUserBySessionToken(String sessionToken)
  throws Exception {
    chat.state.structs.Session session = state.getSessionById(sessionToken);
    if (session == null) {
      return null;
    }

    chat.state.structs.User user = state.getUserById(session.userId);
    if (user == null) {
      state.deleteSessionById(session.id);
    }
    return user;
  }

  public static class Result<T> {
    public boolean success;
    public T successValue;
    public FailureReason failureReason;

    public static enum FailureReason {
      Unknown, IllegalArgument, Unauthorized, Conflict, NotFound
    }
  }

  public static interface NewMessageCallback {
    public void call(chat.app.structs.Message message);
  }
}