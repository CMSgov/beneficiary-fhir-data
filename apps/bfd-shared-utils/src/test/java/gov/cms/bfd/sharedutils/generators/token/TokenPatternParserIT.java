package gov.cms.bfd.sharedutils.generators.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import gov.cms.bfd.sharedutils.generators.exceptions.GeneratorException;
import gov.cms.bfd.sharedutils.generators.exceptions.ParsingException;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenPattern;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Enclosed.class)
public class TokenPatternParserIT {

  public static class NonParameterizedTests {
    @Test
    public void shouldProduceExpectedToken() {
      String pattern = "\\-[A-Za-z0-9]TEST[0-9AB]\\d{10}";

      TokenPatternParser parser = new TokenPatternParser();

      TokenPattern tokenPattern = parser.parse(pattern);

      String actual = tokenPattern.createToken(1265846364265L);

      assertEquals("-KTEST65846364265", actual);
    }

    @Test
    public void shouldProduceExpectedTokenWithDifferentPatternOrder() {
      String pattern = "\\-[0-9A-Za-z]TEST[AB0-9]\\d{10}";

      TokenPatternParser parser = new TokenPatternParser();

      TokenPattern tokenPattern = parser.parse(pattern);

      String actual = tokenPattern.createToken(1265846364265L);

      assertEquals("-ATEST45846364265", actual);
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
      String pattern = "[\\dA-F]{3}";

      TokenPatternParser parser = new TokenPatternParser();

      TokenPattern tokenPattern = parser.parse(pattern);

      assertEquals(3840, tokenPattern.parseTokenValue("F00").longValue());
      assertEquals(240, tokenPattern.parseTokenValue("0F0").longValue());
      assertEquals(15, tokenPattern.parseTokenValue("00F").longValue());
    }

    @Test
    public void shouldCorrectlyConvertToken() {
      String fromPattern = "\\-\\d{8}";
      String toPattern = "[0-9A-F]{8}";

      TokenPatternParser parser = new TokenPatternParser();

      TokenPattern fromTokenPattern = parser.parse(fromPattern);
      TokenPattern toTokenPattern = parser.parse(toPattern);

      String token = toTokenPattern.convertToken("-00004351", fromTokenPattern);

      assertEquals("000010FF", token);
    }

    @Test
    public void shouldCorrectlyConvertToken2() {
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

  @RequiredArgsConstructor
  @RunWith(Parameterized.class)
  public static class ValidPatternTests {

    @Parameterized.Parameters(name = "{index}: Stream(\"{0}\")")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {"[A-DE-J]1", null},
            {"[AA]1", ParsingException.class},
            {"[A-DD-F]1", ParsingException.class},
            {"]AB", IllegalArgumentException.class},
          });
    }

    private final String pattern;
    private final Class<? extends Exception> exceptionType;

    @Test
    public void test() {
      try {
        TokenPatternParser parser = new TokenPatternParser();

        parser.parse(pattern);

        if (exceptionType != null) {
          fail("Expected exception '" + exceptionType.getCanonicalName() + "' not thrown.");
        }
      } catch (Exception e) {
        assertNotNull(
            "Unexpected exception '" + e.getClass().getCanonicalName() + "' thrown", exceptionType);
        assertEquals(e.getClass(), exceptionType);
      }
    }
  }
}
