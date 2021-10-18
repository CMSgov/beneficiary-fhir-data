package gov.cms.bfd.sharedutils.generators.token.pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import gov.cms.bfd.sharedutils.generators.exceptions.GeneratorException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Enclosed.class)
public class TokenRangeTest {

  @AllArgsConstructor
  @RunWith(Parameterized.class)
  public static class ValidPatternTests {

    @Parameterized.Parameters(name = "{index}: Range({0}, {1}), Token({3}), shouldThrow: {2}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {'a', 'c', "a", true},
            {'R', 'U', "s", false},
            {'A', 'B', "C", false},
            {'a', 'c', "b ", false},
          });
    }

    private final char lowerBound;
    private final char upperBound;
    private final String tokenValue;
    private final boolean isValid;

    @Test
    public void test() {
      TokenRange pattern = new TokenRange(lowerBound, upperBound);

      assertEquals(isValid, pattern.isValidPattern(tokenValue));
    }
  }

  @AllArgsConstructor
  @RunWith(Parameterized.class)
  public static class GenerateTokenTests {

    @Parameterized.Parameters(name = "{index}: Range({0}, {1}), Seed: {2}, shouldThrow: {4}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {'a', 'z', "0", "a", false},
            {'a', 'z', "10", "k", false},
            {'A', 'Z', "3", "D", false},
            {'0', '9', "4", "4", false},
            {'5', '9', "1", "6", false},
            {'a', 'z', "100", null, true},
          });
    }

    private final char lowerBound;
    private final char upperBound;
    private final String seed;
    private final String expected;
    private final boolean shouldThrow;

    @Test
    public void test() {
      try {
        TokenRange pattern = new TokenRange(lowerBound, upperBound);

        BigInteger bSeed = new BigInteger(seed);

        String actual = pattern.generateToken(bSeed);

        if (shouldThrow) {
          fail(
              "Expected "
                  + GeneratorException.class.getCanonicalName()
                  + " to be thrown but it wasn't");
        }

        assertEquals(expected, actual);
      } catch (GeneratorException e) {
        if (!shouldThrow) {
          throw e;
        }
      }
    }
  }

  @AllArgsConstructor
  @RunWith(Parameterized.class)
  public static class CalculateTokenValueTests {

    @Parameterized.Parameters(name = "{index}: Range({0}, {1}), Token({2})")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {'a', 'z', "a", "0"},
            {'a', 'z', "f", "5"},
            {'A', 'Z', "C", "2"},
            {'0', '9', "4", "4"},
            {'5', '8', "5", "0"},
          });
    }

    private final char lowerBound;
    private final char upperBound;
    private final String tokenValue;
    private final String expectedBigIntValue;

    @Test
    public void test() {
      TokenRange pattern = new TokenRange(lowerBound, upperBound);

      BigInteger expected = new BigInteger(expectedBigIntValue);
      BigInteger actual = pattern.calculateTokenValue(tokenValue);

      assertEquals(expected, actual);
    }
  }

  @AllArgsConstructor
  @RunWith(Parameterized.class)
  public static class ContainsAnyOfTests {

    @Parameterized.Parameters(name = "{index}: Range({0}, {1}), Set({2}), shouldThrow: {3}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {'a', 'z', new Character[] {'A', 'B', 'c'}, true},
            {'A', 'Z', new Character[] {'A', 'B', 'c'}, true},
            {'0', '5', new Character[] {'1', 'B', 'c'}, true},
            {'a', 'z', new Character[] {'A', 'B', 'C'}, false},
            {'3', '8', new Character[] {'1', 'B', 'c'}, false},
            {'a', 'z', new Character[] {}, false},
          });
    }

    private final char lowerBound;
    private final char upperBound;
    private final Character[] characters;
    private final boolean doesContain;

    @Test
    public void test() {
      Set<Character> charSet = Arrays.stream(characters).collect(Collectors.toSet());

      TokenRange pattern = new TokenRange(lowerBound, upperBound);

      assertEquals(doesContain, pattern.containsAnyOf(charSet));
    }
  }

  @AllArgsConstructor
  @RunWith(Parameterized.class)
  public static class CalculatePermutationsTests {

    @Parameterized.Parameters(name = "{index}: Range({0}, {1})")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {'a', 'z', 26},
            {'A', 'Z', 26},
            {'a', 'd', 4},
            {'0', '9', 10},
            {'5', '6', 2},
          });
    }

    private final char lowerBound;
    private final char upperBound;
    private final int permutations;

    @Test
    public void test() {
      TokenRange pattern = new TokenRange(lowerBound, upperBound);

      BigInteger expected = new BigInteger(String.valueOf(permutations));

      assertEquals(expected, pattern.calculatePermutations());
    }
  }

  @AllArgsConstructor
  @RunWith(Parameterized.class)
  public static class ReturnCharactersTests {

    @Parameterized.Parameters(name = "{index}: Range({0}, {1})")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {'0', '9', new Character[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'}},
            {'A', 'F', new Character[] {'A', 'B', 'C', 'D', 'E', 'F'}},
            {'f', 'l', new Character[] {'f', 'g', 'h', 'i', 'j', 'k', 'l'}},
          });
    }

    private final char lowerBound;
    private final char upperBound;
    private final Character[] expectedChars;

    @Test
    public void test() {
      TokenRange pattern = new TokenRange(lowerBound, upperBound);

      Set<Character> expected = Arrays.stream(expectedChars).collect(Collectors.toSet());

      assertEquals(expected, pattern.characters());
    }
  }

  public static class NonParameterizedTests {

    @Test
    public void shouldReturnCorrectTokenLength() {
      TokenRange pattern = new TokenRange('a', 'z');

      assertEquals(1, pattern.tokenLength());
    }
  }
}
