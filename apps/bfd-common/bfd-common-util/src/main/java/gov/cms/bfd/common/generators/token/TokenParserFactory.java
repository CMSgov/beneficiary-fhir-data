package gov.cms.bfd.common.generators.token;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import gov.cms.bfd.common.generators.token.parser.TokenBracketParser;
import gov.cms.bfd.common.generators.token.parser.TokenParser;
import gov.cms.bfd.common.generators.token.parser.TokenRangeParser;
import gov.cms.bfd.common.generators.token.parser.TokenRepeatParser;
import gov.cms.bfd.common.generators.token.parser.TokenSingletonParser;
import java.util.Queue;
import java.util.Set;

public class TokenParserFactory {

  private static final TokenParser SINGLETON_PARSER = new TokenSingletonParser();
  private static final TokenParser BRACKET_PARSER = new TokenBracketParser();
  private static final TokenParser REPEAT_PARSER = new TokenRepeatParser();
  private static final TokenParser RANGE_PARSER = new TokenRangeParser();

  private static final Set<Character> INVALID_SINGLETONS = ImmutableSet.of(']', '}', '-');

  private TokenParserFactory() {}

  public static TokenParser createTokenParser(Queue<Character> patternStream) {
    // ConstantConditions - This will never be null
    //noinspection ConstantConditions
    char firstToken = patternStream.peek();

    if (firstToken == '[') {
      return BRACKET_PARSER;
    } else if (firstToken == '{') {
      return REPEAT_PARSER;
    } else if (isValidRangeSingleton(firstToken)) {
      return RANGE_PARSER;
    } else if (isValidSingleton(firstToken)) {
      return SINGLETON_PARSER;
    } else {
      throw new IllegalArgumentException(
          "Unexpected/invalid token '" + firstToken + "' encountered.");
    }
  }

  @VisibleForTesting
  static boolean isValidRangeSingleton(char token) {
    return Character.isAlphabetic(token) || Character.isDigit(token);
  }

  @VisibleForTesting
  static boolean isValidSingleton(char token) {
    return !INVALID_SINGLETONS.contains(token);
  }
}
