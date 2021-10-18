package gov.cms.bfd.sharedutils.generators.token.pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Sets;
import gov.cms.bfd.sharedutils.generators.exceptions.GeneratorException;
import gov.cms.bfd.sharedutils.generators.exceptions.ParsingException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import org.junit.Test;

public class TokenOneOfTest {

  // DefaultAnnotationParam - Better to be explicit
  @SuppressWarnings("DefaultAnnotationParam")
  @Test(expected = Test.None.class)
  public void shouldNotThrowExceptionIfValidOrTokens() {
    TokenPattern mockPatternA = mock(TokenPattern.class);
    TokenPattern mockPatternB = mock(TokenPattern.class);

    doReturn(false).when(mockPatternA).overlaps(any(TokenPattern.class));

    doReturn(false).when(mockPatternB).overlaps(any(TokenPattern.class));

    new TokenOneOf(Arrays.asList(mockPatternA, mockPatternB));
  }

  @Test(expected = ParsingException.class)
  public void shouldThrowParsingExceptionIfDuplicateTokenPatternFound() {
    TokenPattern mockPatternA = mock(TokenPattern.class);

    new TokenOneOf(Arrays.asList(mockPatternA, mockPatternA));
  }

  @Test(expected = ParsingException.class)
  public void shouldThrowParsingExceptionIfTokenPatternOverlapFound() {
    TokenPattern mockPatternA = mock(TokenPattern.class);
    TokenPattern mockPatternB = mock(TokenPattern.class);

    doReturn(true).when(mockPatternA).overlaps(mockPatternB);

    doReturn(true).when(mockPatternB).overlaps(mockPatternA);

    new TokenOneOf(Arrays.asList(mockPatternA, mockPatternB));
  }

  @Test
  public void shouldReturnTrueForIsValidPattern() {
    TokenPattern mockPatternA = mock(TokenPattern.class);
    TokenPattern mockPatternB = mock(TokenPattern.class);

    String token = "token";

    doReturn(false).when(mockPatternA).isValidPattern(token);

    doReturn(true).when(mockPatternB).isValidPattern(token);

    TokenOneOf pattern = new TokenOneOf(Arrays.asList(mockPatternA, mockPatternB));

    assertTrue(pattern.isValidPattern(token));
  }

  @Test
  public void shouldReturnFalseForIsValidPattern() {
    TokenPattern mockPatternA = mock(TokenPattern.class);
    TokenPattern mockPatternB = mock(TokenPattern.class);

    String token = "token";

    doReturn(false).when(mockPatternA).isValidPattern(token);

    doReturn(false).when(mockPatternB).isValidPattern(token);

    TokenOneOf pattern = new TokenOneOf(Arrays.asList(mockPatternA, mockPatternB));

    assertFalse(pattern.isValidPattern(token));
  }

  @Test
  public void shouldGenerateTokenFromSeed() {
    TokenPattern mockPatternA = mock(TokenPattern.class);
    TokenPattern mockPatternB = mock(TokenPattern.class);

    String expected = "token";

    BigInteger seed = new BigInteger("10");

    doReturn(new BigInteger("3")).when(mockPatternA).getTotalPermutations();

    doReturn(new BigInteger("100")).when(mockPatternB).getTotalPermutations();

    doReturn(expected).when(mockPatternB).createToken(new BigInteger("7"));

    TokenOneOf pattern = new TokenOneOf(Arrays.asList(mockPatternA, mockPatternB));

    String actual = pattern.generateToken(seed);

    assertEquals(expected, actual);
  }

  @Test(expected = GeneratorException.class)
  public void shouldThrowGeneratorExceptionIfSeedValueTooBig() {
    TokenPattern mockPatternA = mock(TokenPattern.class);
    TokenPattern mockPatternB = mock(TokenPattern.class);

    String expected = "token";

    BigInteger seed = new BigInteger("700");

    doReturn(new BigInteger("3")).when(mockPatternA).getTotalPermutations();

    doReturn(new BigInteger("20")).when(mockPatternB).getTotalPermutations();

    doReturn(expected).when(mockPatternB).createToken(new BigInteger("697"));

    TokenOneOf pattern = new TokenOneOf(Arrays.asList(mockPatternA, mockPatternB));

    pattern.generateToken(seed);
  }

