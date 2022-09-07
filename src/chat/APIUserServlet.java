package chat;

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
        User user = PersistentModel.shared.getUserById(requestData.userId);
        UserName userName = new UserName();
        userName.name = user.name;
        Gson gson = new Gson();
        String userNameJson = gson.toJson(userName);
        responseData.statusCode = 200;
        responseData.contentType = "application/json";
        responseData.body = userNameJson;
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
        User user = PersistentModel.shared.getUserByName(credentials.name);
        if (user != null) {
          responseData.statusCode = 400;
          return responseData;
        }
        String salt = Utilities.generateRandomString((short) 16);
        String pwSaltHash = Utilities.generateHash(credentials.pw, salt);
        user = new User();
        user.name = credentials.name;
        user.pw = pwSaltHash;
        user.salt = salt;
        if (!PersistentModel.shared.createUser(user)) {
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
        User user = PersistentModel.shared.getUserById(requestData.userId);
        if (Utilities.nullOrEmpty(credentials.pw)) {
          user.name = credentials.name;
        } else if (Utilities.nullOrEmpty(credentials.name)) {
          String pwSaltHash = Utilities.generateHash(credentials.pw, user.salt);
          user.pw = pwSaltHash;
        } else {
          String pwSaltHash = Utilities.generateHash(credentials.pw, user.salt);
          user.name = credentials.name;
          user.pw = pwSaltHash;
        }
        if (!PersistentModel.shared.updateUser(user)) {
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
        if (!PersistentModel.shared.deleteUserById(requestData.userId)) {
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

  private class UserName {
    public String name;
  }
}