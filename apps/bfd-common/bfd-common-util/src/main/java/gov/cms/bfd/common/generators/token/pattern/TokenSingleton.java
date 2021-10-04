package gov.cms.bfd.common.generators.token.pattern;

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
  String generateToken(long seed) {
    return String.valueOf(token);
  }

  @Override
  long calculateTokenValue(String tokenString) {
    return 0;
  }

  @Override
  int tokenLength() {
    return 1;
  }

  @Override
  long calculatePermutations() {
    return 1;
  }
}
