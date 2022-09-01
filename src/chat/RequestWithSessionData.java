package chat;

import java.sql.*;

class RequestWithSessionData {
  public Connection connection;
  public int userId;
  public String contentType;
  public String body;
}