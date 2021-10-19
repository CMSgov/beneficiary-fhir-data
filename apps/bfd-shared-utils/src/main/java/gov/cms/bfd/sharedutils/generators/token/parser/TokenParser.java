package gov.cms.bfd.sharedutils.generators.token.parser;

import gov.cms.bfd.sharedutils.generators.token.pattern.TokenPattern;
import java.util.List;
import java.util.Queue;
import lombok.EqualsAndHashCode;

/** Base class for parsing token strings. */
@EqualsAndHashCode
public abstract class TokenParser {

  /**
   * Parses the next values of the given pattern stream, producing a {@link TokenPattern} object.
   *
   * @param patternStream The pattern stream to pull the next token values from.
   * @return The created {@link TokenPattern} for the next values that were pulled from the pattern
   *     stream.
   */
  public abstract TokenPattern parse(Queue<Character> patternStream);

  /**
   * Parses the next values from the given pattern stream, producing a {@link TokenPattern} object
   * and adding it to the given {@link List}.
   *
   * @param patternStream The pattern stream to pull the next token values from.
   * @param patterns The {@link List} to add the newly parsed {@link TokenPattern} to.
   */
  public void parseAndAdd(Queue<Character> patternStream, List<TokenPattern> patterns) {
    TokenPattern pattern = parse(patternStream);
    patterns.add(pattern);
  }
}
