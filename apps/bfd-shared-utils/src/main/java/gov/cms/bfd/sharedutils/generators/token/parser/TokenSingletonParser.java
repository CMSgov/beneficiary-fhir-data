package gov.cms.bfd.sharedutils.generators.token.parser;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import gov.cms.bfd.sharedutils.generators.exceptions.ParsingException;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenPattern;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenRange;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenSingleton;
import java.util.Queue;
import java.util.Set;

public class TokenSingletonParser implements TokenParser {

  private static final Set<Character> VALID_ESCAPE_CHARS =
      ImmutableSet.of('\\', 'd', '[', ']', '{', '}', '-');

  @Override
  public TokenPattern parse(Queue<Character> patternStream) {
    TokenPattern pattern;
    char token = patternStream.remove();

    if (token == '\\') {
      if (!patternStream.isEmpty()) {
        char escapedToken = patternStream.remove();

        if (isValidEscapedCharacter(escapedToken)) {
          if (escapedToken == 'd') {
            pattern = new TokenRange('0', '9');
          } else {
            pattern = new TokenSingleton(escapedToken);
          }
        } else {
          throw new ParsingException("Invalid escaped character '\\" + escapedToken + "'");
        }
      } else {
        throw new ParsingException("Unexpected end of token pattern.");
      }
    } else {
      pattern = new TokenSingleton(token);
    }

    pattern.init();
    return pattern;
  }

  @VisibleForTesting
  boolean isValidEscapedCharacter(char escapedChar) {
    return VALID_ESCAPE_CHARS.contains(escapedChar);
  }
}
