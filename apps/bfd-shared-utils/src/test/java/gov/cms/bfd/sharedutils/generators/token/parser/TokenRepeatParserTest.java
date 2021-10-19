package gov.cms.bfd.sharedutils.generators.token.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import gov.cms.bfd.sharedutils.generators.exceptions.ParsingException;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenPattern;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenRepeat;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import lombok.AllArgsConstructor;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import testing.TestingUtils;

@RunWith(Enclosed.class)
public class TokenRepeatParserTest {

  public static class NonParameterizedTests {

    @Test
    public void shouldParseWithoutCollection() {
      TokenRepeatParser parserSpy = spy(TokenRepeatParser.class);

      TokenPattern expected = mock(TokenPattern.class);

      // unchecked - This is fine for mocking.
      //noinspection unchecked
      Queue<Character> mockStream = mock(Queue.class);

      // unchecked - This is fine for mocking.
      //noinspection unchecked
      doReturn(null).when(parserSpy).parse(any(Queue.class), anyList());

      doReturn(expected).when(parserSpy).parse(mockStream, null);

      TokenPattern actual = parserSpy.parse(mockStream);

      assertSame(expected, actual);
    }

    @Test
    public void shouldParseWithCollection() {
      int repeats = 3;

      TokenRepeatParser parserSpy = spy(TokenRepeatParser.class);

      TokenPattern mockPattern = mock(TokenPattern.class);

      List<TokenPattern> patterns = Collections.singletonList(mockPattern);

      // unchecked - This is fine for mocking.
      //noinspection unchecked
      Queue<Character> mockStream = mock(Queue.class);

      doReturn(false).when(mockStream).isEmpty();

      doReturn('}').when(mockStream).peek();

      // unchecked - This is fine for mocking.
      //noinspection unchecked
      doReturn(-1).when(parserSpy).parseRepeats(any(Queue.class));

      doReturn(repeats).when(parserSpy).parseRepeats(mockStream);

      TokenPattern expected = new TokenRepeat(mockPattern, repeats);

      doReturn(null).when(parserSpy).addAndReturn(anyInt(), anyList());

      doReturn(expected).when(parserSpy).addAndReturn(repeats, patterns);

      TokenPattern actual = parserSpy.parse(mockStream, patterns);

      assertSame(expected, actual);
    }

    @Test(expected = ParsingException.class)
    public void shouldThrowParsExceptionIfStreamEmpty() {
      TokenRepeatParser parserSpy = spy(TokenRepeatParser.class);

      TokenPattern mockPattern = mock(TokenPattern.class);

      List<TokenPattern> patterns = Collections.singletonList(mockPattern);

      // unchecked - This is fine for mocking.
      //noinspection unchecked
      Queue<Character> mockStream = mock(Queue.class);

      doReturn(true).when(mockStream).isEmpty();

      doReturn('}').when(mockStream).peek();

      // unchecked - This is fine for mocking.
      //noinspection unchecked
      doReturn(-1).when(parserSpy).parseRepeats(any(Queue.class));

      doReturn(null).when(parserSpy).addAndReturn(anyInt(), anyList());

      parserSpy.parse(mockStream, patterns);
    }

    @Test(expected = ParsingException.class)
    public void shouldThrowParsExceptionIfNextCharIsNotClosingCurlyBrace() {
      TokenRepeatParser parserSpy = spy(TokenRepeatParser.class);

      TokenPattern mockPattern = mock(TokenPattern.class);

      List<TokenPattern> patterns = Collections.singletonList(mockPattern);

      // unchecked - This is fine for mocking.
      //noinspection unchecked
      Queue<Character> mockStream = mock(Queue.class);

      doReturn(false).when(mockStream).isEmpty();

      doReturn('a').when(mockStream).peek();

      // unchecked - This is fine for mocking.
      //noinspection unchecked
      doReturn(-1).when(parserSpy).parseRepeats(any(Queue.class));

      doReturn(null).when(parserSpy).addAndReturn(anyInt(), anyList());

      parserSpy.parse(mockStream, patterns);
    }

    @Test
    public void shouldCreateNewTokenRepeatWithLastTokenPattern() {
      TokenRepeatParser parser = new TokenRepeatParser();

      int repeats = 3;

      TokenPattern mockPattern = mock(TokenPattern.class);

      doReturn(new BigInteger("2")).when(mockPattern).getTotalPermutations();

      List<TokenPattern> patterns = new ArrayList<>();
      patterns.add(mockPattern);

      TokenPattern expected = new TokenRepeat(mockPattern, repeats);
      expected.init();
      TokenPattern actual = parser.addAndReturn(repeats, patterns);

      assertEquals(expected, actual);
    }

    @Test
    public void shouldCreateNewTokenRepeatWithNullTokenPattern() {
      TokenRepeatParser parser = new TokenRepeatParser();

      int repeats = 3;

      TokenPattern expected = new TokenRepeat(null, repeats);
      TokenPattern actual = parser.addAndReturn(repeats, null);

      assertEquals(expected, actual);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowIllegalStateExceptionIfPatternListIsEmpty() {
      TokenRepeatParser parser = new TokenRepeatParser();

      parser.addAndReturn(3, Collections.emptyList());
    }

    @Test
    public void shouldInvokeParseWithPatterns() {
      TokenRepeatParser parserSpy = spy(TokenRepeatParser.class);

      // unchecked - This is fine for mocking.
      //noinspection unchecked
      doReturn(null).when(parserSpy).parse(any(Queue.class), anyList());

      // unchecked - This is fine for mocking.
      //noinspection unchecked
      Queue<Character> mockStream = mock(Queue.class);

      List<TokenPattern> patterns = Collections.singletonList(mock(TokenPattern.class));

      parserSpy.parseAndAdd(mockStream, patterns);

      verify(parserSpy, times(1)).parse(mockStream, patterns);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowIllegalStateExceptionIfPatternsListIsEmpty() {
      TokenRepeatParser parserSpy = spy(TokenRepeatParser.class);

      // unchecked - This is fine for mocking.
      //noinspection unchecked
      doReturn(null).when(parserSpy).parse(any(Queue.class), anyList());

      // unchecked - This is fine for mocking.
      //noinspection unchecked
      Queue<Character> mockStream = mock(Queue.class);

      parserSpy.parseAndAdd(mockStream, Collections.emptyList());
    }
  }

  @AllArgsConstructor
  @RunWith(Parameterized.class)
  public static class ParseRepeatsTests {

    // This value represents one number higher than the allowed maximum definable repeats.
    private static final long TOO_BIG = ((long) Integer.MAX_VALUE) + 1;

    @Parameterized.Parameters(name = "{index}: Stream(\"{0}\"), shouldThrow: {2}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {"12}", 12, false},
            {"122a}", 122, false},
            {TOO_BIG + "}", 0, true},
            {"0}", 0, true},
            {"-1}", 0, true},
            {"a}", 0, true},
          });
    }

    private final String stream;
    private final int expected;
    private final boolean shouldThrow;

    @Test
    public void test() {
      TokenRepeatParser parser = new TokenRepeatParser();

      Queue<Character> patternStream = TestingUtils.toCharQueue(stream);

      try {
        int actual = parser.parseRepeats(patternStream);

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
