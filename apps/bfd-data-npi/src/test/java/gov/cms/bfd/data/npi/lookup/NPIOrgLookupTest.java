package gov.cms.bfd.data.npi.lookup;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.bfd.data.npi.dto.NPIData;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** NPI org lookup test. */
public class NPIOrgLookupTest {

  /** Global variable for NPILookup. */
  NPIOrgLookup npiOrgDataLookup;

  /** Global variable for npiOrgDisplay. */
  Optional<String> npiOrgDisplay;

  /** A fake npi number. */
  public static final String FAKE_NPI_NUMBER = "0000000000";

  /** A fake Practitioner NPI. */
  public static final String FAKE_PRACTITIONER_NPI = "1111111111";

  /** A fake org name display that is associated with the FAKE_NPI_ORG_NAME. */
  public static final String FAKE_NPI_ORG_NAME = "Fake ORG Name";

  /** A fake taxonomy code. */
  public static final String FAKE_TAXONOMY_CODE = "0000000X";

  /** A fake taxonomy display. */
  public static final String FAKE_TAXONOMY_DISPLAY = "Fake Taxonomy";

  /** Test JSON, which will be deserialized. */
  private static final String testJson =
      String.format(
          " { \"npi\": \"%s\","
              + " \"entityTypeCode\": \"2\","
              + " \"providerOrganizationName\": \"%s\","
              + " \"taxonomyCode\": \"%s\","
              + " \"taxonomyDisplay\": \"%s\","
              + " \"providerNamePrefix\": \"Dr\","
              + " \"providerFirstName\": \"Stephen\","
              + " \"providerMiddleName\": \"J.\","
              + " \"providerLastName\": \"Smith\","
              + " \"providerNameSuffix\": \"Sr.\","
              + " \"providerCredential\": \"MD\""
              + " }",
          FAKE_NPI_NUMBER, FAKE_NPI_ORG_NAME, FAKE_TAXONOMY_CODE, FAKE_TAXONOMY_DISPLAY);

  /** Setup Before Each test method. */
  @BeforeEach
  void setup() throws IOException {
    npiOrgDisplay = Optional.empty();

    Map<String, String> npiOrgHashMap = new HashMap<>();
    NPIData fakeNpiData =
        NPIData.builder()
            .npi(FAKE_NPI_NUMBER)
            .entityTypeCode("2")
            .providerOrganizationName(FAKE_NPI_ORG_NAME)
            .taxonomyCode(FAKE_TAXONOMY_CODE)
            .taxonomyDisplay(FAKE_TAXONOMY_DISPLAY)
            .providerNamePrefix("Dr.")
            .providerFirstName("Stephen")
            .providerMiddleName("J.")
            .providerLastName("Smith")
            .providerNameSuffix("Sr.")
            .providerCredential("MD")
            .build();
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(fakeNpiData);
    npiOrgHashMap.put(FAKE_NPI_NUMBER, json);
    npiOrgDataLookup = new NPIOrgLookup(npiOrgHashMap);
  }

  /** Should Return taxonomy. */
  @Test
  public void shouldReturnTaxonomy() {
    Optional<NPIData> npiData =
        npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(FAKE_NPI_NUMBER));
    assertTrue(npiData.isPresent());
    assertEquals(FAKE_TAXONOMY_CODE, npiData.get().getTaxonomyCode());
    assertEquals(FAKE_TAXONOMY_DISPLAY, npiData.get().getTaxonomyDisplay());
  }

  /** Return Fake NPI Org. */
  @Test
  public void shouldReturnFakeOrgData() throws IOException {
    Optional<NPIData> npiData =
        npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(FAKE_NPI_NUMBER));
    assertTrue(npiData.isPresent());
  }

  /** Return Fake NPI Org Name. */
  @Test
  public void shouldReturnFakeNPIOrgName() throws IOException {
    Optional<NPIData> npiData =
        npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(FAKE_NPI_NUMBER));
    assertEquals(FAKE_NPI_ORG_NAME, npiData.get().getProviderOrganizationName());
  }

  /** Should not return Org Name and NPI Number is empty. */
  @Test
  public void shouldNotReturnWhenNPINumberIsEmpty() throws IOException {
    Optional<NPIData> npiData = npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.empty());
    assertFalse(npiData.isPresent());
  }

  /** Should not return Org Name and NPI Number is empty string. */
  @Test
  public void shouldNotReturnWhenNPINumberIEmptyString() throws IOException {
    Optional<NPIData> npiData = npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(""));
    assertFalse(npiData.isPresent());
  }

  /**
   * Should Return Map When Input Stream Is Formatted with two columns, the npi number and the npi
   * org name.
   */
  @Test
  public void shouldReturnMapWhenInputStreamIsFormattedCorrectly() throws IOException {

    InputStream targetStream = new ByteArrayInputStream(testJson.getBytes());
    Map<String, String> npiOrgMap = npiOrgDataLookup.readNPIOrgDataStream(targetStream);
    assertFalse(isNullOrEmptyMap(npiOrgMap));
    assertTrue(npiOrgMap.containsKey(FAKE_NPI_NUMBER));
  }

  /**
   * Check to see if a Map is empty or null.
   *
   * @param map being passed in to test whether null or empty
   * @return boolean of whether the map is empty or null
   */
  private static boolean isNullOrEmptyMap(Map<?, ?> map) {
    return (map == null || map.isEmpty());
  }
}
