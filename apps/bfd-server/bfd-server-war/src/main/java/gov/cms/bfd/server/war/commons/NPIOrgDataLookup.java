package gov.cms.bfd.server.war.commons;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides an NPI org lookup */
public class NPIOrgDataLookup {
  private static final Logger LOGGER = LoggerFactory.getLogger(NPIOrgDataLookup.class);

  /** A fake npi number used for testing */
  public static final String FAKE_NPI_NUMBER = "0000000000";

  /** A fake org name display that is associated with the FAKE_NPI_ORG_NAME */
  public static final String FAKE_NPI_ORG_NAME = "Fake ORG Name";

  /** Hashmap to keep the org names */
  private final Map<String, String> npiOrgHashMap = new HashMap<>();

  /** A field to return the testing org lookup */
  private static NPIOrgDataLookup npiOrgLookupForTesting;

  /** A field to return the production org lookup */
  private static NPIOrgDataLookup npiOrgLookupForProduction;

  /**
   * Factory method for creating a {@link NPIOrgDataLookup } for testing that includes the fake org
   * name.
   *
   * @return the {@link NPIOrgDataLookup }
   */
  public static NPIOrgDataLookup createNpiOrgLookupForTesting() {
    if (npiOrgLookupForTesting == null) {
      npiOrgLookupForTesting = new NPIOrgDataLookup(true);
    }

    return npiOrgLookupForTesting;
  }

  /**
   * Factory method for creating a {@link NPIOrgDataLookup } for production that does not include
   * the fake org name.
   *
   * @return the {@link NPIOrgDataLookup }
   */
  public static NPIOrgDataLookup createNpiOrgLookupForProduction() {
    if (npiOrgLookupForProduction == null) {
      npiOrgLookupForProduction = new NPIOrgDataLookup(false);
    }

    return npiOrgLookupForProduction;
  }

  /**
   * Constructs an {@link NPIOrgDataLookup}
   *
   * @param includeFakeNpiOrgName whether to include the fake testing npi org name or not
   */
  private NPIOrgDataLookup(boolean includeFakeNpiOrgName) {
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
      return null;
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
   * @return a map with npi numbers and org data
   * @param includeFakeNPIOrgCode whether to include the fake testing NPI Org
   */
  private void readNPIOrgDataFile(boolean includeFakeNPIOrgCode) {
    try (final InputStream npiOrgStream =
            Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(gov.cms.bfd.server.data.utilities.NPIApp.NPI_RESOURCE);
        final BufferedReader npiOrgIn = new BufferedReader(new InputStreamReader(npiOrgStream))) {
      String line = "";
      // skip first line which is header
      npiOrgIn.readLine();
      while ((line = npiOrgIn.readLine()) != null) {
        String npiProductColumns[] = line.split("\t");
        try {
          npiOrgHashMap.put(npiProductColumns[0], npiProductColumns[1]);
        } catch (StringIndexOutOfBoundsException e) {
          continue;
        }
      }

      if (includeFakeNPIOrgCode) {
        npiOrgHashMap.put(FAKE_NPI_NUMBER, FAKE_NPI_ORG_NAME);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to read NPI data.", e);
    }
  }
}
