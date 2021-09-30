package gov.cms.bfd.common.generators.token;

import static org.junit.Assert.fail;

import org.junit.Test;

public class TokenPatternParserTest {

  @Test
  public void shouldGenerateCorrectTokenValues() {
    String pattern = "\\-[A-Za-z]\\d{3}[AB0-9]\\d{10}";

    TokenPatternParser parser = new TokenPatternParser();

    TokenPattern tokenPattern = parser.parse(pattern);

    fail();
  }
}
