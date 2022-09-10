package chat.server;

interface RequestHandlerCallback {
  public ResponseData call(RequestData requestData);

  public static class RequestData {
    public String sessionToken;
    public String contentType;
    public String body;
  }

  public static class ResponseData {
    public int statusCode;
    public Cookie[] cookies;
    public String contentType;
    public String body;

    public static class Cookie {
      public String key;
      public String value;
      public long expiration;
    }
  }
}