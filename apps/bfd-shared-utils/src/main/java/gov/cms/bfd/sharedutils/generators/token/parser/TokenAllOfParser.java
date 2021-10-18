package gov.cms.bfd.sharedutils.generators.token.parser;

import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenAllOf;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenPattern;
import java.util.ArrayList;
import java.util.List;

/**
 * This class helps parse 'and' group tokens.
 *
 * <p>Group parsing is mostly common logic, so this only defines the parts that make it different.
 *
 * <p>This parser is probably only used for the initial parsing of a full pattern with the current
 * API
 */
public class TokenAllOfParser extends AbstractTokenGroupParser<List<TokenPattern>> {

  @Override
  protected List<TokenPattern> createCollection() {
    return new ArrayList<>();
  }

  @Override
  protected TokenPattern createTokenPattern(List<TokenPattern> tokenPatterns) {
    TokenPattern pattern = newTokenAllOf(tokenPatterns);
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
  TokenPattern newTokenAllOf(List<TokenPattern> tokenPatterns) {
    return new TokenAllOf(tokenPatterns);
  }
}
