package chat.server;

import chat.app.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.file.*;

public class PageRootServlet extends HttpServlet {
  public void doGet(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException {
    RequestHandler.handleRequest(request, response,
    new GetRequestHandlerCallback());
  }

  private static class GetRequestHandlerCallback
  implements RequestHandler.Callback {
    private static final String resourcePath = "/public/index.html";
    private static final String redirectLocation = "/login";

    public RequestHandler.Callback.ResponseData call(
    RequestHandler.Callback.RequestData requestData) {
      RequestHandler.Callback.ResponseData responseData = new ResponseData();
      App.Result<Object> result = App.shared
      .verifySessionToken(requestData.sessionToken);
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