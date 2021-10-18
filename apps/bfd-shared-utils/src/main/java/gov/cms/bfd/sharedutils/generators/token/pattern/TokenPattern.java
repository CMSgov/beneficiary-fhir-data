package gov.cms.bfd.sharedutils.generators.token.pattern;

import gov.cms.bfd.sharedutils.generators.exceptions.GeneratorException;
import gov.cms.bfd.sharedutils.generators.exceptions.ParsingException;
import java.math.BigInteger;
import java.util.Random;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** The base class for all {@link TokenPattern} implementations. */
@EqualsAndHashCode
public abstract class TokenPattern {

  /** We store this value for optimization. */
  @Getter private BigInteger totalPermutations;

  /**
   * Performs any initializations the class object needs, such as calculating the total permutations
   * for more optimized use in later logic.
   */
  public void init() {
    totalPermutations = calculatePermutations();
  }

  /**
   * Creates a random valid token that falls anywhere in the range of total permutations for it's
   * currently stored pattern.
   *
   * @return A randomly generated token that is valid for the currently stored pattern.
   */
  public String createToken() {
    BigInteger randomNumber;

    do {
      randomNumber = new BigInteger(totalPermutations.bitLength(), new Random());
    } while (randomNumber.compareTo(totalPermutations) >= 0);

    return createToken(randomNumber);
  }

  /**
   * Create a token from the given seed.
   *
   * <p>This acts as a single faster function for generating a token with a seed that falls within a
   * long value
   *
   * @param seed The seed value to generate a token with.
   * @return The token generated from the given seed.
   */
  public String createToken(long seed) {
    return createToken(new BigInteger(String.valueOf(seed)));
  }

  /**
   * Create a token from the given seed.
   *
   * @param seed The {@link BigInteger} seed value to generate a token with.
   * @return The token generated from the given seed.
   */
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

  /**
   * Checks if the given {@link TokenPattern} "overlaps" with the current one.
   *
   * <p>Example: If one pattern is A-M and the other is K-R, the patterns overlap on K-M
   *
   * <p>This check is used to prevent duplicate entries in 'or' groups
   *
   * @param tokenPattern The target {@link TokenPattern} to check for overlaps with the current one.
   * @return True if the patterns overlap, False otherwise.
   */
  public boolean overlaps(TokenPattern tokenPattern) {
    return tokenPattern.containsAnyOf(this.characters());
  }

  /**
   * Converts the given token, using the given base pattern, into a value using the currently stored
   * pattern.
   *
   * <p>This is good if you have a given pattern (i.e. -00032) that you want to quickly convert to
   * another.
   *
   * <p>You can think of this as similar to converting base values (i.e. 1001 (base2) to 9 (base 10)
   *
   * @param token The base value to convert.
   * @param basePattern The pattern of the base value being converted.
   * @return A token based off the given base value, but using the current sequence pattern.
   */
  public String convertToken(String token, TokenPattern basePattern) {
    BigInteger value = basePattern.parseTokenValue(token);

    if (totalPermutations.compareTo(value) < 0) {
      throw new ParsingException("Target pattern does not have enough permutations to convert.");
    }

    return createToken(value);
  }

  /**
   * Gets the backing numeric value that the given token represents for the current token pattern.
   *
   * @param tokenValue The token to parse.
   * @return The backing numeric value for the provided token using the currently stored pattern.
   */
  public BigInteger parseTokenValue(String tokenValue) {
    if (tokenValue.length() != tokenLength()) {
      throw new ParsingException("Given token value is invalid for this pattern");
    }

    return calculateTokenValue(tokenValue);
  }

  /**
   * Determines if the given token value is valid for the currently stored pattern.
   *
   * <p>This is implemented by the child class.
   *
   * @param value The token to be checked.
   * @return True if the given token value is valid for the currently stored pattern.
   */
  public abstract boolean isValidPattern(String value);

  /**
   * Generates a token for the given {@link BigInteger} seed value.
   *
   * <p>This is implemented by the child class.
   *
   * @param seed The {@link BigInteger} seed value to generate a token for.
   * @return The generated token that represents the given seed for the currently stored pattern.
   */
  abstract String generateToken(BigInteger seed);

  /**
   * Calculates the {@link BigInteger} backing value of the given token using the currently stored
   * token pattern.
   *
   * <p>This is implemented by the child class.
   *
   * @param tokenString The token value to parse.
   * @return The {@link BigInteger} backing value that is represented by the given token value for
   *     the currently stored token pattern.
   */
  abstract BigInteger calculateTokenValue(String tokenString);

  /**
   * The length (in characters) of the current {@link TokenPattern}.
   *
   * <p>This is used for various validation checks and permutation calculations.
   *
   * <p>This is implemented by the child class.
   *
   * @return The number of characters the currently stored pattern supports.
   */
  abstract int tokenLength();

  /**
   * Calculates the total number of permutations for the currently stored token pattern.
   *
   * <p>This helps with later validation checks and logic. This value should be calculated via an
   * {@link #init()} call to improve performance.
   *
   * <p>This is implemented by the child class.
   *
   * @return The total number of permutations for the currently stored token pattern.
   */
  abstract BigInteger calculatePermutations();

  /**
   * Checks if the currently stored token pattern contains any of the given characters.
   *
   * <p>This is used to help with "overlap" checks.
   *
   * <p>This is implemented by the child class.
   *
   * @param chars The characters to check for overlap with the currently stored token pattern.
   * @return True if at least one of the given characters is in the currently stored token pattern,
   *     meaning they overlap.
   */
  abstract boolean containsAnyOf(Set<Character> chars);

  /**
   * Gets a {@link Set} of the currently stored token pattern's possible characters.
   *
   * <p>This is used in conjunction with {@link #containsAnyOf(Set)} to check for overlaps.
   *
   * <p>This is implemented by the child class.
   *
   * @return The {@link Set} of characters in the currently stored token pattern.
   */
  abstract Set<Character> characters();
}
