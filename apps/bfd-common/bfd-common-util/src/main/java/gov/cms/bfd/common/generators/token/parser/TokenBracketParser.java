package gov.cms.bfd.common.generators.token.parser;

import gov.cms.bfd.common.generators.token.TokenParser;
import gov.cms.bfd.common.generators.token.TokenPattern;
import java.util.LinkedList;
import java.util.Queue;

public class TokenBracketParser implements TokenParser {

  @Override
  public TokenPattern parse(Queue<Character> patternStream) {
    Queue<Character> bracketPatternStream = new LinkedList<>();

    int bracketCount = 1;

    char currentToken = patternStream.remove();

    while ((currentToken != ']' || bracketCount > 1) && !patternStream.isEmpty()) {
      currentToken = patternStream.remove();

      if (currentToken == '[') ++bracketCount;

      if (currentToken != ']' || bracketCount > 1) {
        if (currentToken == ']') --bracketCount;

        bracketPatternStream.add(currentToken);
      }
    }

    if (currentToken != ']') {
      throw new IllegalArgumentException("Unexpected end of bracket group");
    } else if (bracketPatternStream.isEmpty()) {
      throw new IllegalArgumentException("Empty bracket groups are not allowed.");
    }

    return new TokenOneOfParser().parse(bracketPatternStream);
  }
}
