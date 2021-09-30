package gov.cms.bfd.common.generators.token.pattern;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TokenSingleton extends TokenPattern {

  private final char token;

  @Override
  String generateToken(long seed) {
    return String.valueOf(token);
  }

  @Override
  long calculatePermutations() {
    return 1;
  }
}
