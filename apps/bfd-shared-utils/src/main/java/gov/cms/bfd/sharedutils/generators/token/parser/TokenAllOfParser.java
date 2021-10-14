package gov.cms.bfd.sharedutils.generators.token.parser;

import gov.cms.bfd.sharedutils.generators.token.pattern.TokenAllOf;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenPattern;
import java.util.ArrayList;
import java.util.List;

/**
 * This class helps parse 'and' group tokens.
 *
 * <p>Group parsing is mostly common, so this only defines the parts that make it different.
 */
public class TokenAllOfParser extends AbstractTokenGroupParser<List<TokenPattern>> {

  @Override
  protected List<TokenPattern> createCollection() {
    return new ArrayList<>();
  }

  @Override
  protected TokenPattern createTokenPattern(List<TokenPattern> tokenPatterns) {
    TokenPattern pattern = new TokenAllOf(tokenPatterns);
    pattern.init();
    return pattern;
  }
}
