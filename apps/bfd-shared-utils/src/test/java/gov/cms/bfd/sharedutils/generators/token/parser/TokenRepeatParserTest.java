package gov.cms.bfd.sharedutils.generators.token.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import gov.cms.bfd.sharedutils.generators.exceptions.ParsingException;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenPattern;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenRepeat;
import java.util.Arrays;
import java.util.Queue;
import lombok.AllArgsConstructor;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import testing.TestingUtils;

@RunWith(Enclosed.class)
public class TokenRepeatParserTest {

  // This value represents one number higher than the allowed maximum definable repeats.
  private static final long TOO_BIG = ((long) Integer.MAX_VALUE) + 1;

  @AllArgsConstructor
  @RunWith(Parameterized.class)
  public static class ParseTests {

    @Parameterized.Parameters(name = "{index}: Stream(\"{0}\"), shouldThrow: {2}")
    public static Iterable<Object[]> data() {
      return Arrays.asList(
          new Object[][] {
            {"{12}", 12, false},
            {"{122}", 122, false},
            {"{" + TOO_BIG + "}", 0, true},
            {"{0}", 0, true},
            {"{-1}", 0, true},
            {"{a}", 0, true},
            {"{12", 0, true},
            {"{", 0, true},
            {"{}", 0, true},
          });
    }

    private final String stream;
    private final long repeats;
    private final boolean shouldThrow;

    @Test
    public void test() {
      TokenRepeatParser parser = new TokenRepeatParser();

      Queue<Character> patternStream = TestingUtils.toCharQueue(stream);

      try {
        TokenPattern actual = parser.parse(patternStream);
        TokenPattern expected = new TokenRepeat((int) repeats);

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
