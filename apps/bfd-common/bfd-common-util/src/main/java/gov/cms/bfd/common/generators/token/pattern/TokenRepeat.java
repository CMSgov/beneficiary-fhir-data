package gov.cms.bfd.common.generators.token.pattern;

import gov.cms.bfd.common.generators.token.TokenPattern;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenRepeat implements TokenPattern {

  private TokenPattern pattern;
  private final int repeats;
}
