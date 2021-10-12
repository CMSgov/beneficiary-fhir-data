package gov.cms.bfd.sharedutils.generators.token.pattern;

import gov.cms.bfd.sharedutils.generators.exceptions.GeneratorException;
import gov.cms.bfd.sharedutils.generators.exceptions.ParsingException;
import java.math.BigInteger;
import java.util.Random;
import lombok.Getter;

public abstract class TokenPattern {

  @Getter private BigInteger totalPermutations;

  public void init() {
    totalPermutations = calculatePermutations();
  }

  public String createToken() {
    BigInteger randomNumber;

    do {
      randomNumber = new BigInteger(totalPermutations.bitLength(), new Random());
    } while (randomNumber.compareTo(totalPermutations) >= 0);

    return createToken(randomNumber);
  }

  public String createToken(long seed) {
    return createToken(new BigInteger(String.valueOf(seed)));
  }

  public String createToken(BigInteger seed) {
    try {
      if (seed.compareTo(BigInteger.ZERO) < 0) {
        throw new GeneratorException("Seed value can not be negative.");
      } else if (totalPermutations.compareTo(seed) <= 0) {
        throw new GeneratorException("Seed value is too high for the given generator");
      }

      return generateToken(seed);
    } catch (GeneratorException e) {
      // This will bubble all the way up so we can check what seed value caused failure and
      // investigate.
      throw new GeneratorException("Failed with seed: " + seed, e);
    }
  }

  public String convertToken(String token, TokenPattern basePattern) {
    BigInteger value = basePattern.parseTokenValue(token);

    if (totalPermutations.compareTo(value) < 0) {
      throw new ParsingException("Target pattern does not have enough permutations to convert.");
    }

    return createToken(value);
  }

  public BigInteger parseTokenValue(String tokenValue) {
    if (tokenValue.length() != tokenLength()) {
      throw new ParsingException("Given token value is invalid for this pattern");
    }

    return calculateTokenValue(tokenValue);
  }

  public abstract boolean isValidPattern(String value);

  abstract String generateToken(BigInteger seed);

  abstract BigInteger calculateTokenValue(String tokenString);

  abstract int tokenLength();

  abstract BigInteger calculatePermutations();
}
