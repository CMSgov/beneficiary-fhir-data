package gov.cms.bfd.common.generators.token.pattern;

import gov.cms.bfd.common.generators.token.TokenPattern;
import lombok.Data;

@Data
public class TokenSingleton implements TokenPattern {

  private final char token;
}
