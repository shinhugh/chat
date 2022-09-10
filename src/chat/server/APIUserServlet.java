package chat.server;

import chat.app.*;
import chat.app.structs.*;
import chat.util.*;
import com.google.gson.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;

public class APIUserServlet extends HttpServlet {
  public void doGet(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException {
    RequestHandler.handleRequest(request, response,
    new GetRequestHandlerCallback());
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException {
    RequestHandler.handleRequest(request, response,
    new PostRequestHandlerCallback());
  }

  public void doPut(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException {
    RequestHandler.handleRequest(request, response,
    new PutRequestHandlerCallback());
  }

  public void doDelete(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException {
    RequestHandler.handleRequest(request, response,
    new DeleteRequestHandlerCallback());
  }

  private static class GetRequestHandlerCallback
  implements RequestHandlerCallback {
    public RequestHandlerCallback.ResponseData call(
    RequestHandlerCallback.RequestData requestData) {
      RequestHandlerCallback.ResponseData responseData
      = new RequestHandlerCallback.ResponseData();
      App.Result<User> result = App.shared.getUser(requestData.sessionToken);
      if (!result.success) {
        switch(result.failureReason) {
          case Unauthorized:
            responseData.statusCode = 403;
            break;
          default:
            responseData.statusCode = 500;
            break;
        }
        return responseData;
      }
      Gson gson = new Gson();
      String userJson = gson.toJson(result.successValue);
      responseData.statusCode = 200;
      responseData.contentType = "application/json";
      responseData.body = userJson;
      return responseData;
    }
  }

  private static class PostRequestHandlerCallback
  implements RequestHandlerCallback {
    public RequestHandlerCallback.ResponseData call(
    RequestHandlerCallback.RequestData requestData) {
      RequestHandlerCallback.ResponseData responseData
      = new RequestHandlerCallback.ResponseData();
      Credentials credentials = null;
      if (!Utilities.nullOrEmpty(requestData.body)) {
        try {
          Gson gson = new Gson();
          credentials = gson.fromJson(requestData.body, Credentials.class);
        } catch (JsonSyntaxException error) { }
      }
      App.Result<Object> result = App.shared.createUser(credentials);
      if (!result.success) {
        switch(result.failureReason) {
          case IllegalArgument:
          case Conflict:
            responseData.statusCode = 400;
            break;
          default:
            responseData.statusCode = 500;
            break;
        }
        return responseData;
      }
      responseData.statusCode = 200;
      return responseData;
    }
  }

  private static class PutRequestHandlerCallback
  implements RequestHandlerCallback {
    public RequestHandlerCallback.ResponseData call(
    RequestHandlerCallback.RequestData requestData) {
      RequestHandlerCallback.ResponseData responseData
      = new RequestHandlerCallback.ResponseData();
      Credentials credentials = null;
      if (!Utilities.nullOrEmpty(requestData.body)) {
        try {
          Gson gson = new Gson();
          credentials = gson.fromJson(requestData.body, Credentials.class);
        } catch (JsonSyntaxException error) { }
      }
      App.Result<Object> result = App.shared.updateUser(requestData
      .sessionToken, credentials);
      if (!result.success) {
        switch(result.failureReason) {
          case Unauthorized:
            responseData.statusCode = 403;
            break;
          case IllegalArgument:
          case Conflict:
            responseData.statusCode = 400;
            break;
          default:
            responseData.statusCode = 500;
            break;
        }
        return responseData;
      }
      responseData.statusCode = 200;
      return responseData;
    }
  }

  private static class DeleteRequestHandlerCallback
  implements RequestHandlerCallback {
    public RequestHandlerCallback.ResponseData call(
    RequestHandlerCallback.RequestData requestData) {
      RequestHandlerCallback.ResponseData responseData
      = new RequestHandlerCallback.ResponseData();
      App.Result<Object> result = App.shared.deleteUser(requestData
      .sessionToken);
      if (!result.success) {
        switch(result.failureReason) {
          case Unauthorized:
            responseData.statusCode = 403;
            break;
          default:
            responseData.statusCode = 500;
            break;
        }
        return responseData;
      }
      responseData.statusCode = 200;
      return responseData;
    }
  }
}