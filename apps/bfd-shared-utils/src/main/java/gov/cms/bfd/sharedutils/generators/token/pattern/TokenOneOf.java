package gov.cms.bfd.sharedutils.generators.token.pattern;

import gov.cms.bfd.sharedutils.generators.exceptions.GeneratorException;
import gov.cms.bfd.sharedutils.generators.exceptions.ParsingException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import lombok.EqualsAndHashCode;

/**
 * This class holds tokens of an "or" group, meaning one of the stored token patterns will be used.
 *
 * <p>The order in which the tokens are used is defined by the order in which they were declared by
 * the user in their token pattern string.
 *
 * <p>Example or group: [ABF0-9]. This would allow A, B, F, or any digit to be chosen, in that
 * order.
 */
@EqualsAndHashCode(callSuper = true)
public class TokenOneOf extends TokenPattern {

  private final List<TokenPattern> patterns;

  public TokenOneOf(List<TokenPattern> tokens) {
    patterns = tokens;
    Set<TokenPattern> uniqueTokens = new HashSet<>();

    for (TokenPattern newToken : patterns) {
      // Make sure there are no duplicates
      if (uniqueTokens.contains(newToken)) {
        throw new ParsingException("Duplicate 'or' type token pattern found");
      } else {
        // Even if not a duplicate, make sure it doesn't overlap with other patterns
        for (TokenPattern storedToken : uniqueTokens) {
          if (newToken.overlaps(storedToken)) {
            throw new ParsingException("Token pattern overlap found");
          }
        }
        uniqueTokens.add(newToken);
      }
    }
  }

  @Override
  public boolean isValidPattern(String value) {
    boolean isValid = false;

    for (int i = 0; !isValid && i < patterns.size(); ++i) {
      isValid = patterns.get(i).isValidPattern(value);
    }

    return isValid;
  }

  @Override
  String generateToken(BigInteger seed) {
    String token = "";
    Iterator<TokenPattern> iterator = patterns.iterator();

    BigInteger currentSeed = seed;

    while (token.isEmpty() && iterator.hasNext()) {
      TokenPattern pattern = iterator.next();

      if (currentSeed.compareTo(pattern.getTotalPermutations()) < 0) {
        token = pattern.createToken(currentSeed);
      } else {
        currentSeed = currentSeed.subtract(pattern.getTotalPermutations());
      }
    }

    if (currentSeed.compareTo(BigInteger.ZERO) < 0) {
      throw new GeneratorException(
          "This shouldn't have happened, seed size exceeded permutations, seed value: " + seed);
    }

    return token;
  }

  @Override
  BigInteger calculateTokenValue(String tokenString) {
    BigInteger tokenValue = BigInteger.ZERO;

    for (int i = 0; tokenValue.compareTo(BigInteger.ZERO) == 0 && i < patterns.size(); ++i) {
      TokenPattern tokenPattern = patterns.get(i);

      if (tokenPattern.isValidPattern(tokenString)) {
        tokenValue = tokenValue.add(tokenPattern.parseTokenValue(tokenString));
      } else {
        tokenValue = tokenValue.add(tokenPattern.getTotalPermutations());
      }
    }

    return tokenValue;
  }

  @Override
  int tokenLength() {
    return patterns.iterator().next().tokenLength();
  }

  @Override
  BigInteger calculatePermutations() {
    BigInteger permutations = BigInteger.ZERO;

    for (TokenPattern pattern : patterns) {
      permutations = permutations.add(pattern.getTotalPermutations());
    }

    return permutations;
  }

  @Override
  boolean containsAnyOf(Set<Character> chars) {
    return patterns.stream().anyMatch(t -> t.containsAnyOf(chars));
  }

  @Override
  Set<Character> characters() {
    Set<Character> characters = new HashSet<>();

    patterns.forEach(t -> characters.addAll(t.characters()));

    return characters;
  }
}
