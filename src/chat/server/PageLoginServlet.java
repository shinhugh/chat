package chat.server;

import chat.app.*;
import chat.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;

public class PageLoginServlet extends HttpServlet {
  public void doGet(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException {
    RequestHandler.handleRequest(request, response, new GetRequestHandlerCallback());
  }

  private static class GetRequestHandlerCallback
  implements RequestHandler.Callback {
    private static final String resourcePath = "/login/index.html";
    private static final String contentType = "text/html";
    private static final String redirectLocation = "/";

    public RequestHandler.Callback.ResponseData call(RequestHandler.Callback.RequestData requestData) {
      RequestHandler.Callback.ResponseData responseData = new ResponseData();
      App.Result<Object> result = App.shared.verifySessionToken(requestData.sessionToken);
      if (result.success) {
        responseData.statusCode = 302;
        responseData.location = redirectLocation;
        return responseData;
      }
      if (result.failureReason == App.Result.FailureReason.Unknown) {
        responseData.statusCode = 500;
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