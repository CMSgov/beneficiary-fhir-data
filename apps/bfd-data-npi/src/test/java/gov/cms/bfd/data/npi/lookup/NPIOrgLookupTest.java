package gov.cms.bfd.data.npi.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
    npiOrgDataLookup = NPIOrgLookup.createNpiOrgLookupForTesting();
    npiOrgDisplay = Optional.empty();
  }

  /** Return Fake NPI Org Data when the parameter bfdServer.include.fake.drug.code is true. */
  @Test
  public void shouldReturnFakeOrgDataWhenConstructorSetToTrue() throws IOException {
    npiOrgDisplay =
        npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(NPIOrgLookup.FAKE_NPI_NUMBER));
    assertNotEquals(null, npiOrgDisplay.get());
  }

  /** Return Fake NPI Org Name when the parameter bfdServer.include.fake.drug.code is true. */
  @Test
  public void shouldReturnFakeNPIOrgNameWhenConstructorSetToTrue() throws IOException {
    npiOrgDisplay =
        npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(NPIOrgLookup.FAKE_NPI_NUMBER));
    assertEquals(NPIOrgLookup.FAKE_NPI_ORG_NAME, npiOrgDisplay.get());
  }

  /**
   * Should not return Org Name when the parameter bfdServer.include.fake.drug.code is true and NPI
   * Number is empty.
   */
  @Test
  public void shouldNotReturnWhenNPINumberIsEmptyAndWhenConstructorSetToTrue() throws IOException {
    npiOrgDisplay = npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.empty());
    assertEquals(false, npiOrgDisplay.isPresent());
  }

  /**
   * Should not return Org Name when the parameter bfdServer.include.fake.drug.code is true and NPI
   * Number is empty string.
   */
  @Test
  public void shouldNotReturnWhenNPINumberIEmptyStringAndWhenConstructorSetToTrue()
      throws IOException {
    npiOrgDisplay = npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(""));
    assertEquals(false, npiOrgDisplay.isPresent());
  }

  /** Should Return Map When Input Stream Is Formatted Correctly. */
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

  /** Should Not Return Map When Input Stream Is Not Formatted Correctly. */
  @Test
  public void shouldReturnMapWhenInputStreamIsNotFormattedCorrectly() throws IOException {
    StringBuilder initialString = new StringBuilder();
    initialString.append(npiOrgDataLookup.FAKE_NPI_NUMBER);
    initialString.append("\t");
    initialString.append(npiOrgDataLookup.FAKE_NPI_ORG_NAME);
    initialString.append("\t");
    initialString.append("Extra Org");

    InputStream targetStream = new ByteArrayInputStream(initialString.toString().getBytes());
    Map<String, String> npiOrgMap = npiOrgDataLookup.readNPIOrgDataStream(targetStream);
    assertTrue(isNullOrEmptyMap(npiOrgMap));
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
