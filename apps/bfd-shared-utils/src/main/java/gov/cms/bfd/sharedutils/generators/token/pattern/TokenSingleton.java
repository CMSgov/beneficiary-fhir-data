package gov.cms.bfd.sharedutils.generators.token.pattern;

import com.google.common.collect.Sets;
import java.math.BigInteger;
import java.util.Set;
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

  @Override
  boolean containsAnyOf(Set<Character> chars) {
    return chars.contains(token);
  }

  @Override
  Set<Character> characters() {
    return Sets.newHashSet(token);
  }
}
