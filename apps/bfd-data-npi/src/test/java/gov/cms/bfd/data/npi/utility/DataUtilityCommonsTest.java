package gov.cms.bfd.data.npi.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Data Utility Commons Test. */
public class DataUtilityCommonsTest {
  /** A fake Taxonomy Code. */
  private static final String TAXONOMY_CODE = "207X00000X";

  /** A fake Taxonomy Display. */
  private static final String TAXONOMY_DISPLAY = "Orthopaedic Surgery";

  /** This logic is used when parsing the header for the NPI files. */
  @Test
  public void getsTheMapForTheNpiFileHeaderLine() {
    String[] fields = new String[] {"field1", "field2", "field3", "field4", "field5"};
    Map<String, Integer> mapOfIndexResults = DataUtilityCommons.getIndexNumbers(fields);

    for (int i = fields.length - 1; i > 0; i--) {
      assertEquals(i, mapOfIndexResults.get(fields[i]));
    }
  }

  /**
   * Tests a map with indexes for each field is returned from getIndexNumbers() when a list of
   * fields is supplied.
   */
  @Test
  public void getIndexNumbersReturnsAllIndexesWhenPassedMultipleFields() {

    String fieldToFind = "field1";

    Map<String, Integer> mapOfIndexResults =
        new HashMap<String, Integer>() {
          {
            put(fieldToFind, 1);
            put("field2", 2);
          }
        };
    assertEquals(
        mapOfIndexResults.get(fieldToFind),
        DataUtilityCommons.getIndexNumberForField(mapOfIndexResults, fieldToFind));
  }

  /** This test ensures the proper loading of the taxonomy codes and descriptions. */
  @Test
  public void taxonomyDescriptionsProperlyLoad() throws IOException {
    Map<String, String> taxonomyMap = DataUtilityCommons.processTaxonomyDescriptions();
    assertEquals(TAXONOMY_DISPLAY, taxonomyMap.get(TAXONOMY_CODE));
  }

  /**
   * This tests makes sure to return a exception when a field in a map of header fields cannot be
   * found.
   */
  @Test
  public void getExceptionWhenFieldCantBeFoundInMap() {

    Map<String, Integer> mapOfIndexResults =
        new HashMap<String, Integer>() {
          {
            put("field1", 1);
            put("field2", 2);
          }
        };

    Exception exception =
        assertThrows(
            IllegalStateException.class,
            () -> {
              DataUtilityCommons.getIndexNumberForField(mapOfIndexResults, "field3");
            });

    String expectedMessage = "NPI Org File Processing Error: Cannot field fieldname field3";
    assertEquals(expectedMessage, exception.getMessage());
  }
}
