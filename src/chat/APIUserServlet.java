package chat;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.sql.*;

public class APIUserServlet extends HttpServlet {
  public void doGet(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException
  {
    RequestHandler.handleRequestWithSession(request, response,
    new GetRequestHandlerCallback());
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException
  {
    // TODO
    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  public void doPut(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException
  {
    RequestHandler.handleRequestWithSession(request, response,
    new PutRequestHandlerCallback());
  }

  public void doDelete(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException
  {
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
        String userName = results.getString(1);
        responseData.statusCode = 200;
        responseData.contentType = "application/json";
        responseData.body = "{\"name\":\"" + userName + "\"}\n";
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

  // TODO: Implement PostRequestHandlerCallback

  private class PutRequestHandlerCallback
  implements RequestWithSessionHandlerCallback {
    public ResponseData call(RequestWithSessionData requestData) {
      PreparedStatement statement = null;
      ResultSet results = null;
      try {
        ResponseData responseData = new ResponseData();
        String userNameNew = "John"; // TODO: Parse request body for new name
        String queryString = "UPDATE users SET name = ? WHERE id = ?;";
        statement = requestData.connection.prepareStatement(queryString);
        statement.setString(1, userNameNew);
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
        DbHelper.close(statement, results);
      }
    }
  }

  private class DeleteRequestHandlerCallback
  implements RequestWithSessionHandlerCallback {
    public ResponseData call(RequestWithSessionData requestData) {
      PreparedStatement statement = null;
      ResultSet results = null;
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
        DbHelper.close(statement, results);
      }
    }
  }
}