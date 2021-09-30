package gov.cms.bfd.common.generators.token.parser;

import gov.cms.bfd.common.generators.token.pattern.TokenAllOf;
import gov.cms.bfd.common.generators.token.pattern.TokenPattern;
import java.util.ArrayList;
import java.util.List;

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
