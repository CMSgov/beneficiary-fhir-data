package gov.cms.bfd.sharedutils.generators.token.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import gov.cms.bfd.sharedutils.generators.exceptions.ParsingException;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenPattern;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenRange;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenSingleton;
import java.util.Arrays;
import java.util.Queue;
import lombok.AllArgsConstructor;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import testing.TestingUtils;

@RunWith(Enclosed.class)
public class TokenRangeParserTest {

  public static class ParseTests {

    @Test
    public void shouldCreateATokenSingletonIfNoHyphen() {
      TokenRangeParser parserSpy = spy(TokenRangeParser.class);

      Queue<Character> patternStream = TestingUtils.toCharQueue("AB");

      // unchecked - This is fine for creating a mock.
      //noinspection unchecked
      doReturn(null).when(parserSpy).parseRange(anyChar(), any(Queue.class));

      TokenPattern expected = new TokenSingleton('A');
      expected.init(); // This also ensures the tested code invoked init()
      TokenPattern actual = parserSpy.parse(patternStream);

      assertEquals(expected, actual);
    }

    @Test
    public void shouldCreateATokenSingletonIfEndOfStream() {
      TokenRangeParser parserSpy = spy(TokenRangeParser.class);

      Queue<Character> patternStream = TestingUtils.toCharQueue("A");

      // unchecked - This is fine for creating a mock.
      //noinspection unchecked
      doReturn(null).when(parserSpy).parseRange(anyChar(), any(Queue.class));

      TokenPattern expected = new TokenSingleton('A');
      expected.init(); // This also ensures the tested code invoked init()
      TokenPattern actual = parserSpy.parse(patternStream);

      assertEquals(expected, actual);
    }

    @Test
    public void shouldCreateATokenRangeIfHasHyphen() {
      TokenRangeParser parserSpy = spy(TokenRangeParser.class);

      Queue<Character> patternStream = TestingUtils.toCharQueue("A-B");

      Queue<Character> expectedStream = TestingUtils.toCharQueue("-B");

      TokenPattern expected = mock(TokenPattern.class);

      doReturn(expected).when(parserSpy).parseRange('A', expectedStream);

      TokenPattern actual = parserSpy.parse(patternStream);

      assertEquals(expected, actual);
      verify(expected, times(1)).init();
    }
  }

  @AllArgsConstructor
  @RunWith(Parameterized.class)
  public static class ParseRangeTests {

    @Parameterized.Parameters(name = "{index}: Range({0}, {1}), shouldThrow: {5}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {'a', 'z', "-z", 'a', 'z', false},
            {'A', 'Z', "-Z", 'A', 'Z', false},
            {'0', '9', "-9", '0', '9', false},
            {'a', 'z', "-", 'a', 'z', true},
          });
    }

    private final char lowerBound;
    private final char upperBound;
    private final String stream;
    private final char minBound;
    private final char maxBound;
    private final boolean shouldThrow;

    @Test
    public void test() {
      try {
        TokenRangeParser parserSpy = spy(TokenRangeParser.class);

        Queue<Character> patternStream = TestingUtils.toCharQueue(stream);

        TokenPattern expected = mock(TokenPattern.class);

        doReturn(null)
            .when(parserSpy)
            .parseWithBoundsCheck(anyChar(), anyChar(), anyChar(), anyChar());

        doReturn(expected)
            .when(parserSpy)
            .parseWithBoundsCheck(lowerBound, upperBound, minBound, maxBound);

        TokenPattern actual = parserSpy.parseRange(lowerBound, patternStream);

        if (shouldThrow) {
          fail(
              "Expected "
                  + ParsingException.class.getCanonicalName()
                  + " to be thrown but it wasn't");
        }

        assertSame(expected, actual);
      } catch (ParsingException e) {
        if (!shouldThrow) {
          throw e;
        }
      }
    }
  }

  @AllArgsConstructor
  @RunWith(Parameterized.class)
  public static class ParseWithBoundsCheckTests {

    @Parameterized.Parameters(name = "{index}: Range({0}, {1}), shouldThrow: {4}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {'a', 'z', 'a', 'z', false},
            {'A', 'Z', 'A', 'Z', false},
            {'0', '9', '0', '9', false},
            {'f', 'p', 'a', 'z', false},
            {'G', 'O', 'A', 'Z', false},
            {'4', '7', '0', '9', false},
            {'a', 'a', 'A', 'Z', true},
            {'a', 'b', 'A', 'Z', true},
            {'a', 'Z', 'A', 'Z', true},
            {'o', 'f', 'a', 'z', true},
            {'R', 'D', 'A', 'Z', true},
            {'5', '3', '0', '9', true},
            {'a', ']', 'a', 'z', true},
            {'A', ' ', 'A', 'Z', true},
            {'A', 'A', 'A', 'Z', true},
          });
    }

    private final char lowerBound; // Requested lower bound
    private final char upperBound; // Requested upper bound
    private final char minBound; // Lower bound limit
    private final char maxBound; // Upper bound limit
    private final boolean shouldThrow;

    @Test
    public void test() {
      TokenRangeParser parser = new TokenRangeParser();

      try {
        TokenPattern actual =
            parser.parseWithBoundsCheck(lowerBound, upperBound, minBound, maxBound);
        TokenPattern expected = new TokenRange(lowerBound, upperBound);

        if (shouldThrow) {
          fail(
              "Expected "
                  + ParsingException.class.getCanonicalName()
                  + " to be thrown but it wasn't");
        }

        assertEquals(expected, actual);
      } catch (ParsingException e) {
        if (!shouldThrow) {
          throw e;
        }
      }
    }
  }
}
