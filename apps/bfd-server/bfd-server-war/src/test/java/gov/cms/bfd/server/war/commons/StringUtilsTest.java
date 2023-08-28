package gov.cms.bfd.server.war.commons;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.Test;

/** Unit tests for {@link StringUtils}. */
public class StringUtilsTest {

  /** Test to verify veracity. of customSplit function. */
  @Test
  public void testCustomSplit() {
    final String inputString = " header1, header2 ,  header3 , header4 ";
    String[] expected = {"header1", "header2", "header3", "header4"};
    String[] response = StringUtils.splitOnCommas(inputString);
    assertArrayEquals(expected, response);
  }
}
