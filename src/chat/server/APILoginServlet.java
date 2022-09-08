package chat.server;

import chat.app.*;
import chat.app.structs.*;
import com.google.gson.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.*;

public class APILoginServlet extends HttpServlet {
  public void doPost(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException
  {
    RequestHandler.handleRequest(request, response,
    new PostRequestHandlerCallback());
  }

  public void doDelete(HttpServletRequest request, HttpServletResponse response)
  throws IOException, ServletException
  {
    RequestHandler.handleRequestWithSession(request, response,
    new DeleteRequestHandlerCallback());
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
        Session session = App.shared.logIn(credentials);
        if (session == null) {
          responseData.statusCode = 403;
          return responseData;
        }
        responseData.statusCode = 200;
        responseData.cookies = new HashMap<String, Map.Entry<String, Long>>();
        responseData.cookies.put("session",
        new AbstractMap.SimpleEntry<String, Long>(session.token,
        session.expiration));
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
        if (!App.shared.logOut(requestData.sessionId)) {
          responseData.statusCode = 400;
          return responseData;
        }
        responseData.statusCode = 200;
        responseData.cookies = new HashMap<String, Map.Entry<String, Long>>();
        responseData.cookies.put("session",
        new AbstractMap.SimpleEntry<String, Long>(null, (long) 0));
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