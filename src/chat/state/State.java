package chat.state;

// TODO: Thread safety for shared Connection instance

import chat.state.structs.*;
import chat.util.*;
import java.sql.*;
import java.util.*;

public class State {
  public static State shared = new State();

  private static final String dbDriverClass = "org.mariadb.jdbc.Driver";
  private static final String dbUrl = "jdbc:mariadb://localhost/chat";
  private static final String dbUser = "root";
  private static final String dbPw = "";
  private static final int connectionCheckTimeout = 1;
  private static final int dbErrorCodeDuplicateKey = 1062;

  private static void close(Connection connection) {
    if (connection == null) {
      return;
    }
    try {
      connection.close();
    } catch (SQLException error) { }
  }

  private static void close(PreparedStatement statement) {
    if (statement == null) {
      return;
    }
    try {
      statement.close();
    } catch (SQLException error) { }
  }

  private static void close(ResultSet results) {
    if (results == null) {
      return;
    }
    try {
      results.close();
    } catch (SQLException error) { }
  }

  private Connection connection;

  public void close() {
    close(connection);
    connection = null;
  }

  /*
   * All the following methods will:
   * - Throw an exception on a call with illegal arguments (e.g. null, < 1)
   * - Throw an exception on unexpected errors (e.g. database not responding)
   * - Return instance (found) / null (not found) for read methods
   * - Return true (success) / false (failure) for write methods
   *
   * User getUserById(int userId) +
   * User getUserByName(String userName) +
   * boolean createUser(User user) +
   * boolean updateUser(User user) +
   * boolean deleteUserById(int userId) +
   *
   * Session getSessionById(String sessionId) +
   * boolean createSession(Session session) +
   * boolean deleteSessionById(String sessionId) +
   * boolean deleteSessionsByUserId(int userId) +
   * boolean deleteSessionsByExpirationCutoff(long sessionExpirationCutoff)
   *
   * Message[] getMessagesUntilTimestamp(long maxTimestamp, int count) +
   * boolean createMessage(Message message) +
   */

