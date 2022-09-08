package chat;

// TODO: Thread safety for shared Connection instance
// TODO: Sanity check with all method arguments (null, < 0, etc)

import java.lang.*;
import java.sql.*;
import java.util.*;

class PersistentModel {
  public static PersistentModel shared = new PersistentModel();

  private static final String dbDriverClass = "org.mariadb.jdbc.Driver";
  private static final String dbUrl = "jdbc:mariadb://localhost/chat";
  private static final String dbUser = "root";
  private static final String dbPw = "";
  private static final int connectionCheckTimeout = 1;

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
   * User getUserById(int userId)
   * User getUserByName(String userName)
   * User getUserBySessionId(String sessionId)
   * boolean createUser(User user)
   * boolean updateUser(User user)
   * boolean deleteUserById(int userId)
   *
   * Session getSessionById(String sessionId)
   * boolean createSession(Session session)
   * boolean deleteSessionById(String sessionId)
   *
   * Message[] getMessages()
   * boolean createMessage(Message message)
   */

  public User getUserById(int userId)
  throws Exception {
    if (userId < 1) {
      throw new IllegalArgumentException("Illegal argument");
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;
    ResultSet results = null;

    try {
      String queryString = "SELECT name, pw, salt FROM users WHERE id = ?;";
      statement = connection.prepareStatement(queryString);
      statement.setInt(1, userId);
      results = statement.executeQuery();
      if (!results.next()) {
        return null;
      }

      User user = new User();
      user.id = userId;
      user.name = results.getString(1);
      user.pw = results.getString(2);
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
      throw new IllegalArgumentException("Illegal argument");
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;
    ResultSet results = null;

    try {
      String queryString = "SELECT id, pw, salt FROM users WHERE name = ?;";
      statement = connection.prepareStatement(queryString);
      statement.setString(1, userName);
      results = statement.executeQuery();
      if (!results.next()) {
        return null;
      }

      User user = new User();
      user.id = results.getInt(1);
      user.name = userName;
      user.pw = results.getString(2);
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

  public User getUserBySessionId(String sessionId)
  throws Exception {
    if (Utilities.nullOrEmpty(sessionId)) {
      throw new IllegalArgumentException("Illegal argument");
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;
    ResultSet results = null;

    try {
      String queryString = "SELECT user FROM sessions WHERE id = ?;";
      statement = connection.prepareStatement(queryString);
      statement.setString(1, sessionId);
      results = statement.executeQuery();
      if (!results.next()) {
        return null;
      }
      int userId = results.getInt(1);

      close(results);
      close(statement);
      queryString = "SELECT name, pw, salt FROM users WHERE id = ?;";
      statement = connection.prepareStatement(queryString);
      statement.setInt(1, userId);
      results = statement.executeQuery();
      if (!results.next()) {
        close(results);
        close(statement);
        queryString = "DELETE FROM sessions WHERE id = ?;";
        statement = connection.prepareStatement(queryString);
        statement.setString(1, sessionId);
        statement.executeUpdate();
        return null;
      }

      User user = new User();
      user.id = userId;
      user.name = results.getString(1);
      user.pw = results.getString(2);
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
    if (user == null || Utilities.nullOrEmpty(user.name)
    || Utilities.nullOrEmpty(user.pw) || Utilities.nullOrEmpty(user.salt)) {
      throw new IllegalArgumentException("Illegal argument");
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;
    ResultSet results = null;

    try {
      String queryString = "SELECT * FROM users WHERE name = ?;";
      statement = connection.prepareStatement(queryString);
      statement.setString(1, user.name);
      results = statement.executeQuery();
      if (results.next()) {
        return false;
      }

      close(results);
      close(statement);
      queryString = "INSERT INTO users (name, pw, salt) VALUES (?, ?, ?);";
      statement = connection.prepareStatement(queryString);
      statement.setString(1, user.name);
      statement.setString(2, user.pw);
      statement.setString(3, user.salt);
      return statement.executeUpdate() == 1;
    }

    catch (SQLException error) {
      throw new Exception("Encountered unexpected behavior with database");
    }
    finally {
      close(results);
      close(statement);
    }
  }

  public boolean updateUser(User user)
  throws Exception {
    if (user == null || user.id < 1 || (Utilities.nullOrEmpty(user.name)
    && Utilities.nullOrEmpty(user.pw) && Utilities.nullOrEmpty(user.salt))) {
      throw new IllegalArgumentException("Illegal argument");
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;
    ResultSet results = null;

    try {
      if (!Utilities.nullOrEmpty(user.name)) {
        String queryString = "SELECT * FROM users WHERE name = ?"
        + " AND NOT id = ?;";
        statement = connection.prepareStatement(queryString);
        statement.setString(1, user.name);
        statement.setInt(2, user.id);
        results = statement.executeQuery();
        if (results.next()) {
          return false;
        }
      }

      connection.setAutoCommit(false);

      if (!Utilities.nullOrEmpty(user.name)) {
        close(results);
        close(statement);
        String queryString = "UPDATE users SET name = ? WHERE id = ?;";
        statement = connection.prepareStatement(queryString);
        statement.setString(1, user.name);
        statement.setInt(2, user.id);
        if (statement.executeUpdate() != 1) {
          return false;
        }
      }

      if (!Utilities.nullOrEmpty(user.pw)) {
        close(results);
        close(statement);
        String queryString = "UPDATE users SET pw = ? WHERE id = ?;";
        statement = connection.prepareStatement(queryString);
        statement.setString(1, user.pw);
        statement.setInt(2, user.id);
        if (statement.executeUpdate() != 1) {
          connection.rollback();
          return false;
        }
      }

      if (!Utilities.nullOrEmpty(user.salt)) {
        close(results);
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
      close(results);
      close(statement);
      try {
        connection.setAutoCommit(true);
      } catch (SQLException error) { }
    }
  }

  public boolean deleteUserById(int userId)
  throws Exception {
    if (userId < 1) {
      throw new IllegalArgumentException("Illegal argument");
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
      throw new IllegalArgumentException("Illegal argument");
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;
    ResultSet results = null;

    try {
      String queryString = "SELECT user, expiration FROM sessions"
      + " WHERE id = ?;";
      statement = connection.prepareStatement(queryString);
      statement.setString(1, sessionId);
      results = statement.executeQuery();
      if (!results.next()) {
        return null;
      }

      Session session = new Session();
      session.id = sessionId;
      session.user = results.getInt(1);
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
    if (session == null || Utilities.nullOrEmpty(session.id)
    || session.user < 1 || session.expiration < 0) {
      throw new IllegalArgumentException("Illegal argument");
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;
    ResultSet results = null;

    try {
      String queryString = "SELECT * FROM sessions WHERE id = ?;";
      statement = connection.prepareStatement(queryString);
      statement.setString(1, session.id);
      results = statement.executeQuery();
      if (results.next()) {
        return false;
      }

      close(results);
      close(statement);
      queryString = "INSERT INTO sessions (id, user, expiration)"
      + " VALUES (?, ?, ?);";
      statement = connection.prepareStatement(queryString);
      statement.setString(1, session.id);
      statement.setInt(2, session.user);
      statement.setLong(3, session.expiration);
      return statement.executeUpdate() == 1;
    }

    catch (SQLException error) {
      throw new Exception("Encountered unexpected behavior with database");
    }
    finally {
      close(results);
      close(statement);
    }
  }

  public boolean deleteSessionById(String sessionId)
  throws Exception {
    if (Utilities.nullOrEmpty(sessionId)) {
      throw new IllegalArgumentException("Illegal argument");
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

  public Message[] getMessages()
  throws Exception {
    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;
    ResultSet results = null;

    try {
      ArrayList<Message> messages = new ArrayList<Message>();
      String queryString = "SELECT id, user, timestamp, content FROM messages;";
      statement = connection.prepareStatement(queryString);
      results = statement.executeQuery();
      while (results.next()) {
        Message message = new Message();
        message.id = results.getInt(1);
        message.user = results.getInt(2);
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

  public boolean createMessage(Message message)
  throws Exception {
    if (message == null || message.user < 1 || message.timestamp < 0
    || Utilities.nullOrEmpty(message.content)) {
      throw new IllegalArgumentException("Illegal argument");
    }

    if (!assureConnection()) {
      throw new Exception("Cannot establish connection with database");
    }

    PreparedStatement statement = null;

    try {
      String queryString = "INSERT INTO messages (user, timestamp, content)"
      + " VALUES (?, ?, ?);";
      statement = connection.prepareStatement(queryString);
      statement.setInt(1, message.user);
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

  private PersistentModel() {
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