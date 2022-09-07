package chat;

import java.sql.*;

class RequestWithSessionData {
  public String sessionId;
  public int userId;
  public String contentType;
  public String body;
}