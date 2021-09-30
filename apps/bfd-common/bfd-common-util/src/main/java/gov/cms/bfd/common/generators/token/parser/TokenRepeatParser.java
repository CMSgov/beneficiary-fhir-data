package gov.cms.bfd.common.generators.token.parser;

import com.google.common.base.Objects;
import gov.cms.bfd.common.generators.token.TokenParser;
import gov.cms.bfd.common.generators.token.TokenPattern;
import gov.cms.bfd.common.generators.token.pattern.TokenRepeat;
import java.util.Queue;

public class TokenRepeatParser implements TokenParser {

  @Override
  public TokenPattern parse(Queue<Character> patternStream) {
    // Toss the opening curly bracket
    patternStream.remove();

    long repeats = 0;

    if (!patternStream.isEmpty()) {
      char currentToken = patternStream.remove();

      if (Character.isDigit(currentToken) && currentToken != '0') {
        repeats += (currentToken - '0');

        while (!patternStream.isEmpty() && Character.isDigit(patternStream.peek())) {
          currentToken = patternStream.remove();
          repeats = (repeats * 10) + (currentToken - '0');

          if (repeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Repeat value too large");
          }
        }
      } else {
        throw new IllegalArgumentException("Illegal value '" + currentToken + "' for repeat count");
      }
    } else {
      throw new IllegalArgumentException("Unexpected end of repeat definition");
    }

    if (!Objects.equal(patternStream.peek(), '}')) {
      throw new IllegalArgumentException("Expected closing repeat bracket not found.");
    }

    // Toss the closing curly bracket
    patternStream.remove();

    return new TokenRepeat(null, (int) repeats);
  }
}
