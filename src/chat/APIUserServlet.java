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
    // TODO
  }

  private class PutRequestHandlerCallback
  implements RequestWithSessionHandlerCallback {
    public ResponseData call(RequestWithSessionData requestData) {
      PreparedStatement statement = null;
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
        if (user.name == null) {
          responseData.statusCode = 400;
          return responseData;
        }
        String queryString = "UPDATE users SET name = ? WHERE id = ?;";
        statement = requestData.connection.prepareStatement(queryString);
        statement.setString(1, user.name);
        statement.setInt(2, requestData.userId);
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

  private class User {
    private String name;
  }
}