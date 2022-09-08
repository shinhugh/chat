package chat;

import java.nio.charset.*;
import java.security.*;
import java.util.*;

public class Utilities {
  private static final byte[] hexPool = "0123456789abcdef"
  .getBytes(StandardCharsets.US_ASCII);

  public static boolean nullOrEmpty(String str) {
    return str == null || "".equals(str);
  }

  public static String generateRandomString(int length) {
    if (length < 1) {
      return "";
    }
    String pool
    = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890";
    StringBuilder builder = new StringBuilder();
    Random random = new Random();
    while (builder.length() < length) {
      short index = (short) (random.nextFloat() * pool.length());
      builder.append(pool.charAt(index));
    }
    return builder.toString();
  }

  public static String generateHash(String pw, String salt) {
    MessageDigest digest = null;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException error) {
      return null;
    }
    byte[] bytes = digest.digest((pw + salt).getBytes(StandardCharsets.UTF_8));
    return Utilities.hexString(bytes);
  }

  public static String hexString(byte[] bytes) {
    byte[] builder = new byte[bytes.length * 2];
    for (int i = 0; i < bytes.length; i++) {
      int b = Byte.toUnsignedInt(bytes[i]);
      builder[i * 2] = hexPool[b >>> 4];
      builder[i * 2 + 1] = hexPool[b & 0x0F];
    }
    return new String(builder, StandardCharsets.UTF_8);
  }

  private Utilities() { }
}