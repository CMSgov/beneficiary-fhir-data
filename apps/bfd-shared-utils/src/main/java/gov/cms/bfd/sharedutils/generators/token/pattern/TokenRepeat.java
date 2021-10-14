package gov.cms.bfd.sharedutils.generators.token.pattern;

import java.math.BigInteger;
import java.util.Set;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class TokenRepeat extends TokenPattern {

  private TokenPattern pattern;
  private final int repeats;

  public TokenRepeat(int repeats) {
    this.repeats = repeats;
  }

  public TokenRepeat(TokenPattern pattern, TokenRepeat oldRepeat) {
    this.pattern = pattern;
    this.repeats = oldRepeat.repeats;
  }

  @Override
  public boolean isValidPattern(String value) {
    boolean isValid = true;
    int tokenLength = pattern.tokenLength();

    for (int i = 0; isValid && i < repeats; ++i) {
      int startIndex = i * tokenLength;

      String substring = value.substring(startIndex, startIndex + tokenLength);
      isValid = pattern.isValidPattern(substring);
    }

    return isValid;
  }

  @Override
  String generateToken(BigInteger seed) {
    StringBuilder token = new StringBuilder();

    BigInteger remainingSeed = seed;

    for (int i = 0; i < repeats; ++i) {
      BigInteger nextSeed = remainingSeed.mod(pattern.getTotalPermutations());
      remainingSeed = remainingSeed.divide(pattern.getTotalPermutations());
      // Building string backwards to optimize arithmetic
      token.insert(0, pattern.createToken(nextSeed));
    }

    return token.toString();
  }

  @Override
  BigInteger calculateTokenValue(String tokenString) {
    BigInteger tokenValue = BigInteger.ZERO;
    int tokenLength = pattern.tokenLength();
    BigInteger permutations = pattern.getTotalPermutations();

    for (int i = 0; i < repeats; ++i) {
      int startIndex = i * tokenLength;

      String substring = tokenString.substring(startIndex, startIndex + tokenLength);
      tokenValue = tokenValue.multiply(permutations);
      tokenValue = tokenValue.add(pattern.parseTokenValue(substring));
    }

    return tokenValue;
  }

  @Override
  int tokenLength() {
    return pattern.tokenLength() * repeats;
  }

  @Override
  BigInteger calculatePermutations() {
    return pattern.getTotalPermutations().pow(repeats);
  }

  @Override
  boolean containsAnyOf(Set<Character> chars) {
    return pattern.containsAnyOf(chars);
  }

  @Override
  Set<Character> characters() {
    return pattern.characters();
  }
}
