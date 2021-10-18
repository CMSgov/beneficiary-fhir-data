package gov.cms.bfd.sharedutils.generators.token.pattern;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Enclosed.class)
public class TokenSingletonTest {

  @AllArgsConstructor
  @RunWith(Parameterized.class)
  public static class ValidPatternTests {

    @Parameterized.Parameters(name = "{index}: Stream(\"{0}\"), shouldThrow: {2}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {'a', "a", true},
            {'A', "A", true},
            {' ', " ", true},
            {'a', "A", false},
            {'a', "a ", false},
            {'a', "ab", false},
            {'a', "", false},
          });
    }

    private final char token;
    private final String tokenValue;
    private final boolean isValid;

    @Test
    public void test() {
      TokenSingleton pattern = new TokenSingleton(token);

      assertEquals(isValid, pattern.isValidPattern(tokenValue));
    }
  }

  @AllArgsConstructor
  @RunWith(Parameterized.class)
  public static class GenerateTokenTests {

    @Parameterized.Parameters(name = "{index}: token(\"{0}\"), BigInt: {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {'a', "0"},
            {'a', "1"},
            {'a', "-1"},
          });
    }

    private final char token;
    private final String seed;

    @Test
    public void test() {
      TokenSingleton pattern = new TokenSingleton(token);

      assertEquals(String.valueOf(token), pattern.generateToken(new BigInteger(seed)));
    }
  }

  @AllArgsConstructor
  @RunWith(Parameterized.class)
  public static class CalculateTokenValueTests {

    @Parameterized.Parameters(name = "{index}: token(\"{0}\"), BigInt: {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {'a', "a"},
            {'a', "doesn't matter"},
            {'a', ""},
            {'a', null},
          });
    }

    private final char token;
    private final String tokenString;

    @Test
    public void test() {
      TokenSingleton pattern = new TokenSingleton(token);

      assertEquals(BigInteger.ZERO, pattern.calculateTokenValue(tokenString));
    }
  }

  @AllArgsConstructor
  @RunWith(Parameterized.class)
  public static class ContainsAnyOfTests {

    @Parameterized.Parameters(name = "{index}: token(\"{0}\"), BigInt: {1}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {'a', new Character[] {'a', 'b', 'c'}, true},
            {'a', new Character[] {'A', 'b', 'c'}, false},
            {'a', new Character[] {}, false},
          });
    }

    private final char token;
    private final Character[] characters;
    private final boolean doesContain;

    @Test
    public void test() {
      Set<Character> charSet = Arrays.stream(characters).collect(Collectors.toSet());

      TokenSingleton pattern = new TokenSingleton(token);

      assertEquals(doesContain, pattern.containsAnyOf(charSet));
    }
  }

  public static class NonParameterizedTests {

    @Test
    public void shouldReturnCorrectTokenLength() {
      TokenSingleton pattern = new TokenSingleton('a');

      assertEquals(1, pattern.tokenLength());
    }

    @Test
    public void shouldCalculateCorrectPermutations() {
      TokenSingleton pattern = new TokenSingleton('a');

      assertEquals(BigInteger.ONE, pattern.calculatePermutations());
    }

    @Test
    public void shouldReturnCorrectCharacters() {
      TokenSingleton pattern = new TokenSingleton('a');

      assertEquals(Collections.singleton('a'), pattern.characters());
    }
  }
}
