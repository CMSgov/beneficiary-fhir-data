package gov.cms.bfd.common.generators.token.pattern;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TokenRange extends TokenPattern {

  private final char lowerBound;
  private final char upperBound;

  @Override
  String generateToken(long seed) {
    return String.valueOf((char) (lowerBound + seed));
  }

  @Override
  long calculatePermutations() {
    return upperBound - lowerBound + 1L;
  }
}
