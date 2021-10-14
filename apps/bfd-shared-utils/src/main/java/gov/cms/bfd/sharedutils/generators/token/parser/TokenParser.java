package gov.cms.bfd.sharedutils.generators.token.parser;

import gov.cms.bfd.sharedutils.generators.token.pattern.TokenPattern;
import java.util.Queue;

/** Base class for parsing token strings. */
public interface TokenParser {

  /**
   * Parses the next values of the given pattern stream, producing a {@link TokenPattern} object.
   *
   * @param patternStream The pattern stream to pull the next token values from.
   * @return The created {@link TokenPattern} for the next values that were pulled from the pattern
   *     stream.
   */
  TokenPattern parse(Queue<Character> patternStream);
}
