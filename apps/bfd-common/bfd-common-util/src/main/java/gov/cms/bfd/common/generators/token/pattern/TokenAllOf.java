package gov.cms.bfd.common.generators.token.pattern;

import gov.cms.bfd.common.exceptions.GeneratorException;
import java.math.BigInteger;
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
  String generateToken(BigInteger seed) {
    StringBuilder token = new StringBuilder();
    BigInteger remainingSeed = seed;

    // Start iterator at the end so we can search backwards (least significant value first)
    ListIterator<TokenPattern> iterator = patterns.listIterator(patterns.size());

    while (iterator.hasPrevious()) {
      TokenPattern pattern = iterator.previous();

      BigInteger currentSeed = remainingSeed.mod(pattern.getTotalPermutations());
      remainingSeed = remainingSeed.divide(pattern.getTotalPermutations());
      // Build string backwards, least significant value first.
      token.insert(0, pattern.generateToken(currentSeed));
    }

    if (remainingSeed.compareTo(BigInteger.ZERO) < 0) {
      throw new GeneratorException(
          "This shouldn't have happened, seed size exceeded permutations, seed value: " + seed);
    }

    return token.toString();
  }

  @Override
  BigInteger calculateTokenValue(String tokenString) {
    BigInteger tokenValue = BigInteger.ZERO;

    int startIndex = 0;

    for (TokenPattern tokenPattern : patterns) {
      String substring = tokenString.substring(startIndex, startIndex + tokenPattern.tokenLength());
      tokenValue = tokenValue.multiply(tokenPattern.getTotalPermutations());
      tokenValue = tokenValue.add(tokenPattern.parseTokenValue(substring));

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
  BigInteger calculatePermutations() {
    BigInteger permutations = patterns.isEmpty() ? BigInteger.ZERO : BigInteger.ONE;

    for (TokenPattern pattern : patterns) {
      permutations = permutations.multiply(pattern.getTotalPermutations());
    }

    return permutations;
  }
}
