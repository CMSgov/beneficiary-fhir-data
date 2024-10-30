package gov.cms.bfd.data.npi.lookup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.bfd.data.npi.dto.NPIData;
import gov.cms.bfd.data.npi.utility.App;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.InflaterInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides an NPI org lookup. */
public class NPIOrgLookup {
  private static final Logger LOGGER = LoggerFactory.getLogger(NPIOrgLookup.class);

  /** Hashmap to keep the org names. */
  private Map<String, String> npiOrgHashMap = new HashMap<>();

  /** A field to return the production org lookup. */
  private static NPIOrgLookup npiOrgLookupForProduction;

  /**
   * Factory method for creating a {@link NPIOrgLookup } for production that does not include the
   * fake org name.
   *
   * @return the {@link NPIOrgLookup }
   * @throws IOException if there is an issue reading file
   */
  public static NPIOrgLookup createNpiOrgLookup() throws IOException {
    if (npiOrgLookupForProduction == null) {
      InputStream npiDataStream = getFileInputStream(App.NPI_RESOURCE);
      npiOrgLookupForProduction = new NPIOrgLookup(npiDataStream);
    }

    return npiOrgLookupForProduction;
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
  public Optional<NPIData> retrieveNPIOrgDisplay(Optional<String> npiNumber) {
    /*
     * Handle bad data (e.g. our random test data) if npiNumber is empty
     */
    if (!npiNumber.isPresent()) {
      return Optional.empty();
    }

    if (npiOrgHashMap.containsKey(npiNumber.get())) {
      String json = npiOrgHashMap.get(npiNumber.get());
      ObjectMapper mapper = new ObjectMapper();
      try {
        NPIData npiData = mapper.readValue(json, NPIData.class);
        return Optional.of(npiData);
      } catch (JsonProcessingException e) {
        return Optional.empty();
      }
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
    boolean isFileStream = !(inputStream instanceof ByteArrayInputStream);
    Map<String, String> npiProcessedData = new HashMap<>();
    String line;
    try (InflaterInputStream inflaterInputStream = new InflaterInputStream(inputStream);
         // The resource only needs to be inflated if it came from a file.
         final BufferedReader reader =
            new BufferedReader(
                new InputStreamReader(isFileStream ? inflaterInputStream : inputStream))) {
      while ((line = reader.readLine()) != null) {
        // the first part of the line will be the NPI, and the second part is the json.
        String[] tsv = line.split("\t");
        if (tsv.length == 2) {
          npiProcessedData.put(tsv[0], tsv[1]);
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
