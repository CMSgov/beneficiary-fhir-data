package gov.cms.bfd.common.generators.token.pattern;

import gov.cms.bfd.common.exceptions.GeneratorException;
import gov.cms.bfd.common.exceptions.ParsingException;
import lombok.Getter;

public abstract class TokenPattern {

  @Getter private long totalPermutations;

  public void init() {
    totalPermutations = calculatePermutations();
  }

  public String createToken() {
    return createToken((long) (totalPermutations * Math.random()));
  }

  public String createToken(long seed) {
    try {
      if (seed < 0) {
        throw new GeneratorException("Seed value can not be negative.");
      } else if (getTotalPermutations() < seed) {
        throw new GeneratorException("Seed value is too high for the given generator");
      }

      return generateToken(seed);
    } catch (GeneratorException e) {
      // This will bubble all the way up so we can check what seed value caused failure and
      // investigate.
      throw new GeneratorException("Failed with seed: " + seed);
    }
  }

  public String convertToken(String token, TokenPattern basePattern) {
    long value = basePattern.parseTokenValue(token);

    if (value >= totalPermutations) {
      throw new ParsingException("Target pattern does not have enough permutations to convert.");
    }

    return createToken(value);
  }

  public long parseTokenValue(String tokenValue) {
    if (tokenValue.length() != tokenLength()) {
      throw new ParsingException("Given token value is invalid for this pattern");
    }

    return calculateTokenValue(tokenValue);
  }

  public abstract boolean isValidPattern(String value);

  abstract String generateToken(long seed);

  abstract long calculateTokenValue(String tokenString);

  abstract int tokenLength();

  abstract long calculatePermutations();
}
