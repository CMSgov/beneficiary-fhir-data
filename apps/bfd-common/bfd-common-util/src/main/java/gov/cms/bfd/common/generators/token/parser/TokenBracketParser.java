package gov.cms.bfd.common.generators.token.parser;

import gov.cms.bfd.common.exceptions.ParsingException;
import gov.cms.bfd.common.generators.token.pattern.TokenPattern;
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
      throw new ParsingException("Unexpected end of bracket group");
    } else if (bracketPatternStream.isEmpty()) {
      throw new ParsingException("Empty bracket groups are not allowed.");
    }

    return new TokenOneOfParser().parse(bracketPatternStream);
  }
}
