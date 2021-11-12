package gov.cms.bfd.server.war.commons;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;

public class AbstractSamhsaMatcherTest {

  @Test
  public void shouldReturnColumnValues() {
    List<String> expected = List.of("1", "2", "3", "4");
    List<String> actual = AbstractSamhsaMatcher.resourceCsvColumnToList("test_file.csv", "columnB");

    assertEquals(expected, actual);
  }
}
