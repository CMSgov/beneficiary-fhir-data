package gov.cms.bfd.sharedutils.generators.token.pattern;

import java.math.BigInteger;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

/** Defines a range of characters that are valid for this particular token character. */
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
  String generateToken(BigInteger seed) {
    return String.valueOf((char) (lowerBound + seed.intValue()));
  }

  @Override
  BigInteger calculateTokenValue(String tokenString) {
    return new BigInteger(String.valueOf(tokenString.charAt(0) - lowerBound));
  }

  @Override
  int tokenLength() {
    return 1;
  }

  @Override
  BigInteger calculatePermutations() {
    return new BigInteger(String.valueOf(upperBound - lowerBound + 1L));
  }

  @Override
  boolean containsAnyOf(Set<Character> chars) {
    for (Character c : chars) {
      if (c >= lowerBound && c <= upperBound) {
        return true;
      }
    }

    return false;
  }

  @Override
  Set<Character> characters() {
    return IntStream.range(lowerBound, upperBound + 1)
        .boxed()
        .map(c -> (char) c.intValue())
        .collect(Collectors.toSet());
  }
}
