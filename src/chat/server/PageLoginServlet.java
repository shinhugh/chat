package chat.server;

import chat.app.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.file.*;

public class PageLoginServlet extends HttpServlet {
  public void doGet(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException {
    RequestHandler.handleRequest(request, response,
    new GetRequestHandlerCallback());
  }

  private static class GetRequestHandlerCallback
  implements RequestHandler.Callback {
    private static final String resourcePath = "/public/login/index.html";
    private static final String redirectLocation = "/chat";

    public RequestHandler.Callback.ResponseData call(
    RequestHandler.Callback.RequestData requestData) {
      RequestHandler.Callback.ResponseData responseData = new ResponseData();
      App.Result<Object> result = App.shared
      .verifySessionToken(requestData.sessionToken);
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
      StringBuilder sb = new StringBuilder();
      String path = System.getProperty("catalina.base") + "/webapps/ROOT"
      + resourcePath;
      try {
        BufferedReader fileReader = Files.newBufferedReader(Paths.get(path));
        String line = fileReader.readLine();
        while (line != null) {
          sb.append(line);
          sb.append('\n');
          line = fileReader.readLine();
        }
      } catch (IOException error) {
        responseData.statusCode = 500;
        return responseData;
      }
      if (sb.length() > 0) {
        sb.deleteCharAt(sb.length() - 1);
        body = sb.toString();
      }
      responseData.statusCode = 200;
      responseData.contentType = "text/html";
      responseData.body = body;
      return responseData;
    }
  }
}