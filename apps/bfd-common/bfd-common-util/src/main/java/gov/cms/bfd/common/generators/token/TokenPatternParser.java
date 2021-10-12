package gov.cms.bfd.common.generators.token;

import gov.cms.bfd.common.generators.token.parser.TokenAllOfParser;
import gov.cms.bfd.common.generators.token.pattern.TokenPattern;
import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Collectors;

public class TokenPatternParser {

  // S1612 - Can't cast using char.class::cast
  @SuppressWarnings("squid:S1612")
  public TokenPattern parse(String pattern) {
    Queue<Character> tokens =
        pattern.chars().mapToObj(c -> (char) c).collect(Collectors.toCollection(LinkedList::new));

    return new TokenAllOfParser().parse(tokens);
  }
}
