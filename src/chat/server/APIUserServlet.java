package chat.server;

import chat.*;
import chat.app.*;
import chat.app.structs.*;
import chat.server.structs.*;
import com.google.gson.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;

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
      try {
        ResponseData responseData = new ResponseData();
        App.Result<User> result = App.shared.getUser(requestData.sessionToken);
        if (!result.success) {
          if (result.failureReason == App.Result.FailureReason.Unauthorized) {
            responseData.statusCode = 403;
            return responseData;
          }
          responseData.statusCode = 500;
          return responseData;
        }
        Gson gson = new Gson();
        String userJson = gson.toJson(result.successValue);
        responseData.statusCode = 200;
        responseData.contentType = "application/json";
        responseData.body = userJson;
        return responseData;
      }

      catch (Exception error) {
        ResponseData responseData = new ResponseData();
        responseData.statusCode = 500;
        return responseData;
      }
    }
  }

  private class PostRequestHandlerCallback
  implements RequestHandlerCallback {
    public ResponseData call(RequestData requestData) {
      try {
        ResponseData responseData = new ResponseData();
        if (Utilities.nullOrEmpty(requestData.body)) {
          responseData.statusCode = 400;
          return responseData;
        }
        Credentials credentials = null;
        try {
          Gson gson = new Gson();
          credentials = gson.fromJson(requestData.body, Credentials.class);
        } catch (JsonSyntaxException error) {
          responseData.statusCode = 400;
          return responseData;
        }
        if (Utilities.nullOrEmpty(credentials.name)
        || Utilities.nullOrEmpty(credentials.pw)) {
          responseData.statusCode = 400;
          return responseData;
        }
        App.Result<Object> result = App.shared.createUser(credentials);
        if (!result.success) {
          if (result.failureReason == App.Result.FailureReason.Conflict) {
            responseData.statusCode = 400;
            return responseData;
          }
          responseData.statusCode = 500;
          return responseData;
        }
        responseData.statusCode = 200;
        return responseData;
      }

      catch (Exception error) {
        ResponseData responseData = new ResponseData();
        responseData.statusCode = 500;
        return responseData;
      }
    }
  }

  private class PutRequestHandlerCallback
  implements RequestWithSessionHandlerCallback {
    public ResponseData call(RequestWithSessionData requestData) {
      try {
        ResponseData responseData = new ResponseData();
        if (Utilities.nullOrEmpty(requestData.body)) {
          responseData.statusCode = 400;
          return responseData;
        }
        Credentials credentials = null;
        try {
          Gson gson = new Gson();
          credentials = gson.fromJson(requestData.body, Credentials.class);
        } catch (JsonSyntaxException error) {
          responseData.statusCode = 400;
          return responseData;
        }
        if (Utilities.nullOrEmpty(credentials.name)
        && Utilities.nullOrEmpty(credentials.pw)) {
          responseData.statusCode = 400;
          return responseData;
        }
        App.Result<Object> result = App.shared.updateUser(requestData.sessionToken,
        credentials);
        if (!result.success) {
          if (result.failureReason == App.Result.FailureReason.Unauthorized) {
            responseData.statusCode = 403;
            return responseData;
          }
          if (result.failureReason == App.Result.FailureReason.Conflict) {
            responseData.statusCode = 400;
            return responseData;
          }
          responseData.statusCode = 500;
          return responseData;
        }
        responseData.statusCode = 200;
        return responseData;
      }

      catch (Exception error) {
        ResponseData responseData = new ResponseData();
        responseData.statusCode = 500;
        return responseData;
      }
    }
  }

  private class DeleteRequestHandlerCallback
  implements RequestWithSessionHandlerCallback {
    public ResponseData call(RequestWithSessionData requestData) {
      try {
        ResponseData responseData = new ResponseData();
        App.Result<Object> result = App.shared.deleteUser(requestData.sessionToken);
        if (!result.success) {
          if (result.failureReason == App.Result.FailureReason.Unauthorized) {
            responseData.statusCode = 403;
            return responseData;
          }
          responseData.statusCode = 500;
          return responseData;
        }
        responseData.statusCode = 200;
        return responseData;
      }

      catch (Exception error) {
        ResponseData responseData = new ResponseData();
        responseData.statusCode = 500;
        return responseData;
      }
    }
  }
}