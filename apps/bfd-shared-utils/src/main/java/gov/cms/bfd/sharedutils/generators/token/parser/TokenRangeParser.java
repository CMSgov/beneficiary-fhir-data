package gov.cms.bfd.sharedutils.generators.token.parser;

import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.sharedutils.generators.exceptions.ParsingException;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenPattern;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenRange;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenSingleton;
import java.util.Queue;

/**
 * This class parses token ranges.
 *
 * <p>Currently only letters (upper or lower) and digits can be used in defined ranges.
 *
 * <p>This class is created by the {@link
 * gov.cms.bfd.sharedutils.generators.token.TokenParserFactory} if the next character in the pattern
 * stream can potentially be a range definition. This class subsequently checks if it actually is a
 * range by checking for an ensuing '-', and creates a {@link TokenSingleton} instead if it's not
 * there.
 *
 * <p>\d is automatically parsed as a range 0-9
 *
 * <p>Partial ranges can be used, ex: 4-8 or d-k.
 */
public class TokenRangeParser extends TokenParser {

  @Override
  public TokenPattern parse(Queue<Character> patternStream) {
    TokenPattern pattern;
    char token = patternStream.remove();

    if (!patternStream.isEmpty() && patternStream.peek() == '-') {
      pattern = parseRange(token, patternStream);
    } else {
      pattern = new TokenSingleton(token);
    }

    pattern.init();
    return pattern;
  }

  @VisibleForTesting
  TokenPattern parseRange(char lowerBound, Queue<Character> patternStream) {
    TokenPattern pattern;
    patternStream.remove(); // remove hyphen

    if (!patternStream.isEmpty()) {
      char upperBound = patternStream.remove();

      if (lowerBound >= 'a') {
        pattern = parseWithBoundsCheck(lowerBound, upperBound, 'a', 'z');
      } else if (lowerBound >= 'A') {
        pattern = parseWithBoundsCheck(lowerBound, upperBound, 'A', 'Z');
      } else {
        pattern = parseWithBoundsCheck(lowerBound, upperBound, '0', '9');
      }
    } else {
      throw new ParsingException("Unexpected end of token pattern.");
    }

    return pattern;
  }

  @VisibleForTesting
  TokenPattern parseWithBoundsCheck(
      char lowerBound, char upperBound, char minBound, char maxBound) {
    if (upperBound > lowerBound && lowerBound >= minBound && upperBound <= maxBound) {
      return new TokenRange(lowerBound, upperBound);
    }

    throw new ParsingException(
        "Invalid range defined in token pattern '" + lowerBound + "-" + upperBound + "'");
  }
}
