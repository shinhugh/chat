package chat;

import java.sql.*;

class RequestWithSessionData {
  public Connection connection;
  public String sessionId;
  public int userId;
  public String contentType;
  public String body;
}