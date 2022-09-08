package chat.server;

import chat.server.structs.*;

interface RequestWithSessionHandlerCallback {
  public ResponseData call(RequestWithSessionData requestData);
}