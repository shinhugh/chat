package chat.server;

interface RequestHandlerCallback {
  public ResponseData call(RequestData requestData);
}