package chat;

import java.sql.*;

public class DbHelper {
  public static void close(PreparedStatement statement, ResultSet results) {
    try {
      results.close();
    } catch (Exception error) { }
    try {
      statement.close();
    } catch (Exception error) { }
  }

  public static int mapSessionIdToUserId(String sessionId,
  Connection connection) throws SQLException {
    PreparedStatement statement = null;
    ResultSet results = null;
    try {
      String queryString
      = "SELECT user, expiration FROM sessions WHERE id = ?;";
      statement = connection.prepareStatement(queryString);
      statement.setString(1, sessionId);
      results = statement.executeQuery();
      if (!results.next()) {
        return -1;
      }
      long sessionExpiration = results.getLong(2);
      if (sessionExpiration <= System.currentTimeMillis()) {
        close(statement, results);
        queryString = "DELETE FROM sessions WHERE id = ?;";
        statement = connection.prepareStatement(queryString);
        statement.setString(1, sessionId);
        statement.executeUpdate();
        return -1;
      }
      int userId = results.getInt(1);
      close(statement, results);
      queryString = "SELECT * FROM users WHERE id = ?;";
      statement = connection.prepareStatement(queryString);
      statement.setInt(1, userId);
      results = statement.executeQuery();
      if (!results.next()) {
        close(statement, results);
        queryString = "DELETE FROM sessions WHERE user = ?;";
        statement = connection.prepareStatement(queryString);
        statement.setInt(1, userId);
        statement.executeUpdate();
        return -1;
      }
      return userId;
    } finally {
      close(statement, results);
    }
  }
}