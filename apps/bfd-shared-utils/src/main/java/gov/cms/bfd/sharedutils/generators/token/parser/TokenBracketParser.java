package gov.cms.bfd.sharedutils.generators.token.parser;

import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.sharedutils.generators.exceptions.ParsingException;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenPattern;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

/**
 * This parser handles bracket groups, which can be square brackets ('or' groups) or curly brackets
 * (repeat groups)
 *
 * <p>This class exists just as a validation check prior to calling {@link TokenOneOfParser} to make
 * sure the group isn't partially formed or otherwise invalid.
 *
 * <p>Example: [AB0-9] defines an 'or' group that can be A, B, or any digit.
 */
public class TokenBracketParser extends TokenParser {

  @Override
  public TokenPattern parse(Queue<Character> patternStream) {
    Deque<Character> bracketPatternStream = new LinkedList<>();

    int bracketCount = 0;

    char currentToken;

    do {
      currentToken = patternStream.remove();

      if (bracketCount > 0) {
        // This will add the trailing ']', but we'll remove it later.
        bracketPatternStream.add(currentToken);
      }

      switch (currentToken) {
        case '[':
          ++bracketCount;
          break;
        case ']':
          --bracketCount;
          break;
        default:
      }
    } while (bracketCount > 0 && !patternStream.isEmpty());

    if (currentToken != ']' || bracketCount > 0) {
      throw new ParsingException("Unexpected end of bracket group");
    } else if (bracketPatternStream.size() < 2) {
      // If there's only 1 element, it's the closing ']'
      throw new ParsingException("Empty bracket groups are not allowed.");
    }

    bracketPatternStream.removeLast(); // Remove the last ']' added.

    return newTokenOneOfParser().parse(bracketPatternStream);
  }

  /**
   * Helper method to make unit testing easier.
   *
   * @return A new instance of a {@link TokenOneOfParser}
   */
  @VisibleForTesting
  TokenOneOfParser newTokenOneOfParser() {
    return new TokenOneOfParser();
  }
}
