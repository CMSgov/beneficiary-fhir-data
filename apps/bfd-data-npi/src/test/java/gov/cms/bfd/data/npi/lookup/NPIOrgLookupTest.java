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

  /**
   * A fake npi number.
   */
  public static final String FAKE_NPI_NUMBER = "0000000000";

  /**
   * A fake org name display that is associated with the FAKE_NPI_ORG_NAME.
   */
  public static final String FAKE_NPI_ORG_NAME = "Fake ORG Name";

  /**
   * Setup Before Each test method.
   */
  @BeforeEach
  void setup() throws IOException {
    InputStream npiDataStream = new ByteArrayInputStream("".getBytes());
    npiOrgDataLookup = new NPIOrgLookup(npiDataStream);
    npiOrgDisplay = Optional.empty();

    Map<String, String> npiOrgHashMap = new HashMap<>();
    npiOrgHashMap.put(FAKE_NPI_NUMBER, FAKE_NPI_ORG_NAME);

    npiOrgDataLookup.npiOrgHashMap = npiOrgHashMap;
  }

  /** Return Fake NPI Org. */
  @Test
  public void shouldReturnFakeOrgData() throws IOException {
    npiOrgDisplay =
            npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(FAKE_NPI_NUMBER));
    assertNotEquals(null, npiOrgDisplay.get());
  }

  /** Return Fake NPI Org Name. */
  @Test
  public void shouldReturnFakeNPIOrgName() throws IOException {
    npiOrgDisplay =
            npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(FAKE_NPI_NUMBER));
    assertEquals(FAKE_NPI_ORG_NAME, npiOrgDisplay.get());
  }

  /** Should not return Org Name and NPI Number is empty. */
  @Test
  public void shouldNotReturnWhenNPINumberIsEmpty() throws IOException {
    npiOrgDisplay = npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.empty());
    assertFalse(npiOrgDisplay.isPresent());
  }

  /** Should not return Org Name and NPI Number is empty string. */
  @Test
  public void shouldNotReturnWhenNPINumberIEmptyString() throws IOException {
    npiOrgDisplay = npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(""));
    assertFalse(npiOrgDisplay.isPresent());
  }

  /**
   * Should Return Map When Input Stream Is Formatted with two columns, the npi number and the npi
   * org name.
   */
  @Test
  public void shouldReturnMapWhenInputStreamIsFormattedCorrectly() throws IOException {
    String initialString = FAKE_NPI_NUMBER +
            "\t" +
            FAKE_NPI_ORG_NAME;

    InputStream targetStream = new ByteArrayInputStream(initialString.getBytes());
    Map<String, String> npiOrgMap = npiOrgDataLookup.readNPIOrgDataStream(targetStream);
    assertFalse(isNullOrEmptyMap(npiOrgMap));
    assertEquals(
            FAKE_NPI_ORG_NAME, npiOrgMap.get(FAKE_NPI_NUMBER));
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
