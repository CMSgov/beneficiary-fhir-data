package gov.cms.bfd.common.generators.token.pattern;

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
  String generateToken(long seed) {
    StringBuilder token = new StringBuilder();

    long remainingSeed = seed;

    for (int i = 0; i < repeats; ++i) {
      long nextSeed = remainingSeed % pattern.getTotalPermutations();
      remainingSeed /= pattern.getTotalPermutations();
      // Building string backwards to optimize arithmetic
      token.insert(0, pattern.createToken(nextSeed));
    }

    return token.toString();
  }

  @Override
  long calculateTokenValue(String tokenString) {
    long tokenValue = 0;
    int tokenLength = pattern.tokenLength();
    long permutations = pattern.getTotalPermutations();

    for (int i = 0; i < repeats; ++i) {
      int startIndex = i * tokenLength;

      String substring = tokenString.substring(startIndex, startIndex + tokenLength);
      tokenValue *= permutations;
      tokenValue += pattern.parseTokenValue(substring);
    }

    return tokenValue;
  }

  @Override
  int tokenLength() {
    return pattern.tokenLength() * repeats;
  }

  @Override
  long calculatePermutations() {
    return (long) Math.pow(pattern.getTotalPermutations(), repeats);
  }
}
