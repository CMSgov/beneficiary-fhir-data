package gov.cms.bfd.common.generators.token.pattern;

import java.math.BigInteger;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TokenSingleton extends TokenPattern {

  private final char token;

  @Override
  public boolean isValidPattern(String value) {
    return value.length() == 1 && value.charAt(0) == token;
  }

  @Override
  String generateToken(BigInteger seed) {
    return String.valueOf(token);
  }

  @Override
  BigInteger calculateTokenValue(String tokenString) {
    return BigInteger.ZERO;
  }

  @Override
  int tokenLength() {
    return 1;
  }

  @Override
  BigInteger calculatePermutations() {
    return BigInteger.ONE;
  }
}
