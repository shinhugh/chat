package chat;

import java.util.*;

class ResponseData {
  public int statusCode;
  public Map<String, Map.Entry<String, Long>> cookies;
  public String contentType;
  public String body;
}