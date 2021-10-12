package gov.cms.bfd.sharedutils.generators.token.pattern;

import gov.cms.bfd.sharedutils.generators.exceptions.GeneratorException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TokenOneOf extends TokenPattern {

  private final Set<TokenPattern> orTokens;
  private final List<TokenPattern> patternOrder = new ArrayList<>();

  public void sortTokens() {
    patternOrder.clear();
    patternOrder.addAll(orTokens);

    // TODO: Fix this priority sort, right now 'A' is before 'a'
    patternOrder.sort(Comparator.comparing(pattern -> pattern.createToken(0)));
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