  @Test
  public void shouldCalculateCorrectTokenValue() {
    TokenPattern mockPatternA = mock(TokenPattern.class);
    TokenPattern mockPatternB = mock(TokenPattern.class);

    String token = "token";

    doReturn(false).when(mockPatternA).isValidPattern(token);

    doReturn(new BigInteger("5")).when(mockPatternA).getTotalPermutations();

    doReturn(true).when(mockPatternB).isValidPattern(token);

    doReturn(new BigInteger("10")).when(mockPatternB).parseTokenValue(token);

    TokenOneOf pattern = new TokenOneOf(Arrays.asList(mockPatternA, mockPatternB));

    BigInteger expected = new BigInteger("15");
    BigInteger actual = pattern.calculateTokenValue(token);

    assertEquals(expected, actual);
  }

  @Test(expected = ParsingException.class)
  public void shouldThrowExceptionIfTokenPatternDoesNotMatch() {
    TokenPattern mockPatternA = mock(TokenPattern.class);
    TokenPattern mockPatternB = mock(TokenPattern.class);

    String token = "token";

    doReturn(false).when(mockPatternA).isValidPattern(token);

    doReturn(new BigInteger("5")).when(mockPatternA).getTotalPermutations();

    doReturn(false).when(mockPatternB).isValidPattern(token);

    doReturn(new BigInteger("10")).when(mockPatternB).getTotalPermutations();

    TokenOneOf pattern = new TokenOneOf(Arrays.asList(mockPatternA, mockPatternB));

    pattern.calculateTokenValue(token);
  }

  @Test
  public void shouldReturnCorrectTokenLength() {
    TokenPattern mockPatternA = mock(TokenPattern.class);
    TokenPattern mockPatternB = mock(TokenPattern.class);

    int expected = 5;

    doReturn(expected).when(mockPatternA).tokenLength();

    TokenOneOf pattern = new TokenOneOf(Arrays.asList(mockPatternA, mockPatternB));

    int actual = pattern.tokenLength();

    assertEquals(expected, actual);
  }

  @Test
  public void shouldCalculateCorrectPermutations() {
    TokenPattern mockPatternA = mock(TokenPattern.class);
    TokenPattern mockPatternB = mock(TokenPattern.class);

    doReturn(new BigInteger("10")).when(mockPatternA).getTotalPermutations();

    doReturn(new BigInteger("7")).when(mockPatternB).getTotalPermutations();

    TokenOneOf pattern = new TokenOneOf(Arrays.asList(mockPatternA, mockPatternB));

    BigInteger expected = new BigInteger("17");
    BigInteger actual = pattern.calculatePermutations();

    assertEquals(expected, actual);
  }

  @Test
  public void shouldReturnTrueForContainsCharacters() {
    TokenPattern mockPatternA = mock(TokenPattern.class);
    TokenPattern mockPatternB = mock(TokenPattern.class);

    Set<Character> chars = Collections.singleton('A');

    doReturn(false).when(mockPatternA).containsAnyOf(anySet());

    doReturn(true).when(mockPatternB).containsAnyOf(chars);

    TokenOneOf pattern = new TokenOneOf(Arrays.asList(mockPatternA, mockPatternB));

    assertTrue(pattern.containsAnyOf(chars));
  }

  @Test
  public void shouldReturnFalseForContainsCharacters() {
    TokenPattern mockPatternA = mock(TokenPattern.class);
    TokenPattern mockPatternB = mock(TokenPattern.class);

    Set<Character> chars = Collections.singleton('A');

    doReturn(false).when(mockPatternA).containsAnyOf(anySet());

    doReturn(false).when(mockPatternB).containsAnyOf(chars);

    TokenOneOf pattern = new TokenOneOf(Arrays.asList(mockPatternA, mockPatternB));

    assertFalse(pattern.containsAnyOf(chars));
  }

  @Test
  public void shouldReturnAllCharacters() {
    TokenPattern mockPatternA = mock(TokenPattern.class);
    TokenPattern mockPatternB = mock(TokenPattern.class);

    Set<Character> charsA = Sets.newHashSet('A', 'B', 'C');
    Set<Character> charsB = Sets.newHashSet('A', '0', '1', '2');

    doReturn(charsA).when(mockPatternA).characters();

    doReturn(charsB).when(mockPatternB).characters();

    TokenOneOf pattern = new TokenOneOf(Arrays.asList(mockPatternA, mockPatternB));

    Set<Character> expected = Sets.newHashSet('A', 'B', 'C', '0', '1', '2');
    Set<Character> actual = pattern.characters();

    assertEquals(expected, actual);
  }
}
