package gov.cms.bfd.common.generators.token.pattern;

import gov.cms.bfd.common.generators.token.TokenPattern;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenAllOf implements TokenPattern {

  private List<TokenPattern> pattern;
}
