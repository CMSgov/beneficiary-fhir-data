package gov.cms.bfd.sharedutils.generators.token.parser;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import gov.cms.bfd.sharedutils.generators.token.pattern.TokenOneOf;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenPattern;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class TokenOneOfParserTest {

  @Test
  public void shouldCreateArrayListInstance() {
    TokenOneOfParser parser = new TokenOneOfParser();

    List<TokenPattern> actual = parser.createCollection();

    assertTrue(actual instanceof ArrayList);
  }

  @Test
  public void shouldCreateNewInstanceOfList() {
    TokenOneOfParser parser = new TokenOneOfParser();

    List<TokenPattern> instanceA = parser.createCollection();
    List<TokenPattern> instanceB = parser.createCollection();

    assertNotSame(instanceA, instanceB);
  }

  @Test
  public void shouldCreateAndInitializeTokenPattern() {
    TokenOneOfParser parserSpy = spy(TokenOneOfParser.class);

    // unchecked - This is fine for creating a mock.
    //noinspection unchecked
    List<TokenPattern> mockPatterns = mock(List.class);

    TokenPattern expected = mock(TokenPattern.class);

    doReturn(expected).when(parserSpy).newTokenOneOf(mockPatterns);

    TokenPattern actual = parserSpy.createTokenPattern(mockPatterns);

    assertSame(expected, actual);
    verify(expected, times(1)).init();
  }

  @Test
  public void shouldCreateTokenAllOfInstance() {
    TokenOneOfParser parser = new TokenOneOfParser();

    TokenPattern actual = parser.newTokenOneOf(Collections.emptyList());

    assertTrue(actual instanceof TokenOneOf);
  }

  @Test
  public void shouldCreateNewTokenInstance() {
    TokenOneOfParser parser = new TokenOneOfParser();

    TokenPattern instanceA = parser.newTokenOneOf(Collections.emptyList());
    TokenPattern instanceB = parser.newTokenOneOf(Collections.emptyList());

    assertNotSame(instanceA, instanceB);
  }
}
