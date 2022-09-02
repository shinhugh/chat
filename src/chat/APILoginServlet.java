package chat;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.sql.*;
import java.util.*;
import com.google.gson.*;

public class APILoginServlet extends HttpServlet {
  public void doPost(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException
  {
    RequestHandler.handleRequest(request, response,
    new PostRequestHandlerCallback());
  }

  public void doDelete(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException
  {
    RequestHandler.handleRequestWithSession(request, response,
    new DeleteRequestHandlerCallback());
  }

  private class PostRequestHandlerCallback
  implements RequestHandlerCallback {
    public ResponseData call(RequestData requestData) {
      PreparedStatement statement = null;
      ResultSet results = null;
      try {
        ResponseData responseData = new ResponseData();
        if (requestData.body == null) {
          responseData.statusCode = 400;
          return responseData;
        }
        User user = null;
        try {
          Gson gson = new Gson();
          user = gson.fromJson(requestData.body, User.class);
        } catch (Exception error) {
          responseData.statusCode = 400;
          return responseData;
        }
        if (user.name == null || user.pw == null) {
          responseData.statusCode = 400;
          return responseData;
        }
        String queryString = "SELECT id, pw, salt FROM users WHERE name = ?;";
        statement = requestData.connection.prepareStatement(queryString);
        statement.setString(1, user.name);
        results = statement.executeQuery();
        if (!results.next()) {
          responseData.statusCode = 403;
          return responseData;
        }
        String pwExpected = results.getString(2);
        String salt = results.getString(3);
        if (!Utilities.generateHash(user.pw, salt).equals(pwExpected)) {
          responseData.statusCode = 403;
          return responseData;
        }
        String sessionId = Utilities.generateRandomString(32);
        int userId = results.getInt(1);
        DbHelper.close(statement, results);
        queryString = "SELECT * FROM sessions WHERE id = ?;";
        statement = requestData.connection.prepareStatement(queryString);
        statement.setString(1, sessionId);
        results = statement.executeQuery();
        while (results.next()) {
          DbHelper.close(null, results);
          sessionId = Utilities.generateRandomString(32);
          statement.setString(1, sessionId);
          results = statement.executeQuery();
        }
        long expiration = System.currentTimeMillis() + 86400000;
        DbHelper.close(statement, results);
        queryString
        = "INSERT INTO sessions (id, user, expiration) VALUES (?, ?, ?);";
        statement = requestData.connection.prepareStatement(queryString);
        statement.setString(1, sessionId);
        statement.setInt(2, userId);
        statement.setLong(3, expiration);
        int affectedCount = statement.executeUpdate();
        if (affectedCount != 1) {
          responseData.statusCode = 500;
          return responseData;
        }
        responseData.statusCode = 200;
        responseData.cookies = new HashMap<String, Map.Entry<String, Long>>();
        responseData.cookies.put("session",
        new AbstractMap.SimpleEntry<String, Long>(sessionId, expiration));
        return responseData;
      } catch (Exception error) {
        ResponseData responseData = new ResponseData();
        responseData.statusCode = 500;
        return responseData;
      } finally {
        DbHelper.close(statement, results);
      }
    }
  }

  private class DeleteRequestHandlerCallback
  implements RequestWithSessionHandlerCallback {
    public ResponseData call(RequestWithSessionData requestData) {
      PreparedStatement statement = null;
      try {
        ResponseData responseData = new ResponseData();
        String queryString = "DELETE FROM sessions WHERE id = ?;";
        statement = requestData.connection.prepareStatement(queryString);
        statement.setString(1, requestData.sessionId);
        int affectedCount = statement.executeUpdate();
        if (affectedCount != 1) {
          responseData.statusCode = 500;
          return responseData;
        }
        responseData.statusCode = 200;
        return responseData;
      } catch (Exception error) {
        ResponseData responseData = new ResponseData();
        responseData.statusCode = 500;
        return responseData;
      } finally {
        DbHelper.close(statement, null);
      }
    }
  }
}