package gov.cms.bfd.common.generators.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import gov.cms.bfd.common.exceptions.GeneratorException;
import gov.cms.bfd.common.exceptions.ParsingException;
import gov.cms.bfd.common.generators.token.pattern.TokenPattern;
import org.junit.Test;

public class TokenPatternParserTest {

  @Test
  public void shouldProduceExpectedPattern() {
    String pattern = "\\-[A-Za-z]TEST[AB0-9]\\d{10}";

    TokenPatternParser parser = new TokenPatternParser();

    TokenPattern tokenPattern = parser.parse(pattern);

    String actual = tokenPattern.createToken(1265846364265L);

    assertEquals("-KTEST65846364265", actual);
  }

  @Test(expected = GeneratorException.class)
  public void shouldThrowParsingExceptionForSeedTooHigh() {
    String pattern = "[ABF][1-5]";

    TokenPatternParser parser = new TokenPatternParser();

    TokenPattern tokenPattern = parser.parse(pattern);

    tokenPattern.createToken(1000);
  }

  @Test(expected = GeneratorException.class)
  public void shouldThrowParsingExceptionForNegativeSeed() {
    String pattern = "[ABF][1-5]";

    TokenPatternParser parser = new TokenPatternParser();

    TokenPattern tokenPattern = parser.parse(pattern);

    tokenPattern.createToken(-1);
  }

  @Test
  public void shouldCorrectlyValidatePattern() {
    String pattern = "\\-[A-Za-z]TEST[AB0-9]\\d{10}";

    TokenPatternParser parser = new TokenPatternParser();

    TokenPattern tokenPattern = parser.parse(pattern);

    assertTrue(tokenPattern.isValidPattern("-ATESTA5397340181"));
    assertFalse(tokenPattern.isValidPattern("ATEST-5397340181"));
  }

  @Test
  public void shouldGetCorrectValueOfToken() {
    String pattern = "[ABF][1-5]";

    TokenPatternParser parser = new TokenPatternParser();

    TokenPattern tokenPattern = parser.parse(pattern);

    long tokenValue = tokenPattern.parseTokenValue("B2").longValue();

    assertEquals(6, tokenValue);
  }

  @Test
  public void shouldCorrectConvertToken() {
    String fromPattern = "[ABF][1-5]";
    String toPattern = "A[0-9][AB]";

    TokenPatternParser parser = new TokenPatternParser();

    TokenPattern fromTokenPattern = parser.parse(fromPattern);
    TokenPattern toTokenPattern = parser.parse(toPattern);

    String token = toTokenPattern.convertToken("B3", fromTokenPattern);

    assertEquals("A3B", token);
  }

  // If the target pattern doesn't have enough permutations to represent the base value, it should
  // throw an error.
  @Test(expected = ParsingException.class)
  public void shouldThrowParsingExceptionForInvalidToken() {
    String fromPattern = "[ABF][1-5]";
    String toPattern = "[AB]";

    TokenPatternParser parser = new TokenPatternParser();

    TokenPattern fromTokenPattern = parser.parse(fromPattern);
    TokenPattern toTokenPattern = parser.parse(toPattern);

    toTokenPattern.convertToken("B3", fromTokenPattern);
  }
}
