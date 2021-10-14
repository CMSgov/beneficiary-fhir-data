package gov.cms.bfd.sharedutils.generators.token;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import gov.cms.bfd.sharedutils.generators.token.parser.TokenBracketParser;
import gov.cms.bfd.sharedutils.generators.token.parser.TokenParser;
import gov.cms.bfd.sharedutils.generators.token.parser.TokenRangeParser;
import gov.cms.bfd.sharedutils.generators.token.parser.TokenRepeatParser;
import gov.cms.bfd.sharedutils.generators.token.parser.TokenSingletonParser;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenPattern;
import java.util.Queue;
import java.util.Set;

/**
 * Factory used to identify the correct {@link TokenParser} to create for the current pattern
 * stream.
 */
public class TokenParserFactory {

  private static final TokenParser SINGLETON_PARSER = new TokenSingletonParser();
  private static final TokenParser BRACKET_PARSER = new TokenBracketParser();
  private static final TokenParser REPEAT_PARSER = new TokenRepeatParser();
  private static final TokenParser RANGE_PARSER = new TokenRangeParser();

  private static final Set<Character> INVALID_SINGLETONS = ImmutableSet.of(']', '}', '-');

  private TokenParserFactory() {}

  /**
   * Creates a {@link TokenPattern} depending on the next available data in the given pattern
   * stream.
   *
   * @param patternStream A character stream of the token pattern being parsed.
   * @return The correct {@link TokenPattern} based on the next data in the given pattern stream.
   */
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

  /**
   * Determines if the given token is valid for use in a token range.
   *
   * @param token The token being checked.
   * @return True if the token is valid for a token range, False otherwise.
   */
  @VisibleForTesting
  static boolean isValidRangeSingleton(char token) {
    return Character.isAlphabetic(token) || Character.isDigit(token);
  }

  /**
   * Determines if the given token is a valid singleton token.
   *
   * <p>This acts as a means to prevent special meaning tokens from being used without being escaped
   * first.
   *
   * @param token The token to be checked.
   * @return True if the token is a valid singleton token, False otherwise.
   */
  @VisibleForTesting
  static boolean isValidSingleton(char token) {
    return !INVALID_SINGLETONS.contains(token);
  }
}
