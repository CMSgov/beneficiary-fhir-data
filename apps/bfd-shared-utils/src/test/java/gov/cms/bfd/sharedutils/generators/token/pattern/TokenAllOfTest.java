package gov.cms.bfd.sharedutils.generators.token.pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Sets;
import gov.cms.bfd.sharedutils.generators.exceptions.GeneratorException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import org.junit.Test;

public class TokenAllOfTest {

  @Test
  public void shouldCorrectlyValidatePattern() {
    TokenPattern mockPatternA = mock(TokenPattern.class);
    TokenPattern mockPatternB = mock(TokenPattern.class);

    doReturn(3).when(mockPatternA).tokenLength();

    doReturn(true).when(mockPatternA).isValidPattern("abc");

    doReturn(2).when(mockPatternB).tokenLength();

    doReturn(true).when(mockPatternB).isValidPattern("12");

    TokenAllOf pattern = new TokenAllOf(Arrays.asList(mockPatternA, mockPatternB));

    assertTrue(pattern.isValidPattern("abc12"));
    assertFalse(pattern.isValidPattern("abc123"));
    assertFalse(pattern.isValidPattern("abcab"));
  }

  @Test
  public void shouldGenerateCorrectToken() {
    TokenPattern mockPatternA = mock(TokenPattern.class);
    TokenPattern mockPatternB = mock(TokenPattern.class);

    doReturn(new BigInteger("10")).when(mockPatternA).getTotalPermutations();

    doReturn("G").when(mockPatternA).generateToken(new BigInteger("6"));

    doReturn(new BigInteger("10")).when(mockPatternB).getTotalPermutations();

    doReturn("B").when(mockPatternB).generateToken(new BigInteger("1"));

    TokenAllOf pattern = new TokenAllOf(Arrays.asList(mockPatternA, mockPatternB));

    String expected = "GB";
    String actual = pattern.generateToken(new BigInteger("61"));

    assertEquals(expected, actual);
  }

  @Test(expected = GeneratorException.class)
  public void shouldThrowGeneratorExceptionIfSeedSizeExceededPermutations() {
    TokenPattern mockPatternA = mock(TokenPattern.class);
    TokenPattern mockPatternB = mock(TokenPattern.class);

    doReturn(new BigInteger("10")).when(mockPatternA).getTotalPermutations();

    doReturn("G").when(mockPatternA).generateToken(new BigInteger("6"));

    doReturn(new BigInteger("10")).when(mockPatternB).getTotalPermutations();

    doReturn("B").when(mockPatternB).generateToken(new BigInteger("1"));

    TokenAllOf pattern = new TokenAllOf(Arrays.asList(mockPatternA, mockPatternB));

    pattern.generateToken(new BigInteger("9961"));
  }

  @Test
  public void shouldCorrectlyCalculateTokenValue() {
    TokenPattern mockPatternA = mock(TokenPattern.class);
    TokenPattern mockPatternB = mock(TokenPattern.class);

    doReturn(3).when(mockPatternA).tokenLength();

    doReturn(new BigInteger("15")).when(mockPatternA).getTotalPermutations();

    doReturn(new BigInteger("3")).when(mockPatternA).parseTokenValue("ABA");

    doReturn(2).when(mockPatternB).tokenLength();

    doReturn(new BigInteger("20")).when(mockPatternB).getTotalPermutations();

    doReturn(new BigInteger("22")).when(mockPatternB).parseTokenValue("22");

    TokenAllOf pattern = new TokenAllOf(Arrays.asList(mockPatternA, mockPatternB));

    // Should be ABA = 3 (AAA, AAB, AAC, ABA)
    // 20 permutations in 2nd pattern, so 3 * 20 = 60
    // then add the last string "22", which resolves simply to a value of 22
    // 60 + 22 = 82
    BigInteger expected = new BigInteger("82");
    BigInteger actual = pattern.calculateTokenValue("ABA22");

    assertEquals(expected, actual);
  }

  @Test
  public void shouldGetCorrectTokenLength() {
    TokenPattern mockPatternA = mock(TokenPattern.class);
    TokenPattern mockPatternB = mock(TokenPattern.class);

    doReturn(3).when(mockPatternA).tokenLength();

    doReturn(5).when(mockPatternB).tokenLength();

    TokenAllOf pattern = new TokenAllOf(Arrays.asList(mockPatternA, mockPatternB));

    int expected = 8;
    int actual = pattern.tokenLength();

    assertEquals(expected, actual);
  }

  @Test
  public void shouldCorrectlyCalculatePermutations() {
    TokenPattern mockPatternA = mock(TokenPattern.class);
    TokenPattern mockPatternB = mock(TokenPattern.class);

    doReturn(new BigInteger("5")).when(mockPatternA).getTotalPermutations();

    doReturn(new BigInteger("10")).when(mockPatternB).getTotalPermutations();

    TokenAllOf pattern = new TokenAllOf(Arrays.asList(mockPatternA, mockPatternB));

    BigInteger expected = new BigInteger("50");
    BigInteger actual = pattern.calculatePermutations();

    assertEquals(expected, actual);
  }

  @Test
  public void shouldCorrectlyCheckContainsAnyOf() {
    TokenPattern mockPatternA = mock(TokenPattern.class);
    TokenPattern mockPatternB = mock(TokenPattern.class);

    Set<Character> chars = Collections.singleton('A');

    doReturn(false).when(mockPatternA).containsAnyOf(chars);

    doReturn(false).when(mockPatternB).containsAnyOf(anySet());

    doReturn(true).when(mockPatternB).containsAnyOf(chars);

    TokenAllOf pattern = new TokenAllOf(Arrays.asList(mockPatternA, mockPatternB));

    assertTrue(pattern.containsAnyOf(chars));
    assertFalse(pattern.containsAnyOf(Collections.singleton('B')));
  }

  @Test
  public void shouldCorrectlyGetCharacters() {
    TokenPattern mockPatternA = mock(TokenPattern.class);
    TokenPattern mockPatternB = mock(TokenPattern.class);

    Set<Character> charsA = Sets.newHashSet('A', 'B', 'C');
    Set<Character> charsB = Sets.newHashSet('A', 'B', '1', '2', '3');

    doReturn(charsA).when(mockPatternA).characters();

    doReturn(charsB).when(mockPatternB).characters();

    TokenAllOf pattern = new TokenAllOf(Arrays.asList(mockPatternA, mockPatternB));

    Set<Character> expected = Sets.newHashSet('A', 'B', 'C', '1', '2', '3');
    Set<Character> actual = pattern.characters();

    assertEquals(expected, actual);
  }
}
