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
  long calculatePermutations() {
    long permutations = 0;

    for (TokenPattern pattern : orTokens) {
      permutations += pattern.getTotalPermutations();
    }

    return permutations;
  }
}
