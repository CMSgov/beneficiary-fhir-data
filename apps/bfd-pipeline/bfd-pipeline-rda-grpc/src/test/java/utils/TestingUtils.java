package utils;

import java.util.Base64;

public class TestingUtils {

  public static String createTokenWithExpiration(long expirationDateEpochSeconds) {
    String claimsString = String.format("{\"exp\":%d}", expirationDateEpochSeconds);
    String claimsToken = Base64.getEncoder().encodeToString(claimsString.getBytes());
    return String.format("NotAReal.%s.Token", claimsToken);
  }
}
