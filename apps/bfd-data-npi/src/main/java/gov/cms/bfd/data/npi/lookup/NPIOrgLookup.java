package gov.cms.bfd.data.npi.lookup;

import gov.cms.bfd.data.npi.utility.App;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides an NPI org lookup. */
public class NPIOrgLookup {
  private static final Logger LOGGER = LoggerFactory.getLogger(NPIOrgLookup.class);

  /** A fake npi number used for testing. */
  public static final String FAKE_NPI_NUMBER = "0000000000";

  /** A fake org name display that is associated with the FAKE_NPI_ORG_NAME. */
  public static final String FAKE_NPI_ORG_NAME = "Fake ORG Name";

  /** Hashmap to keep the org names. */
  private Map<String, String> npiOrgHashMap = new HashMap<>();

  /** A field to return the production org lookup. */
  private static NPIOrgLookup npiOrgLookupForProduction;

  /**
   * Factory method for creating a {@link NPIOrgLookup } for production that does not include the
   * fake org name.
   *
   * @throws IOException if there is an issue reading file
   * @return the {@link NPIOrgLookup }
   */
  public static NPIOrgLookup createNpiOrgLookup() throws IOException {
    if (npiOrgLookupForProduction == null) {
      InputStream npiDataStream = getFileInputStream(App.NPI_RESOURCE);
      npiOrgLookupForProduction = new NPIOrgLookup(npiDataStream);
    }

    return npiOrgLookupForProduction;
  }

  /** Constructs an {@link NPIOrgLookup} used for testing purposes only. */
  public NPIOrgLookup() {
    npiOrgHashMap.put(FAKE_NPI_NUMBER, FAKE_NPI_ORG_NAME);
  }

  /**
   * Constructs an {@link NPIOrgLookup} used for testing purposes only.
   *
   * @param npiOrgMap map for test data
   */
  public NPIOrgLookup(Map<String, String> npiOrgMap) {
    npiOrgHashMap.putAll(npiOrgMap);
  }

  /**
   * Constructs an {@link NPIOrgLookup}.
   *
   * @param npiDataStream input stream for npi org
   * @throws IOException if there is an issue reading file
   */
  public NPIOrgLookup(InputStream npiDataStream) throws IOException {
    npiOrgHashMap = readNPIOrgDataStream(npiDataStream);
  }

  /**
   * Retrieves the Org Data for NPI Display for the given NPI number using the npi file downloaded
   * during the build.
   *
   * @param npiNumber - npiNumber value in claim records
   * @return the npi org data display string
   */
  public Optional<String> retrieveNPIOrgDisplay(Optional<String> npiNumber) {
    /*
     * Handle bad data (e.g. our random test data) if npiNumber is empty
     */
    if (!npiNumber.isPresent()) {
      return Optional.empty();
    }

    if (npiOrgHashMap.containsKey(npiNumber.get())) {
      String npiDisplay = npiOrgHashMap.get(npiNumber.get());
      return Optional.of(npiDisplay);
    }

    return Optional.empty();
  }

  /**
   * Reads all the npi number and npi org data fields from the NPPES file which was downloaded
   * during the build process.
   *
   * @param inputStream for the npi file
   * @throws IOException if there is an issue reading file
   * @return the hashmapped for npis and the npi org names
   */
  protected Map<String, String> readNPIOrgDataStream(InputStream inputStream) throws IOException {
    Map<String, String> npiProcessedData = new HashMap<String, String>();

    try (final InputStream npiStream = inputStream;
        final BufferedReader npiReader = new BufferedReader(new InputStreamReader(npiStream))) {

      String line = "";
      while ((line = npiReader.readLine()) != null) {
        String npiDataColumns[] = line.split("\t");
        if (npiDataColumns.length == 2) {
          npiProcessedData.put(
              npiDataColumns[0].replace("\"", ""), npiDataColumns[1].replace("\"", ""));
        }
      }
    }

    return npiProcessedData;
  }

  /**
   * Returns a inputStream from file name passed in.
   *
   * @param fileName is the file name passed in
   * @return InputStream of file.
   */
  protected static InputStream getFileInputStream(String fileName) {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
  }
}
