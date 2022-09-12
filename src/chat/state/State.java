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
   * Message[] getMessages() +
   * Message[] getMessagesUntilTimestamp(long timestamp, int count)
   * boolean createMessage(Message message) +
   */

  public User getUserById(int userId)
  throws Exception {
    if (userId < 1) {
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
      statement.setInt(1, userId);
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
      user.id = results.getInt(1);
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
    if (user == null || Utilities.nullOrEmpty(user.name) || Utilities.nullOrEmpty(user.hash) || Utilities.nullOrEmpty(user.salt)) {
      throw new IllegalArgumentException();
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;

    try {
      String queryString = null;
      if (user.id > 0) {
        queryString = "INSERT INTO users (id, name, hash, salt) VALUES (?, ?, ?, ?);";
        statement = connection.prepareStatement(queryString);
        statement.setInt(1, user.id);
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

      queryString = "INSERT INTO users (name, hash, salt) VALUES (?, ?, ?);";
      statement = connection.prepareStatement(queryString);
      statement.setString(1, user.name);
      statement.setString(2, user.hash);
      statement.setString(3, user.salt);
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
    if (user == null || user.id < 1 || (Utilities.nullOrEmpty(user.name) && Utilities.nullOrEmpty(user.hash) && Utilities.nullOrEmpty(user.salt))) {
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
        statement.setInt(2, user.id);
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
        statement.setInt(2, user.id);
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
        statement.setInt(2, user.id);
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

  public boolean deleteUserById(int userId)
  throws Exception {
    if (userId < 1) {
      throw new IllegalArgumentException();
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;

    try {
      String queryString = "DELETE FROM users WHERE id = ?;";
      statement = connection.prepareStatement(queryString);
      statement.setInt(1, userId);
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
      session.userId = results.getInt(1);
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
    if (session == null || Utilities.nullOrEmpty(session.id) || session.userId < 1 || session.expiration < 0) {
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
      statement.setInt(2, session.userId);
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

  public boolean deleteSessionsByUserId(int userId)
  throws Exception {
    if (userId < 1) {
      throw new IllegalArgumentException();
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;

    try {
      String queryString = "DELETE FROM sessions WHERE userId = ?;";
      statement = connection.prepareStatement(queryString);
      statement.setInt(1, userId);
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

  public Message[] getMessages()
  throws Exception {
    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;
    ResultSet results = null;

    try {
      ArrayList<Message> messages = new ArrayList<Message>();
      String queryString = "SELECT id, userId, timestamp, content FROM messages ORDER BY timestamp ASC;";
      statement = connection.prepareStatement(queryString);
      results = statement.executeQuery();
      while (results.next()) {
        Message message = new Message();
        message.id = results.getInt(1);
        message.userId = results.getInt(2);
        message.timestamp = results.getLong(3);
        message.content = results.getString(4);
        messages.add(message);
      }
      Message[] output = new Message[messages.size()];
      for (int i = 0; i < messages.size(); i++) {
        output[i] = messages.get(i);
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

  public Message[] getMessagesUntilTimestamp(long timestamp, int limit)
  throws Exception {
    if (timestamp < 0 || limit < 0) {
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
      statement.setLong(1, timestamp);
      statement.setInt(2, limit);
      results = statement.executeQuery();
      while (results.next() && limit-- > 0) {
        Message message = new Message();
        message.id = results.getInt(1);
        message.userId = results.getInt(2);
        message.timestamp = results.getLong(3);
        message.content = results.getString(4);
        messages.add(message);
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
    if (message == null || message.userId < 1 || message.timestamp < 0 || Utilities.nullOrEmpty(message.content)) {
      throw new IllegalArgumentException();
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;

    try {
      if (message.id > 0) {
        String queryString = "INSERT INTO messages (id, userId, timestamp, content) VALUES (?, ?, ?, ?);";
        statement = connection.prepareStatement(queryString);
        statement.setInt(1, message.id);
        statement.setInt(2, message.userId);
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

      String queryString = "INSERT INTO messages (userId, timestamp, content) VALUES (?, ?, ?);";
      statement = connection.prepareStatement(queryString);
      statement.setInt(1, message.userId);
      statement.setLong(2, message.timestamp);
      statement.setString(3, message.content);
      return statement.executeUpdate() == 1;
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