package gov.cms.bfd.common.generators.token;

import static org.junit.Assert.fail;

import gov.cms.bfd.common.generators.token.pattern.TokenPattern;
import org.junit.Test;

public class TokenPatternParserTest {

  @Test
  public void shouldGenerateCorrectTokenValues() {
    String pattern = "\\-[A-Za-z]TEST[AB0-9]\\d{10}";

    TokenPatternParser parser = new TokenPatternParser();

    TokenPattern tokenPattern = parser.parse(pattern);

    //    System.out.println(tokenPattern.createToken(2684358725416156L));

    //    for (int i = 0; i < 10; ++i) {
    //      System.out.println(tokenPattern.createToken(i));
    //    }

    for (int i = 0; i < 40; ++i) {
      long s = (long) (tokenPattern.getTotalPermutations() * Math.random());
      System.out.println(s + ": " + tokenPattern.createToken(s));
    }

    fail();
  }
}
