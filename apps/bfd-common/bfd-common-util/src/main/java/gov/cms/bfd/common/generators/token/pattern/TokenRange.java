package gov.cms.bfd.common.generators.token.pattern;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TokenRange extends TokenPattern {

  private final char lowerBound;
  private final char upperBound;

  @Override
  public boolean isValidPattern(String value) {
    return value.length() == 1 && value.charAt(0) >= lowerBound && value.charAt(0) <= upperBound;
  }

  @Override
  String generateToken(long seed) {
    return String.valueOf((char) (lowerBound + seed));
  }

  @Override
  long calculateTokenValue(String tokenString) {
    return (tokenString.charAt(0) - lowerBound);
  }

  @Override
  int tokenLength() {
    return 1;
  }

  @Override
  long calculatePermutations() {
    return upperBound - lowerBound + 1L;
  }
}
