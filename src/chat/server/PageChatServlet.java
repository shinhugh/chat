package chat.server;

import chat.app.*;
import chat.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;

public class PageChatServlet extends HttpServlet {
  public void doGet(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException {
    RequestHandler.handleRequest(request, response, new GetRequestHandlerCallback());
  }

  private static class GetRequestHandlerCallback
  implements RequestHandler.Callback {
    private static final String resourcePath = "/chat/index.html";
    private static final String contentType = "text/html";
    private static final String redirectLocation = "/login";

    public RequestHandler.Callback.ResponseData call(RequestHandler.Callback.RequestData requestData) {
      RequestHandler.Callback.ResponseData responseData = new ResponseData();
      App.Result<Object> result = App.shared.verifySessionToken(requestData.sessionToken);
      if (!result.success) {
        switch(result.failureReason) {
          case Unauthorized:
            responseData.statusCode = 302;
            responseData.location = redirectLocation;
            break;
          default:
            responseData.statusCode = 500;
            break;
        }
        return responseData;
      }
      String body = null;
      try {
        body = Utilities.readPublicFile(resourcePath);
      } catch (IOException error) {
        responseData.statusCode = 500;
        return responseData;
      }
      responseData.statusCode = 200;
      responseData.contentType = contentType;
      responseData.body = body;
      return responseData;
    }
  }
}