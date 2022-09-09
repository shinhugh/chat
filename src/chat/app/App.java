package chat.app;

import chat.*;
import chat.app.structs.*;
import chat.state.*;
import chat.state.structs.*;
import java.util.*;

public class App {
  public static App shared = new App(State.shared);

  private State state;

  /*
   * Possible FailureReason values:
   * - Unknown
   * - IllegalArgument
   * - Unauthorized
   */
  public Result<chat.app.structs.Session> logIn(Credentials credentials) {
    try {
      Result<chat.app.structs.Session> result
      = new Result<chat.app.structs.Session>();

      if (credentials == null || Utilities.nullOrEmpty(credentials.name)
      || Utilities.nullOrEmpty(credentials.pw)) {
        result.failureReason = Result.FailureReason.IllegalArgument;
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
      chat.state.structs.Session duplicateSession = state
      .getSessionById(session.id);
      while (duplicateSession != null) {
        session.id = Utilities.generateRandomString(32);
        duplicateSession = state.getSessionById(session.id);
      }
      session.userId = user.id;
      session.expiration = System.currentTimeMillis() + 86400000;
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
      Result<chat.app.structs.Session> result
      = new Result<chat.app.structs.Session>();
      result.failureReason = Result.FailureReason.Unknown;
      return result;
    }
  }

  /*
   * Possible FailureReason values:
   * - Unknown
   * - IllegalArgument
   * - Unauthorized
   */
  public Result<Object> logOut(String sessionToken) {
    try {
      Result<Object> result = new Result<Object>();

      if (Utilities.nullOrEmpty(sessionToken)) {
        result.failureReason = Result.FailureReason.IllegalArgument;
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
   * - IllegalArgument
   * - Unauthorized
   */
  public Result<Object> verifySessionToken(String sessionToken) {
    try {
      Result<Object> result = new Result<Object>();

      if (Utilities.nullOrEmpty(sessionToken)) {
        result.failureReason = Result.FailureReason.IllegalArgument;
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
   * - IllegalArgument
   * - Unauthorized
   */
  public Result<chat.app.structs.User> getUser(String sessionToken) {
    try {
      Result<chat.app.structs.User> result
      = new Result<chat.app.structs.User>();

      if (Utilities.nullOrEmpty(sessionToken)) {
        result.failureReason = Result.FailureReason.IllegalArgument;
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
      Result<chat.app.structs.User> result
      = new Result<chat.app.structs.User>();
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

      if (credentials == null || Utilities.nullOrEmpty(credentials.name)
      || Utilities.nullOrEmpty(credentials.pw)) {
        result.failureReason = Result.FailureReason.IllegalArgument;
        return result;
      }

      // TODO: Check credentials for illegal characters

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
   * - IllegalArgument
   * - Unauthorized
   * - Conflict
   */
  public Result<Object> updateUser(String sessionToken,
  Credentials credentials) {
    try {
      Result<Object> result = new Result<Object>();

      if (Utilities.nullOrEmpty(sessionToken) || credentials == null
      || (Utilities.nullOrEmpty(credentials.name)
      && Utilities.nullOrEmpty(credentials.pw))) {
        result.failureReason = Result.FailureReason.IllegalArgument;
        return result;
      }

      chat.state.structs.User user = getUserBySessionToken(sessionToken);
      if (user == null) {
        result.failureReason = Result.FailureReason.Unauthorized;
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
   * - IllegalArgument
   * - Unauthorized
   */
  public Result<Object> deleteUser(String sessionToken) {
    try {
      Result<Object> result = new Result<Object>();

      if (Utilities.nullOrEmpty(sessionToken)) {
        result.failureReason = Result.FailureReason.IllegalArgument;
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
   * - IllegalArgument
   * - Unauthorized
   */
  public Result<chat.app.structs.Message[]> getMessages(String sessionToken) {
    try {
      Result<chat.app.structs.Message[]> result
      = new Result<chat.app.structs.Message[]>();

      if (Utilities.nullOrEmpty(sessionToken)) {
        result.failureReason = Result.FailureReason.IllegalArgument;
        return result;
      }

      chat.state.structs.User user = getUserBySessionToken(sessionToken);
      if (user == null) {
        result.failureReason = Result.FailureReason.Unauthorized;
        return result;
      }

      HashMap<Integer, String> userNameCache = new HashMap<Integer, String>();
      chat.state.structs.Message[] messages = state.getMessages();
      chat.app.structs.Message[] appMessages
      = new chat.app.structs.Message[messages.length];
      for (int i = 0; i < messages.length; i++) {
        appMessages[i] = new chat.app.structs.Message();
        appMessages[i].outgoing = messages[i].userId == user.id;
        if (!appMessages[i].outgoing) {
          if (!userNameCache.containsKey(messages[i].userId)) {
            chat.state.structs.User messageUser = state
            .getUserById(messages[i].userId);
            if (messageUser != null) {
              userNameCache.put(messages[i].userId, messageUser.name);
            } else {
              userNameCache.put(messages[i].userId, "[Deleted user]");
            }
          }
          appMessages[i].userName = userNameCache.get(messages[i].userId);
        }
        appMessages[i].timestamp = messages[i].timestamp;
        appMessages[i].content = messages[i].content;
      }
      result.success = true;
      result.successValue = appMessages;
      return result;
    }

    catch (Exception error) {
      Result<chat.app.structs.Message[]> result
      = new Result<chat.app.structs.Message[]>();
      result.failureReason = Result.FailureReason.Unknown;
      return result;
    }
  }

  /*
   * Possible FailureReason values:
   * - Unknown
   * - IllegalArgument
   * - Unauthorized
   */
  public Result<Object> createMessage(String sessionToken,
  chat.app.structs.Message message) {
    try {
      Result<Object> result = new Result<Object>();

      if (Utilities.nullOrEmpty(sessionToken) || message == null
      || message.timestamp < 0 || Utilities.nullOrEmpty(message.content)) {
        result.failureReason = Result.FailureReason.IllegalArgument;
        return result;
      }

      chat.state.structs.User user = getUserBySessionToken(sessionToken);
      if (user == null) {
        result.failureReason = Result.FailureReason.Unauthorized;
        return result;
      }

      chat.state.structs.Message stateMessage
      = new chat.state.structs.Message();
      stateMessage.userId = user.id;
      stateMessage.timestamp = message.timestamp;
      stateMessage.content = message.content;
      result.success = state.createMessage(stateMessage);
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

  private App(State state) {
    this.state = state;
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

  public class Result<T> {
    public boolean success;
    public T successValue;
    public FailureReason failureReason;

    public enum FailureReason {
      Unknown, IllegalArgument, Unauthorized, NotFound, Conflict
    }
  }
}