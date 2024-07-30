package gov.cms.bfd.data.npi.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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

  /** Setup Before Each test method. */
  @BeforeEach
  void setup() throws IOException {
    StringBuilder initialString = new StringBuilder();
    InputStream npiDataStream = new ByteArrayInputStream(initialString.toString().getBytes());
    npiOrgDataLookup = new NPIOrgLookup(npiDataStream);
    npiOrgDisplay = Optional.empty();

    Map<String, String> npiOrgHashMap = new HashMap<>();
    npiOrgHashMap.put(NPIOrgLookup.FAKE_NPI_NUMBER, NPIOrgLookup.FAKE_NPI_ORG_NAME);

    npiOrgDataLookup.npiOrgHashMap = npiOrgHashMap;
  }

  /** Return Fake NPI Org. */
  @Test
  public void shouldReturnFakeOrgData() throws IOException {
    npiOrgDisplay =
        npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(NPIOrgLookup.FAKE_NPI_NUMBER));
    assertNotEquals(null, npiOrgDisplay.get());
  }

  /** Return Fake NPI Org Name. */
  @Test
  public void shouldReturnFakeNPIOrgName() throws IOException {
    npiOrgDisplay =
        npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(NPIOrgLookup.FAKE_NPI_NUMBER));
    assertEquals(NPIOrgLookup.FAKE_NPI_ORG_NAME, npiOrgDisplay.get());
  }

  /** Should not return Org Name and NPI Number is empty. */
  @Test
  public void shouldNotReturnWhenNPINumberIsEmpty() throws IOException {
    npiOrgDisplay = npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.empty());
    assertEquals(false, npiOrgDisplay.isPresent());
  }

  /** Should not return Org Name and NPI Number is empty string. */
  @Test
  public void shouldNotReturnWhenNPINumberIEmptyString() throws IOException {
    npiOrgDisplay = npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(""));
    assertEquals(false, npiOrgDisplay.isPresent());
  }

  /**
   * Should Return Map When Input Stream Is Formatted with two columns, the npi number and the npi
   * org name.
   */
  @Test
  public void shouldReturnMapWhenInputStreamIsFormattedCorrectly() throws IOException {
    StringBuilder initialString = new StringBuilder();
    initialString.append(npiOrgDataLookup.FAKE_NPI_NUMBER);
    initialString.append("\t");
    initialString.append(npiOrgDataLookup.FAKE_NPI_ORG_NAME);

    InputStream targetStream = new ByteArrayInputStream(initialString.toString().getBytes());
    Map<String, String> npiOrgMap = npiOrgDataLookup.readNPIOrgDataStream(targetStream);
    assertFalse(isNullOrEmptyMap(npiOrgMap));
    assertEquals(
        npiOrgDataLookup.FAKE_NPI_ORG_NAME, npiOrgMap.get(npiOrgDataLookup.FAKE_NPI_NUMBER));
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
