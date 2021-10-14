package gov.cms.bfd.sharedutils.generators.token;

import gov.cms.bfd.sharedutils.generators.token.parser.TokenAllOfParser;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenPattern;
import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 * Class used to parse a given pattern and create a {@link TokenPattern} to be used for
 * parsing/generating tokens of that pattern.
 */
public class TokenPatternParser {

  /**
   * Generates a {@link TokenPattern} based off the given pattern.
   *
   * @param pattern The pattern to create a {@link TokenPattern} for.
   * @return The {@link TokenPattern} created from the given pattern.
   */
  // S1612 - Can't cast using char.class::cast
  @SuppressWarnings("squid:S1612")
  public TokenPattern parse(String pattern) {
    Queue<Character> tokens =
        pattern.chars().mapToObj(c -> (char) c).collect(Collectors.toCollection(LinkedList::new));

    return new TokenAllOfParser().parse(tokens);
  }
}