  public User getUserById(String userId)
  throws Exception {
    if (Utilities.nullOrEmpty(userId)) {
      throw new IllegalArgumentException();
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;
    ResultSet results = null;

    try {
      String queryString = "SELECT name, hash, salt FROM users WHERE id = ?;";
      statement = connection.prepareStatement(queryString);
      statement.setString(1, userId);
      results = statement.executeQuery();
      if (!results.next()) {
        return null;
      }

      User user = new User();
      user.id = userId;
      user.name = results.getString(1);
      user.hash = results.getString(2);
      user.salt = results.getString(3);
      return user;
    }

    catch (SQLException error) {
      throw new Exception("Encountered unexpected behavior with database");
    }
    finally {
      close(results);
      close(statement);
    }
  }

  public User getUserByName(String userName)
  throws Exception {
    if (Utilities.nullOrEmpty(userName)) {
      throw new IllegalArgumentException();
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;
    ResultSet results = null;

    try {
      String queryString = "SELECT id, hash, salt FROM users WHERE name = ?;";
      statement = connection.prepareStatement(queryString);
      statement.setString(1, userName);
      results = statement.executeQuery();
      if (!results.next()) {
        return null;
      }

      User user = new User();
      user.id = results.getString(1);
      user.name = userName;
      user.hash = results.getString(2);
      user.salt = results.getString(3);
      return user;
    }

    catch (SQLException error) {
      throw new Exception("Encountered unexpected behavior with database");
    }
    finally {
      close(results);
      close(statement);
    }
  }

  public boolean createUser(User user)
  throws Exception {
    if (user == null || Utilities.nullOrEmpty(user.id) || Utilities.nullOrEmpty(user.name) || Utilities.nullOrEmpty(user.hash) || Utilities.nullOrEmpty(user.salt)) {
      throw new IllegalArgumentException();
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;

    try {
      String queryString = "INSERT INTO users (id, name, hash, salt) VALUES (?, ?, ?, ?);";
      statement = connection.prepareStatement(queryString);
      statement.setString(1, user.id);
      statement.setString(2, user.name);
      statement.setString(3, user.hash);
      statement.setString(4, user.salt);
      try {
        return statement.executeUpdate() == 1;
      } catch (SQLException error) {
        if (error.getErrorCode() == dbErrorCodeDuplicateKey) {
          return false;
        }
        throw error;
      }
    }

    catch (SQLException error) {
      throw new Exception("Encountered unexpected behavior with database");
    }
    finally {
      close(statement);
    }
  }

  public boolean updateUser(User user)
  throws Exception {
    if (user == null || Utilities.nullOrEmpty(user.id) || (Utilities.nullOrEmpty(user.name) && Utilities.nullOrEmpty(user.hash) && Utilities.nullOrEmpty(user.salt))) {
      throw new IllegalArgumentException();
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;

    try {
      connection.setAutoCommit(false);

      if (!Utilities.nullOrEmpty(user.name)) {
        String queryString = "UPDATE users SET name = ? WHERE id = ?;";
        statement = connection.prepareStatement(queryString);
        statement.setString(1, user.name);
        statement.setString(2, user.id);
        try {
          if (statement.executeUpdate() != 1) {
            return false;
          }
        } catch (SQLException error) {
          if (error.getErrorCode() == dbErrorCodeDuplicateKey) {
            return false;
          }
          throw error;
        }
      }

      if (!Utilities.nullOrEmpty(user.hash)) {
        close(statement);
        String queryString = "UPDATE users SET hash = ? WHERE id = ?;";
        statement = connection.prepareStatement(queryString);
        statement.setString(1, user.hash);
        statement.setString(2, user.id);
        if (statement.executeUpdate() != 1) {
          connection.rollback();
          return false;
        }
      }

      if (!Utilities.nullOrEmpty(user.salt)) {
        close(statement);
        String queryString = "UPDATE users SET salt = ? WHERE id = ?;";
        statement = connection.prepareStatement(queryString);
        statement.setString(1, user.salt);
        statement.setString(2, user.id);
        if (statement.executeUpdate() != 1) {
          connection.rollback();
          return false;
        }
      }

      connection.commit();
      return true;
    }

    catch (SQLException errorA) {
      try {
        connection.rollback();
      } catch (SQLException errorB) { }
      throw new Exception("Encountered unexpected behavior with database");
    }
    finally {
      close(statement);
      try {
        connection.setAutoCommit(true);
      } catch (SQLException error) { }
    }
  }

  public boolean deleteUserById(String userId)
  throws Exception {
    if (Utilities.nullOrEmpty(userId)) {
      throw new IllegalArgumentException();
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;

    try {
      String queryString = "DELETE FROM users WHERE id = ?;";
      statement = connection.prepareStatement(queryString);
      statement.setString(1, userId);
      return statement.executeUpdate() == 1;
    }

    catch (SQLException error) {
      throw new Exception("Encountered unexpected behavior with database");
    }
    finally {
      close(statement);
    }
  }

  public Session getSessionById(String sessionId)
  throws Exception {
    if (Utilities.nullOrEmpty(sessionId)) {
      throw new IllegalArgumentException();
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;
    ResultSet results = null;

    try {
      String queryString = "SELECT userId, expiration FROM sessions WHERE id = ?;";
      statement = connection.prepareStatement(queryString);
      statement.setString(1, sessionId);
      results = statement.executeQuery();
      if (!results.next()) {
        return null;
      }

      Session session = new Session();
      session.id = sessionId;
      session.userId = results.getString(1);
      session.expiration = results.getLong(2);
      return session;
    }

    catch (SQLException error) {
      throw new Exception("Encountered unexpected behavior with database");
    }
    finally {
      close(results);
      close(statement);
    }
  }

  public boolean createSession(Session session)
  throws Exception {
    if (session == null || Utilities.nullOrEmpty(session.id) || Utilities.nullOrEmpty(session.userId) || session.expiration < 0) {
      throw new IllegalArgumentException();
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;

    try {
      String queryString = "INSERT INTO sessions (id, userId, expiration) VALUES (?, ?, ?);";
      statement = connection.prepareStatement(queryString);
      statement.setString(1, session.id);
      statement.setString(2, session.userId);
      statement.setLong(3, session.expiration);
      try {
        return statement.executeUpdate() == 1;
      } catch (SQLException error) {
        if (error.getErrorCode() == dbErrorCodeDuplicateKey) {
          return false;
        }
        throw error;
      }
    }

    catch (SQLException error) {
      throw new Exception("Encountered unexpected behavior with database");
    }
    finally {
      close(statement);
    }
  }

  public boolean deleteSessionById(String sessionId)
  throws Exception {
    if (Utilities.nullOrEmpty(sessionId)) {
      throw new IllegalArgumentException();
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;

    try {
      String queryString = "DELETE FROM sessions WHERE id = ?;";
      statement = connection.prepareStatement(queryString);
      statement.setString(1, sessionId);
      return statement.executeUpdate() == 1;
    }

    catch (SQLException error) {
      throw new Exception("Encountered unexpected behavior with database");
    }
    finally {
      close(statement);
    }
  }

  public boolean deleteSessionsByUserId(String userId)
  throws Exception {
    if (Utilities.nullOrEmpty(userId)) {
      throw new IllegalArgumentException();
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;

    try {
      String queryString = "DELETE FROM sessions WHERE userId = ?;";
      statement = connection.prepareStatement(queryString);
      statement.setString(1, userId);
      statement.executeUpdate();
      return true;
    }

    catch (SQLException error) {
      throw new Exception("Encountered unexpected behavior with database");
    }
    finally {
      close(statement);
    }
  }

  public boolean deleteSessionsByExpirationCutoff(long sessionExpirationCutoff)
  throws Exception {
    if (sessionExpirationCutoff < 0) {
      throw new IllegalArgumentException();
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;

    try {
      String queryString = "DELETE FROM sessions WHERE expiration <= ?;";
      statement = connection.prepareStatement(queryString);
      statement.setLong(1, sessionExpirationCutoff);
      statement.executeUpdate();
      return true;
    }

    catch (SQLException error) {
      throw new Exception("Encountered unexpected behavior with database");
    }
    finally {
      close(statement);
    }
  }

  public Message getMessageById(String messageId)
  throws Exception {
    if (Utilities.nullOrEmpty(messageId)) {
      throw new IllegalArgumentException();
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;
    ResultSet results = null;

    try {
      String queryString = "SELECT userId, timestamp, content FROM messages WHERE id = ?;";
      statement = connection.prepareStatement(queryString);
      statement.setString(1, messageId);
      results = statement.executeQuery();
      if (!results.next()) {
        return null;
      }

      Message message = new Message();
      message.id = messageId;
      message.userId = results.getString(1);
      message.timestamp = results.getLong(2);
      message.content = results.getString(3);
      return message;
    }

    catch (SQLException error) {
      throw new Exception("Encountered unexpected behavior with database");
    }
    finally {
      close(results);
      close(statement);
    }
  }

  public Message[] getMessagesUntilTimestamp(long maxTimestamp, int limit)
  throws Exception {
    if (maxTimestamp < 0 || limit < 1) {
      throw new IllegalArgumentException();
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;
    ResultSet results = null;

    try {
      ArrayList<Message> messages = new ArrayList<Message>();
      String queryString = "SELECT id, userId, timestamp, content FROM messages WHERE timestamp <= ? ORDER BY timestamp DESC LIMIT ?;";
      statement = connection.prepareStatement(queryString);
      statement.setLong(1, maxTimestamp);
      statement.setInt(2, limit);
      results = statement.executeQuery();
      while (results.next() && limit-- > 0) {
        Message message = new Message();
        message.id = results.getString(1);
        message.userId = results.getString(2);
        message.timestamp = results.getLong(3);
        message.content = results.getString(4);
        messages.add(message);
      }
      if (messages.size() == 0) {
        return null;
      }
      Message[] output = new Message[messages.size()];
      for (int i = 0; i < messages.size(); i++) {
        output[i] = messages.get(messages.size() - i - 1);
      }
      return output;
    }

    catch (SQLException error) {
      throw new Exception("Encountered unexpected behavior with database");
    }
    finally {
      close(results);
      close(statement);
    }
  }

  public boolean createMessage(Message message)
  throws Exception {
    if (message == null || Utilities.nullOrEmpty(message.id) || Utilities.nullOrEmpty(message.userId) || message.timestamp < 0 || Utilities.nullOrEmpty(message.content)) {
      throw new IllegalArgumentException();
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;

    try {
      String queryString = "INSERT INTO messages (id, userId, timestamp, content) VALUES (?, ?, ?, ?);";
      statement = connection.prepareStatement(queryString);
      statement.setString(1, message.id);
      statement.setString(2, message.userId);
      statement.setLong(3, message.timestamp);
      statement.setString(4, message.content);
      try {
        return statement.executeUpdate() == 1;
      } catch (SQLException error) {
        if (error.getErrorCode() == dbErrorCodeDuplicateKey) {
          return false;
        }
        throw error;
      }
    }

    catch (SQLException error) {
      throw new Exception("Encountered unexpected behavior with database");
    }
    finally {
      close(statement);
    }
  }

  private State() {
    try {
      Class.forName(dbDriverClass);
    } catch (ClassNotFoundException error) {
      return;
    }
    try {
      connection = DriverManager.getConnection(dbUrl, dbUser, dbPw);
    } catch (SQLException error) { }
  }

  private boolean assureConnection() {
    try {
      if (connection == null || !connection.isValid(connectionCheckTimeout)) {
        close();
        connection = DriverManager.getConnection(dbUrl, dbUser, dbPw);
      }
      return true;
    } catch (SQLException error) {
      return false;
    }
  }
}