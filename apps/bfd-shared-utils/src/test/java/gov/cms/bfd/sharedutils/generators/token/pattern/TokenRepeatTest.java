package gov.cms.bfd.sharedutils.generators.token.pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Set;
import org.junit.Test;

public class TokenRepeatTest {

  @Test
  public void shouldSayTokenIsValidPattern() {
    TokenPattern mockPattern = mock(TokenPattern.class);

    doReturn(2).when(mockPattern).tokenLength();

    doReturn(true).when(mockPattern).isValidPattern("ab");

    TokenRepeat pattern = new TokenRepeat(mockPattern, 3);

    assertTrue(pattern.isValidPattern("ababab"));
  }

  @Test
  public void shouldSayTokenIsInvalidPattern() {
    TokenPattern mockPattern = mock(TokenPattern.class);

    doReturn(2).when(mockPattern).tokenLength();

    doReturn(true).when(mockPattern).isValidPattern("ab");

    TokenRepeat pattern = new TokenRepeat(mockPattern, 3);

    assertFalse(pattern.isValidPattern("ababac"));
  }

  @Test
  public void shouldGenerateCorrectToken() {
    TokenPattern mockPattern = mock(TokenPattern.class);

    doReturn(new BigInteger("2")).when(mockPattern).getTotalPermutations();

    doReturn("1").when(mockPattern).createToken(new BigInteger("1"));

    doReturn("0").when(mockPattern).createToken(new BigInteger("0"));

    TokenRepeat pattern = new TokenRepeat(mockPattern, 4);

    String expected = "1010";
    String actual = pattern.generateToken(new BigInteger("10"));

    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturnCorrectTokenValue() {
    TokenPattern mockPattern = mock(TokenPattern.class);

    doReturn(1).when(mockPattern).tokenLength();

    doReturn(new BigInteger("2")).when(mockPattern).getTotalPermutations();

    doReturn(new BigInteger("1")).when(mockPattern).parseTokenValue("1");

    doReturn(new BigInteger("0")).when(mockPattern).parseTokenValue("0");

    TokenRepeat pattern = new TokenRepeat(mockPattern, 4);

    BigInteger expected = new BigInteger("10");
    BigInteger actual = pattern.parseTokenValue("1010");

    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturnCorrectTokenLength() {
    TokenPattern mockPattern = mock(TokenPattern.class);

    doReturn(1).when(mockPattern).tokenLength();

    TokenRepeat pattern = new TokenRepeat(mockPattern, 4);

    int expected = 4;
    int actual = pattern.tokenLength();

    assertEquals(expected, actual);
  }

  @Test
  public void shouldCalculateCorrectPermutations() {
    TokenPattern mockPattern = mock(TokenPattern.class);

    doReturn(new BigInteger("2")).when(mockPattern).getTotalPermutations();

    TokenRepeat pattern = new TokenRepeat(mockPattern, 4);

    BigInteger expected = new BigInteger("16");
    BigInteger actual = pattern.calculatePermutations();

    assertEquals(expected, actual);
  }

  @Test
  public void shouldInvokeStoredPatternContainsAnyOf() {
    TokenPattern mockPattern = mock(TokenPattern.class);

    TokenRepeat pattern = new TokenRepeat(mockPattern, 4);

    Set<Character> expectedSet = Collections.singleton('a');

    pattern.containsAnyOf(expectedSet);

    verify(mockPattern, times(1)).containsAnyOf(expectedSet);
  }

  @Test
  public void shouldInvokeStoredPatternCharacters() {
    TokenPattern mockPattern = mock(TokenPattern.class);

    Set<Character> expected = Collections.singleton('a');

    doReturn(expected).when(mockPattern).characters();

    TokenRepeat pattern = new TokenRepeat(mockPattern, 4);

    Set<Character> actual = pattern.characters();

    assertSame(expected, actual);
  }
}
