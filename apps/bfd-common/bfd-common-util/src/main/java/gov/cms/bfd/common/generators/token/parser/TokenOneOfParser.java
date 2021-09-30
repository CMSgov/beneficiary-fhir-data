package gov.cms.bfd.common.generators.token.parser;

import gov.cms.bfd.common.generators.token.pattern.TokenOneOf;
import gov.cms.bfd.common.generators.token.pattern.TokenPattern;
import java.util.HashSet;
import java.util.Set;

public class TokenOneOfParser extends AbstractTokenGroupParser<Set<TokenPattern>> {

  @Override
  protected Set<TokenPattern> createCollection() {
    return new HashSet<>();
  }

  @Override
  protected TokenPattern createTokenPattern(Set<TokenPattern> tokenPatterns) {
    TokenOneOf pattern = new TokenOneOf(tokenPatterns);
    pattern.init();
    pattern.sortTokens();
    return pattern;
  }
}
