package gov.cms.bfd.sharedutils.generators.token.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
public class TokenSingletonParserTest {

  // This value represents one number higher than the allowed maximum definable repeats.
  private static final long TOO_BIG = ((long) Integer.MAX_VALUE) + 1;

  @AllArgsConstructor
  @RunWith(Parameterized.class)
  public static class ParseTests {

    @Parameterized.Parameters(name = "{index}: Stream(\"{0}\"), shouldThrow: {2}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {"a", new TokenSingleton('a'), false},
            {"@", new TokenSingleton('@'), false},
            {"4", new TokenSingleton('4'), false},
            {" ", new TokenSingleton(' '), false},
            {"\\\\", new TokenSingleton('\\'), false},
            {"\\d", new TokenRange('0', '9'), false},
            {"\\[", new TokenSingleton('['), false},
            {"\\]", new TokenSingleton(']'), false},
            {"\\{", new TokenSingleton('{'), false},
            {"\\}", new TokenSingleton('}'), false},
            {"\\-", new TokenSingleton('-'), false},
            {"\\@", null, true},
            {"\\", null, true},
          });
    }

    private final String stream;
    private final TokenPattern expected;
    private final boolean shouldThrow;

    @Test
    public void test() {
      TokenSingletonParser parser = new TokenSingletonParser();

      Queue<Character> patternStream = TestingUtils.toCharQueue(stream);

      try {
        TokenPattern actual = parser.parse(patternStream);

        if (shouldThrow) {
          fail(
              "Expected "
                  + ParsingException.class.getCanonicalName()
                  + " to be thrown but it wasn't");
        }

        expected.init();

        assertEquals(expected, actual);
      } catch (ParsingException e) {
        if (!shouldThrow) {
          throw e;
        }
      }
    }
  }
}
