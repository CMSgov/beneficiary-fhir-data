package gov.cms.bfd.common.generators.token.pattern;

import gov.cms.bfd.common.exceptions.GeneratorException;
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
  String generateToken(long seed) {
    String token = "";
    Iterator<TokenPattern> iterator = patternOrder.iterator();

    long currentSeed = seed;

    while (token.isEmpty() && iterator.hasNext()) {
      TokenPattern pattern = iterator.next();

      if (pattern.getTotalPermutations() > currentSeed) {
        token = pattern.createToken(currentSeed);
      } else {
        currentSeed -= pattern.getTotalPermutations();
      }
    }

    if (currentSeed < 0) {
      throw new GeneratorException(
          "This shouldn't have happened, seed size exceeded permutations, seed value: " + seed);
    }

    return token;
  }

  @Override
  long calculateTokenValue(String tokenString) {
    long tokenValue = 0;

    for (int i = 0; tokenValue == 0 && i < patternOrder.size(); ++i) {
      TokenPattern tokenPattern = patternOrder.get(i);

      if (tokenPattern.isValidPattern(tokenString)) {
        tokenValue += tokenPattern.parseTokenValue(tokenString);
      } else {
        tokenValue += tokenPattern.getTotalPermutations();
      }
    }

    return tokenValue;
  }

  @Override
  int tokenLength() {
    return orTokens.iterator().next().tokenLength();
  }

  @Override
  long calculatePermutations() {
    long permutations = 0;

    for (TokenPattern pattern : orTokens) {
      permutations += pattern.getTotalPermutations();
    }

    return permutations;
  }
}
