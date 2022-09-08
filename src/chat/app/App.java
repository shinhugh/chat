package chat.app;

import chat.app.structs.*;
import chat.state.*;
import chat.state.structs.*;

public class App {
  public static App shared = new App(State.shared);

  private State state;

  public chat.app.structs.Session logIn(Credentials credentials)
  throws Exception {
    if (credentials == null || Utilities.nullOrEmpty(credentials.name)
    || Utilities.nullOrEmpty(credentials.pw)) {
      throw new IllegalArgumentException("Illegal argument");
    }

    chat.state.structs.User user = state.getUserByName(credentials.name);
    if (user == null) {
      return null;
    }

    String hash = Utilities.generateHash(credentials.pw, user.salt);
    if (!hash.equals(user.hash)) {
      return null;
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
      return appSession;
    }
    return null;
  }

  public boolean logOut(String sessionToken)
  throws Exception {
    if (Utilities.nullOrEmpty(sessionToken)) {
      throw new IllegalArgumentException("Illegal argument");
    }

    return state.deleteSessionById(sessionToken);
  }

  public boolean verifySessionToken(String sessionToken)
  throws Exception {
    return getUserBySessionToken(sessionToken) != null;
  }

  public chat.app.structs.User getUser(String sessionToken)
  throws Exception {
    chat.state.structs.User user = getUserBySessionToken(sessionToken);
    if (user == null) {
      return null;
    }

    chat.app.structs.User appUser = new chat.app.structs.User();
    appUser.name = user.name;
    return appUser;
  }

  public boolean createUser(Credentials credentials)
  throws Exception {
    if (credentials == null || Utilities.nullOrEmpty(credentials.name)
    || Utilities.nullOrEmpty(credentials.pw)) {
      throw new IllegalArgumentException("Illegal argument");
    }

    if (state.getUserByName(credentials.name) != null) {
      return false;
    }

    chat.state.structs.User user = new chat.state.structs.User();
    user.name = credentials.name;
    user.salt = Utilities.generateRandomString((short) 16);
    user.hash = Utilities.generateHash(credentials.pw, user.salt);
    return state.createUser(user);
  }

  public boolean updateUser(String sessionToken, Credentials credentials)
  throws Exception {
    chat.state.structs.User user = getUserBySessionToken(sessionToken);

    if (credentials == null || (Utilities.nullOrEmpty(credentials.name)
    && Utilities.nullOrEmpty(credentials.pw))) {
      throw new IllegalArgumentException("Illegal argument");
    }

    if (user == null) {
      return false;
    }

    int userId = user.id;
    user = new chat.state.structs.User();
    user.id = userId;
    if (!Utilities.nullOrEmpty(credentials.name)) {
      user.name = credentials.name;
    }
    if (!Utilities.nullOrEmpty(credentials.pw)) {
      user.salt = Utilities.generateRandomString((short) 16);
      user.hash = Utilities.generateHash(credentials.pw, user.salt);
    }
    return state.updateUser(user);
  }

  public boolean deleteUser(String sessionToken)
  throws Exception {
    chat.state.structs.User user = getUserBySessionToken(sessionToken);
    if (user == null) {
      return false;
    }

    state.deleteSessionsByUserId(user.id);
    return state.deleteUserById(user.id);
  }

  public chat.app.structs.Message[] getMessages(String sessionToken)
  throws Exception {
    chat.state.structs.User user = getUserBySessionToken(sessionToken);
    if (user == null) {
      return null;
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
    return appMessages;
  }

  public boolean createMessage(String sessionToken,
  chat.app.structs.Message message)
  throws Exception {
    chat.state.structs.User user = getUserBySessionToken(sessionToken);
    if (user == null) {
      return false;
    }

    chat.state.structs.Message stateMessage = new chat.state.structs.Message();
    stateMessage.userId = user.id;
    stateMessage.timestamp = message.timestamp;
    stateMessage.content = message.content;
    return state.createMessage(stateMessage);
  }

  private App(State state) {
    this.state = state;
  }

  private chat.state.structs.User getUserBySessionToken(String sessionToken) {
    if (Utilities.nullOrEmpty(sessionToken)) {
      throw new IllegalArgumentException("Illegal argument");
    }

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
}