package gov.cms.bfd.common.generators.token.parser;

import gov.cms.bfd.common.generators.token.TokenPattern;
import gov.cms.bfd.common.generators.token.pattern.TokenOneOf;
import java.util.HashSet;
import java.util.Set;

public class TokenOneOfParser extends AbstractTokenGroupParser<Set<TokenPattern>> {

  @Override
  protected Set<TokenPattern> createCollection() {
    return new HashSet<>();
  }

  @Override
  protected TokenPattern createTokenPattern(Set<TokenPattern> tokenPatterns) {
    return new TokenOneOf(tokenPatterns);
  }
}
