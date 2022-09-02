package chat;

import java.util.*;
import java.nio.charset.*;
import java.security.*;

class Utilities {
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

  public static String binaryString(byte input) {
    int inputInt = Byte.toUnsignedInt(input);
    StringBuilder builder = new StringBuilder();
    int mask = 0x00000080;
    for (int i = 0; i < 8; i++) {
      builder.append((inputInt & mask) == 0 ? "0" : "1");
      mask = mask >>> 1;
    }
    return builder.toString();
  }

  public static String binaryString(int input) {
    StringBuilder builder = new StringBuilder();
    int mask = 0x80000000;
    for (int i = 0; i < 32; i++) {
      builder.append((input & mask) == 0 ? "0" : "1");
      mask = mask >>> 1;
    }
    return builder.toString();
  }

  private Utilities() { }

  private static final byte[] hexPool
  = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
}