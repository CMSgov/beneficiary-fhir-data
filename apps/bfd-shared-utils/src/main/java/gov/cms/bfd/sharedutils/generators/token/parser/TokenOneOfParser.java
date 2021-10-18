package gov.cms.bfd.sharedutils.generators.token.parser;

import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenAllOf;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenOneOf;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenPattern;
import java.util.ArrayList;
import java.util.List;

/**
 * This class helps parse 'or' group tokens.
 *
 * <p>Group parsing is mostly common logic, so this only defines the parts that make it different.
 *
 * <p>This parser handles creating separate {@link TokenPattern}s for a defined group. This parser
 * is invoked by the {@link TokenBracketParser} which will have already stripped it from the
 * brackets.
 *
 * <p>Example: [AB0-9] defines an 'or' group that can be A, B, or any digit.
 */
public class TokenOneOfParser extends AbstractTokenGroupParser<List<TokenPattern>> {

  @Override
  protected List<TokenPattern> createCollection() {
    return new ArrayList<>();
  }

  @Override
  protected TokenPattern createTokenPattern(List<TokenPattern> tokenPatterns) {
    TokenPattern pattern = newTokenOneOf(tokenPatterns);
    pattern.init();
    return pattern;
  }

  /**
   * Helper method to allow easier unit testing.
   *
   * @param tokenPatterns The list of {@link TokenPattern}s to create the {@link TokenAllOf} from.
   * @return The {@link TokenAllOf} containing the passed in {@link TokenPattern}s.
   */
  @VisibleForTesting
  TokenPattern newTokenOneOf(List<TokenPattern> tokenPatterns) {
    return new TokenOneOf(tokenPatterns);
  }
}
