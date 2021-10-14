package gov.cms.bfd.sharedutils.generators.token.pattern;

import gov.cms.bfd.sharedutils.generators.exceptions.GeneratorException;
import gov.cms.bfd.sharedutils.generators.exceptions.ParsingException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class TokenOneOf extends TokenPattern {

  private final List<TokenPattern> patternOrder;
  private final Set<TokenPattern> orTokens = new HashSet<>();

  public TokenOneOf(List<TokenPattern> tokens) {
    patternOrder = tokens;

    // TODO: This could use more rigorous range overlap checks
    for (TokenPattern token : patternOrder) {
      // Make sure there are no duplicates
      if (orTokens.contains(token)) {
        throw new ParsingException("Duplicate 'or' type token pattern found");
      } else {
        orTokens.add(token);
      }
    }
  }

  @Override
  public boolean isValidPattern(String value) {
    boolean isValid = false;

    for (int i = 0; !isValid && i < patternOrder.size(); ++i) {
      isValid = patternOrder.get(i).isValidPattern(value);
    }

    return isValid;
  }

  @Override
  String generateToken(BigInteger seed) {
    String token = "";
    Iterator<TokenPattern> iterator = patternOrder.iterator();

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

    for (int i = 0; tokenValue.compareTo(BigInteger.ZERO) == 0 && i < patternOrder.size(); ++i) {
      TokenPattern tokenPattern = patternOrder.get(i);

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
    return orTokens.iterator().next().tokenLength();
  }

  @Override
  BigInteger calculatePermutations() {
    BigInteger permutations = BigInteger.ZERO;

    for (TokenPattern pattern : orTokens) {
      permutations = permutations.add(pattern.getTotalPermutations());
    }

    return permutations;
  }
}
