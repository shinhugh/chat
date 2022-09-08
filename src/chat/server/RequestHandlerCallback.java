package chat.server;

import chat.server.structs.*;

interface RequestHandlerCallback {
  public ResponseData call(RequestData requestData);
}