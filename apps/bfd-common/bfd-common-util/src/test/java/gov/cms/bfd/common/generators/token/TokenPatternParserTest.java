package gov.cms.bfd.common.generators.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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

  @Test
  public void validPatternTest() {
    String pattern = "\\-[A-Za-z]TEST[AB0-9]\\d{10}";

    TokenPatternParser parser = new TokenPatternParser();

    TokenPattern tokenPattern = parser.parse(pattern);

    assertTrue(tokenPattern.isValidPattern("-ATESTA5397340181"));
    assertFalse(tokenPattern.isValidPattern("ATEST-5397340181"));
  }

  @Test
  public void tokenValueTest() {
    String pattern = "\\-[A-Za-z]TEST[AB0-9]\\d{10}";

    TokenPatternParser parser = new TokenPatternParser();

    TokenPattern tokenPattern = parser.parse(pattern);

    assertEquals(1265846364265L, tokenPattern.parseTokenValue("-KTEST65846364265"));
  }

  @Test
  public void generateEvenOnly() {
    String pattern = "[A-Z]{3}\\d{1}[02468]";

    TokenPatternParser parser = new TokenPatternParser();

    TokenPattern tokenPattern = parser.parse(pattern);

    for (int i = 0; i < 50; i++) {
      System.out.println(tokenPattern.createToken(i));
    }
  }

  @Test
  public void generateAB() {
    String pattern = "[ABF][1-5]";

    TokenPatternParser parser = new TokenPatternParser();

    TokenPattern tokenPattern = parser.parse(pattern);

    for (int i = 0; i < tokenPattern.getTotalPermutations(); i++) {
      System.out.println(tokenPattern.createToken(i));
    }
  }

  @Test
  public void escapedTokens() {
    String pattern = "\\[TST\\][a-c]\\-[0-3]";

    TokenPatternParser parser = new TokenPatternParser();

    TokenPattern tokenPattern = parser.parse(pattern);

    for (int i = 0; i < tokenPattern.getTotalPermutations(); i++) {
      System.out.println(tokenPattern.createToken(i));
    }
  }

  @Test
  public void nextValueTest() {
    String pattern = "[ABF][1-5]";

    TokenPatternParser parser = new TokenPatternParser();

    TokenPattern tokenPattern = parser.parse(pattern);

    long tokenValue = tokenPattern.parseTokenValue("B2");

    assertEquals("B3", tokenPattern.createToken(tokenValue + 1));
  }
}
