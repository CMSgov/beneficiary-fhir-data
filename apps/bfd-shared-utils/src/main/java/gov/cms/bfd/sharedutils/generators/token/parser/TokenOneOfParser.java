package gov.cms.bfd.sharedutils.generators.token.parser;

import gov.cms.bfd.sharedutils.generators.token.pattern.TokenOneOf;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenPattern;
import java.util.ArrayList;
import java.util.List;

/**
 * This class helps parse 'or' group tokens.
 *
 * <p>Group parsing is mostly common, so this only defines the parts that make it different.
 */
public class TokenOneOfParser extends AbstractTokenGroupParser<List<TokenPattern>> {

  @Override
  protected List<TokenPattern> createCollection() {
    return new ArrayList<>();
  }

  @Override
  protected TokenPattern createTokenPattern(List<TokenPattern> tokenPatterns) {
    TokenOneOf pattern = new TokenOneOf(tokenPatterns);
    pattern.init();
    return pattern;
  }
}
