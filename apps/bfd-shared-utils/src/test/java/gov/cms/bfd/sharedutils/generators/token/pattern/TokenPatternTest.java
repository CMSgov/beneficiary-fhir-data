package gov.cms.bfd.sharedutils.generators.token.pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Sets;
import gov.cms.bfd.sharedutils.generators.exceptions.GeneratorException;
import gov.cms.bfd.sharedutils.generators.exceptions.ParsingException;
import java.math.BigInteger;
import java.util.Set;
import org.junit.Test;

public class TokenPatternTest {

  @Test
  public void shouldInvokeCalculatePermutations() {
    TokenPattern patternSpy = spy(TokenPattern.class);

    doReturn(new BigInteger("3")).when(patternSpy).calculatePermutations();

    assertNull(patternSpy.getTotalPermutations());
    patternSpy.init();
    assertEquals(new BigInteger("3"), patternSpy.getTotalPermutations());
    verify(patternSpy, times(1)).calculatePermutations();
  }

  @Test
  public void shouldCreateRandomToken() {
    TokenPattern patternSpy = spy(TokenPattern.class);

    doReturn(new BigInteger("3")).when(patternSpy).calculatePermutations();

    patternSpy.init();

    String expected = "token";

    doReturn(expected).when(patternSpy).createToken(any(BigInteger.class));

    String actual = patternSpy.createToken();

    assertEquals(expected, actual);
  }

  @Test
  public void shouldCreateTokenFromSeed() {
    TokenPattern patternSpy = spy(TokenPattern.class);

    String expected = "token";

    doReturn(expected).when(patternSpy).createToken(new BigInteger("3"));

    String actual = patternSpy.createToken(3);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldCreateTokenFromBigIntSeed() {
    TokenPattern patternSpy = spy(TokenPattern.class);

    String expected = "token";

    doReturn(new BigInteger("100")).when(patternSpy).calculatePermutations();

    patternSpy.init();

    doReturn(expected).when(patternSpy).generateToken(new BigInteger("3"));

    String actual = patternSpy.createToken(new BigInteger("3"));

    assertEquals(expected, actual);
  }

  @Test(expected = GeneratorException.class)
  public void shouldThrowGeneratorExceptionIfSeedIsNegative() {
    TokenPattern patternSpy = spy(TokenPattern.class);

    doReturn(null).when(patternSpy).generateToken(any(BigInteger.class));

    patternSpy.createToken(new BigInteger("-3"));
  }

  @Test(expected = GeneratorException.class)
  public void shouldThrowGeneratorExceptionIfSeedTooBig() {
    TokenPattern patternSpy = spy(TokenPattern.class);

    doReturn(new BigInteger("1")).when(patternSpy).calculatePermutations();

    patternSpy.init();

    doReturn(null).when(patternSpy).generateToken(any(BigInteger.class));

    patternSpy.createToken(new BigInteger("3"));
  }

  @Test
  public void shouldReturnTrueForOverlaps() {
    TokenPattern patternSpy = spy(TokenPattern.class);

    Set<Character> characters = Sets.newHashSet('A', 'B');

    doReturn(characters).when(patternSpy).characters();

    TokenPattern mockPattern = mock(TokenPattern.class);

    doReturn(true).when(mockPattern).containsAnyOf(characters);

    assertTrue(patternSpy.overlaps(mockPattern));
  }

  @Test
  public void shouldReturnFalseForOverlaps() {
    TokenPattern patternSpy = spy(TokenPattern.class);

    Set<Character> characters = Sets.newHashSet('A', 'B');

    doReturn(characters).when(patternSpy).characters();

    TokenPattern mockPattern = mock(TokenPattern.class);

    doReturn(false).when(mockPattern).containsAnyOf(characters);

    assertFalse(patternSpy.overlaps(mockPattern));
  }

  @Test
  public void shouldCorrectConvertToken() {
    TokenPattern patternSpy = spy(TokenPattern.class);

    doReturn(new BigInteger("100")).when(patternSpy).calculatePermutations();

    patternSpy.init();

    TokenPattern mockPattern = mock(TokenPattern.class);

    String token = "token";

    String expected = "newToken";

    BigInteger tokenValue = new BigInteger("5");

    doReturn(tokenValue).when(mockPattern).parseTokenValue(token);

    doReturn(null).when(patternSpy).createToken(any(BigInteger.class));

    doReturn(expected).when(patternSpy).createToken(tokenValue);

    String actual = patternSpy.convertToken(token, mockPattern);

    assertEquals(expected, actual);
  }

  @Test(expected = ParsingException.class)
  public void shouldThrowErrorIfTargetPatternHasTooFewPermutations() {
    TokenPattern patternSpy = spy(TokenPattern.class);

    doReturn(new BigInteger("5")).when(patternSpy).calculatePermutations();

    patternSpy.init();

    TokenPattern mockPattern = mock(TokenPattern.class);

    String token = "token";

    BigInteger tokenValue = new BigInteger("15");

    doReturn(tokenValue).when(mockPattern).parseTokenValue(token);

    doReturn(null).when(patternSpy).createToken(any(BigInteger.class));

    patternSpy.convertToken(token, mockPattern);
  }

  @Test
  public void shouldCorrectlyParseTokenValue() {
    TokenPattern patternSpy = spy(TokenPattern.class);

    doReturn(5).when(patternSpy).tokenLength();

    String token = "token";

    BigInteger expected = new BigInteger("100");

    doReturn(expected).when(patternSpy).calculateTokenValue(token);

    BigInteger actual = patternSpy.parseTokenValue(token);

    assertEquals(expected, actual);
  }

  @Test(expected = ParsingException.class)
  public void shouldThrowParsingExceptionIfTokenLengthDoesNotMatch() {
    TokenPattern patternSpy = spy(TokenPattern.class);

    doReturn(3).when(patternSpy).tokenLength();

    String token = "token";

    patternSpy.parseTokenValue(token);
  }
}
