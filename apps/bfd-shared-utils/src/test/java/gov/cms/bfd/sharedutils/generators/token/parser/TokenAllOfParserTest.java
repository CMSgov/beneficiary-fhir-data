package gov.cms.bfd.sharedutils.generators.token.parser;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import gov.cms.bfd.sharedutils.generators.token.pattern.TokenAllOf;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenPattern;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class TokenAllOfParserTest {

  @Test
  public void shouldCreateArrayListInstance() {
    TokenAllOfParser parser = new TokenAllOfParser();

    List<TokenPattern> actual = parser.createCollection();

    assertTrue(actual instanceof ArrayList);
  }

  @Test
  public void shouldCreateNewInstanceOfList() {
    TokenAllOfParser parser = new TokenAllOfParser();

    List<TokenPattern> instanceA = parser.createCollection();
    List<TokenPattern> instanceB = parser.createCollection();

    assertNotSame(instanceA, instanceB);
  }

  @Test
  public void shouldCreateAndInitializeTokenPattern() {
    TokenAllOfParser parserSpy = spy(TokenAllOfParser.class);

    // unchecked - This is fine for creating a mock.
    //noinspection unchecked
    List<TokenPattern> mockPatterns = mock(List.class);

    TokenPattern expected = mock(TokenPattern.class);

    doReturn(expected).when(parserSpy).newTokenAllOf(mockPatterns);

    TokenPattern actual = parserSpy.createTokenPattern(mockPatterns);

    assertSame(expected, actual);
    verify(expected, times(1)).init();
  }

  @Test
  public void shouldCreateTokenAllOfInstance() {
    TokenAllOfParser parser = new TokenAllOfParser();

    TokenPattern actual = parser.newTokenAllOf(Collections.emptyList());

    assertTrue(actual instanceof TokenAllOf);
  }

  @Test
  public void shouldCreateNewTokenInstance() {
    TokenAllOfParser parser = new TokenAllOfParser();

    TokenPattern instanceA = parser.newTokenAllOf(Collections.emptyList());
    TokenPattern instanceB = parser.newTokenAllOf(Collections.emptyList());

    assertNotSame(instanceA, instanceB);
  }
}
