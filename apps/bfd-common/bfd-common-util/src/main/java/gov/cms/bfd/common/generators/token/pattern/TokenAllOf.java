package gov.cms.bfd.common.generators.token.pattern;

import gov.cms.bfd.common.exceptions.GeneratorException;
import java.util.List;
import java.util.ListIterator;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TokenAllOf extends TokenPattern {

  private final List<TokenPattern> patterns;

  @Override
  public boolean isValidPattern(String value) {
    boolean isValid = true;

    int startIndex = 0;

    for (int i = 0; isValid && i < patterns.size(); ++i) {
      TokenPattern tokenPattern = patterns.get(i);

      String substring = value.substring(startIndex, startIndex + tokenPattern.tokenLength());
      isValid = tokenPattern.isValidPattern(substring);
      startIndex += tokenPattern.tokenLength();
    }

    return isValid;
  }

  @Override
  String generateToken(long seed) {
    StringBuilder token = new StringBuilder();
    long remainingSeed = seed;

    // Start iterator at the end so we can search backwards (least significant value first)
    ListIterator<TokenPattern> iterator = patterns.listIterator(patterns.size());

    while (iterator.hasPrevious()) {
      TokenPattern pattern = iterator.previous();

      long currentSeed = remainingSeed % pattern.getTotalPermutations();
      remainingSeed /= pattern.getTotalPermutations();
      // Build string backwards, least significant value first.
      token.insert(0, pattern.generateToken(currentSeed));
    }

    if (remainingSeed < 0) {
      throw new GeneratorException(
          "This shouldn't have happened, seed size exceeded permutations, seed value: " + seed);
    }

    return token.toString();
  }

  @Override
  long calculateTokenValue(String tokenString) {
    long tokenValue = 0;

    int startIndex = 0;

    for (TokenPattern tokenPattern : patterns) {
      String substring = tokenString.substring(startIndex, startIndex + tokenPattern.tokenLength());
      tokenValue *= tokenPattern.getTotalPermutations();
      tokenValue += tokenPattern.parseTokenValue(substring);

      // Prep for next loop
      startIndex += tokenPattern.tokenLength();
    }

    return tokenValue;
  }

  // OptionalGetWithoutIsPresent - TokenPattern#tokenLength can't be null.
  @SuppressWarnings({"OptionalGetWithoutIsPresent", "squid:S3655"})
  @Override
  int tokenLength() {
    return patterns.stream().map(TokenPattern::tokenLength).reduce(Integer::sum).get();
  }

  @Override
  long calculatePermutations() {
    // TODO: Need to implement some overflow protection
    long permutations = patterns.isEmpty() ? 0 : 1;

    for (TokenPattern pattern : patterns) {
      permutations *= pattern.getTotalPermutations();
    }

    return permutations;
  }
}
