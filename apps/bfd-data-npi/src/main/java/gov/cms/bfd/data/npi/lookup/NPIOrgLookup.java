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
  private final Map<String, String> npiOrgHashMap = new HashMap<>();

  /** A field to return the testing org lookup. */
  private static NPIOrgLookup npiOrgLookupForTesting;

  /** A field to return the production org lookup. */
  private static NPIOrgLookup npiOrgLookupForProduction;

  /**
   * Factory method for creating a {@link NPIOrgLookup } for testing that includes the fake org
   * name.
   *
   * @return the {@link NPIOrgLookup }
   */
  public static NPIOrgLookup createNpiOrgLookupForTesting() throws IOException {
    if (npiOrgLookupForTesting == null) {
      npiOrgLookupForTesting = new NPIOrgLookup(true);
    }

    return npiOrgLookupForTesting;
  }

  /**
   * Factory method for creating a {@link NPIOrgLookup } for production that does not include the
   * fake org name.
   *
   * @return the {@link NPIOrgLookup }
   */
  public static NPIOrgLookup createNpiOrgLookupForProduction() throws IOException {
    if (npiOrgLookupForProduction == null) {
      npiOrgLookupForProduction = new NPIOrgLookup(false);
    }

    return npiOrgLookupForProduction;
  }

  /**
   * Constructs an {@link NPIOrgLookup}.
   *
   * @param includeFakeNpiOrgName whether to include the fake testing npi org name or not
   */
  private NPIOrgLookup(boolean includeFakeNpiOrgName) throws IOException {
    readNPIOrgDataFile(includeFakeNpiOrgName);
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
      String npiOrgName = npiOrgHashMap.get(npiNumber.get());
      return Optional.of(npiOrgName);
    }

    return Optional.empty();
  }

  /**
   * Reads all the npi number and npi org data fields from the NPPES file which was downloaded
   * during the build process.
   *
   * <p>See {@link gov.cms.bfd.server.war.NPIDataUtilityApp} for details.
   *
   * @param includeFakeNPIOrgCode whether to include the fake testing NPI Org
   */
  private void readNPIOrgDataFile(boolean includeFakeNPIOrgCode) throws IOException {
    final InputStream npiOrgStream =
        App.class.getClassLoader().getResourceAsStream(App.NPI_RESOURCE);

    if (npiOrgStream != null) {
      try (final BufferedReader npiOrgIn =
          new BufferedReader(new InputStreamReader(npiOrgStream))) {
        String line = "";
        while ((line = npiOrgIn.readLine()) != null) {
          String npiOrgColumns[] = line.split("\t");
          if (npiOrgColumns.length == 2) {
            npiOrgHashMap.put(
                npiOrgColumns[0].replace("\"", ""), npiOrgColumns[1].replace("\"", ""));
          }
        }

      } catch (IOException e) {
        LOGGER.error("Unable to read NPI data:" + e.getMessage());
        throw new IOException(e);
      }
    } else {
      LOGGER.error("Unable to read stream for NPI Resource");
      throw new IOException("Unable to read stream for NPI Resource");
    }

    if (includeFakeNPIOrgCode) {
      npiOrgHashMap.put(FAKE_NPI_NUMBER, FAKE_NPI_ORG_NAME);
    }
  }
}
