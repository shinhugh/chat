package chat;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.sql.*;
import com.google.gson.*;

public class APIUserServlet extends HttpServlet {
  public void doGet(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException {
    RequestHandler.handleRequestWithSession(request, response,
    new GetRequestHandlerCallback());
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException {
    RequestHandler.handleRequest(request, response,
    new PostRequestHandlerCallback());
  }

  public void doPut(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException {
    RequestHandler.handleRequestWithSession(request, response,
    new PutRequestHandlerCallback());
  }

  public void doDelete(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException {
    RequestHandler.handleRequestWithSession(request, response,
    new DeleteRequestHandlerCallback());
  }

  private class GetRequestHandlerCallback
  implements RequestWithSessionHandlerCallback {
    public ResponseData call(RequestWithSessionData requestData) {
      PreparedStatement statement = null;
      ResultSet results = null;
      try {
        ResponseData responseData = new ResponseData();
        String queryString = "SELECT name FROM users WHERE id = ?;";
        statement = requestData.connection.prepareStatement(queryString);
        statement.setInt(1, requestData.userId);
        results = statement.executeQuery();
        results.next();
        User user = new User();
        user.name = results.getString(1);
        Gson gson = new Gson();
        String userJson = gson.toJson(user);
        responseData.statusCode = 200;
        responseData.contentType = "application/json";
        responseData.body = userJson;
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
        String queryString = "SELECT * FROM users WHERE name = ?;";
        statement = requestData.connection.prepareStatement(queryString);
        statement.setString(1, user.name);
        results = statement.executeQuery();
        if (results.next()) {
          responseData.statusCode = 400;
          return responseData;
        }
        String salt = Utilities.generateRandomString((short) 16);
        String pwSaltHash = Utilities.generateHash(user.pw, salt);
        queryString = "INSERT INTO users (name, pw, salt) VALUES (?, ?, ?);";
        statement = requestData.connection.prepareStatement(queryString);
        statement.setString(1, user.name);
        statement.setString(2, pwSaltHash);
        statement.setString(3, salt);
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
        DbHelper.close(statement, results);
      }
    }
  }

  private class PutRequestHandlerCallback
  implements RequestWithSessionHandlerCallback {
    public ResponseData call(RequestWithSessionData requestData) {
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
        if (user.name == null && user.pw == null) {
          responseData.statusCode = 400;
          return responseData;
        }
        int affectedCount = 0;
        if (user.pw == null) {
          String queryString = "UPDATE users SET name = ? WHERE id = ?;";
          statement = requestData.connection.prepareStatement(queryString);
          statement.setString(1, user.name);
          statement.setInt(2, requestData.userId);
          affectedCount = statement.executeUpdate();
        } else if (user.name == null) {
          String queryString = "SELECT salt FROM users WHERE id = ?;";
          statement = requestData.connection.prepareStatement(queryString);
          statement.setInt(1, requestData.userId);
          results = statement.executeQuery();
          results.next();
          String salt = results.getString(1);
          DbHelper.close(statement, results);
          String pwSaltHash = Utilities.generateHash(user.pw, salt);
          queryString = "UPDATE users SET pw = ? WHERE id = ?;";
          statement = requestData.connection.prepareStatement(queryString);
          statement.setString(1, pwSaltHash);
          statement.setInt(2, requestData.userId);
          affectedCount = statement.executeUpdate();
        } else {
          String queryString = "SELECT salt FROM users WHERE id = ?;";
          statement = requestData.connection.prepareStatement(queryString);
          statement.setInt(1, requestData.userId);
          results = statement.executeQuery();
          results.next();
          String salt = results.getString(1);
          DbHelper.close(statement, results);
          String pwSaltHash = Utilities.generateHash(user.pw, salt);
          queryString = "UPDATE users SET name = ?, pw = ? WHERE id = ?;";
          statement = requestData.connection.prepareStatement(queryString);
          statement.setString(1, user.name);
          statement.setString(2, pwSaltHash);
          statement.setInt(3, requestData.userId);
          affectedCount = statement.executeUpdate();
        }
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
        String queryString = "DELETE FROM users WHERE id = ?;";
        statement = requestData.connection.prepareStatement(queryString);
        statement.setInt(1, requestData.userId);
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