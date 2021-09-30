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
  long calculatePermutations() {
    return (long) Math.pow(pattern.getTotalPermutations(), repeats);
  }
}
