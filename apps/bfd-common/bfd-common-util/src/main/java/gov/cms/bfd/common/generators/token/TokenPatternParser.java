package gov.cms.bfd.common.generators.token;

import gov.cms.bfd.common.generators.token.parser.TokenAllOfParser;
import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Collectors;

public class TokenPatternParser {

  public TokenPattern parse(String pattern) {
    Queue<Character> tokens =
        pattern.chars().mapToObj(c -> (char) c).collect(Collectors.toCollection(LinkedList::new));

    return new TokenAllOfParser().parse(tokens);
  }
}
