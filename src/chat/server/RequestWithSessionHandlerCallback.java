package chat.server;

interface RequestWithSessionHandlerCallback {
  public ResponseData call(RequestWithSessionData requestData);
}