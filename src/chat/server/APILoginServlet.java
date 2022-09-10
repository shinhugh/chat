package chat.server;

import chat.app.*;
import chat.app.structs.*;
import chat.util.*;
import com.google.gson.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;

public class APILoginServlet extends HttpServlet {
  public void doPost(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException {
    RequestHandler.handleRequest(request, response,
    new PostRequestHandlerCallback());
  }

  public void doDelete(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException {
    RequestHandler.handleRequest(request, response,
    new DeleteRequestHandlerCallback());
  }

  private static class PostRequestHandlerCallback
  implements RequestHandler.Callback {
    public RequestHandler.Callback.ResponseData call(
    RequestHandler.Callback.RequestData requestData) {
      RequestHandler.Callback.ResponseData responseData = new ResponseData();
      Credentials credentials = null;
      if (!Utilities.nullOrEmpty(requestData.body)) {
        try {
          Gson gson = new Gson();
          credentials = gson.fromJson(requestData.body, Credentials.class);
        } catch (JsonSyntaxException error) { }
      }
      App.Result<Session> result = App.shared.logIn(credentials);
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
      responseData.cookies = new RequestHandler.Callback.ResponseData.Cookie[1];
      responseData.cookies[0]
      = new RequestHandler.Callback.ResponseData.Cookie();
      responseData.cookies[0].key = "session";
      responseData.cookies[0].value = result.successValue.token;
      responseData.cookies[0].expiration = result.successValue.expiration;
      return responseData;
    }
  }

  private static class DeleteRequestHandlerCallback
  implements RequestHandler.Callback {
    public RequestHandler.Callback.ResponseData call(
    RequestHandler.Callback.RequestData requestData) {
      RequestHandler.Callback.ResponseData responseData = new ResponseData();
      App.Result<Object> result = App.shared.logOut(requestData.sessionToken);
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
      responseData.cookies = new RequestHandler.Callback.ResponseData.Cookie[1];
      responseData.cookies[0]
      = new RequestHandler.Callback.ResponseData.Cookie();
      responseData.cookies[0].key = "session";
      responseData.cookies[0].expiration = 0;
      return responseData;
    }
  }
}