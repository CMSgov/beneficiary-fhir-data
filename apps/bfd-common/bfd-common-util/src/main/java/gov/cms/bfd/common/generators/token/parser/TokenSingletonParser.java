package gov.cms.bfd.common.generators.token.parser;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import gov.cms.bfd.common.generators.token.TokenParser;
import gov.cms.bfd.common.generators.token.TokenPattern;
import gov.cms.bfd.common.generators.token.pattern.TokenRange;
import gov.cms.bfd.common.generators.token.pattern.TokenSingleton;
import java.util.Queue;

public class TokenSingletonParser implements TokenParser {

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
          throw new IllegalArgumentException("Invalid escaped character '\\" + escapedToken + "'");
        }
      } else {
        throw new IllegalArgumentException("Unexpected end of token pattern.");
      }
    } else {
      pattern = new TokenSingleton(token);
    }

    return pattern;
  }

  @VisibleForTesting
  boolean isValidEscapedCharacter(char escapedChar) {
    return ImmutableSet.of('\\', 'd', '[', ']', '{', '}', '-').contains(escapedChar);
  }
}
