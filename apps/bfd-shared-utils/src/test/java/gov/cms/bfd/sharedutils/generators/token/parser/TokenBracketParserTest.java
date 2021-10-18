package gov.cms.bfd.sharedutils.generators.token.parser;

import static org.junit.Assert.assertNotSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import gov.cms.bfd.sharedutils.generators.exceptions.ParsingException;
import java.util.Queue;
import org.junit.Test;
import testing.TestingUtils;

public class TokenBracketParserTest {

  @Test
  public void shouldCorrectlyInvokeOneOfParser() {
    TokenBracketParser parserSpy = spy(TokenBracketParser.class);

    String pattern = "[AB[0\\-9]Z]";
    String bracketPattern = pattern.substring(1, pattern.length() - 1);

    Queue<Character> patternStream = TestingUtils.toCharQueue(pattern);
    Queue<Character> bracketPatternStream = TestingUtils.toCharQueue(bracketPattern);

    TokenOneOfParser mockParser = mock(TokenOneOfParser.class);

    doReturn(mockParser).when(parserSpy).newTokenOneOfParser();

    parserSpy.parse(patternStream);

    verify(mockParser, times(1)).parse(bracketPatternStream);
  }

  @Test(expected = ParsingException.class)
  public void shouldThrowParsingExceptionIfOddBracketCount() {
    TokenBracketParser parserSpy = spy(TokenBracketParser.class);

    String pattern = "[AB[0\\-9]";

    Queue<Character> patternStream = TestingUtils.toCharQueue(pattern);

    TokenOneOfParser mockParser = mock(TokenOneOfParser.class);

    doReturn(mockParser).when(parserSpy).newTokenOneOfParser();

    parserSpy.parse(patternStream);
  }

  @Test(expected = ParsingException.class)
  public void shouldThrowParsingExceptionIfEndOfStreamEarly() {
    TokenBracketParser parserSpy = spy(TokenBracketParser.class);

    String pattern = "[AB[0\\-9]a";

    Queue<Character> patternStream = TestingUtils.toCharQueue(pattern);

    TokenOneOfParser mockParser = mock(TokenOneOfParser.class);

    doReturn(mockParser).when(parserSpy).newTokenOneOfParser();

    parserSpy.parse(patternStream);
  }

  @Test(expected = ParsingException.class)
  public void shouldThrowParsingExceptionIfEmptyBracketGroup() {
    TokenBracketParser parserSpy = spy(TokenBracketParser.class);

    String pattern = "[]";

    Queue<Character> patternStream = TestingUtils.toCharQueue(pattern);

    TokenOneOfParser mockParser = mock(TokenOneOfParser.class);

    doReturn(mockParser).when(parserSpy).newTokenOneOfParser();

    parserSpy.parse(patternStream);
  }

  @Test
  public void shouldCreateNewInstanceOfTokenOneOfParser() {
    TokenBracketParser parser = new TokenBracketParser();

    TokenOneOfParser instanceA = parser.newTokenOneOfParser();
    TokenOneOfParser instanceB = parser.newTokenOneOfParser();

    assertNotSame(instanceA, instanceB);
  }
}
