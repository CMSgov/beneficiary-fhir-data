package gov.cms.bfd.common.generators.token.pattern;

import gov.cms.bfd.common.generators.token.TokenPattern;
import java.util.Set;
import lombok.Data;

@Data
public class TokenOneOf implements TokenPattern {

  private final Set<TokenPattern> orTokens;
}
