package gov.cms.bfd.sharedutils.generators.token.parser;

import com.google.common.base.Objects;
import gov.cms.bfd.sharedutils.generators.exceptions.ParsingException;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenPattern;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenRepeat;
import java.util.Queue;

/**
 * This class parses repeated {@link TokenPattern}s, carrying overflow to the next place value.
 *
 * <p>Example: A-F{3} will define a pattern of 3 letters, each between A and F (Inclusive)
 *
 * <p>The token patterns are a subset of Regular Expressions, and to avoid ambiguity in the parsing
 * of token values, they are required to be a fixed length. For this reason, using variable ranges
 * ({1,3}) is not allowed.
 */
public class TokenRepeatParser implements TokenParser {

  @Override
  public TokenPattern parse(Queue<Character> patternStream) {
    // Toss the opening curly bracket
    patternStream.remove();

    long repeats;

    if (!patternStream.isEmpty()) {
      char currentToken = patternStream.remove();

      if (Character.isDigit(currentToken) && currentToken != '0') {
        repeats = (currentToken - '0');

        while (!patternStream.isEmpty() && Character.isDigit(patternStream.peek())) {
          currentToken = patternStream.remove();
          repeats = (repeats * 10) + (currentToken - '0');

          if (repeats > Integer.MAX_VALUE) {
            throw new ParsingException("Repeat value too large");
          }
        }
      } else {
        throw new ParsingException("Illegal value '" + currentToken + "' for repeat count");
      }
    } else {
      throw new ParsingException("Unexpected end of repeat definition");
    }

    if (!Objects.equal(patternStream.peek(), '}')) {
      throw new ParsingException("Expected closing repeat bracket not found.");
    }

    // Toss the closing curly bracket
    patternStream.remove();

    // This TokenPattern will get initialized (init()) later
    return new TokenRepeat((int) repeats);
  }
}
