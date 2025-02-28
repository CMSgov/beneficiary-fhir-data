package gov.cms.bfd.data.npi.lookup;

import static gov.cms.bfd.data.npi.utility.DataUtilityCommons.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.bfd.data.npi.dto.NPIData;
import gov.cms.bfd.data.npi.utility.App;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.InflaterInputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides an NPI org lookup. */
public class NPIOrgLookup {
  private static final Logger LOGGER = LoggerFactory.getLogger(NPIOrgLookup.class);

  /** Hashmap to keep the org names. */
  private Map<String, String> npiOrgHashMap = new HashMap<>();

  /** A field to return the production org lookup. */
  private static NPIOrgLookup npiOrgLookupForProduction;

  /** Zlib compression header. */
  private static final byte[] COMPRESSED_HEADER = {120, -100};

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
    npiOrgHashMap = readNPIOrgDataStream(new BufferedInputStream(npiDataStream));
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
    Map<String, String> npiProcessedData = new HashMap<>();
    // if the stream is compressed, we will have to use InflaterInputStream to read it.
    boolean isCompressedStream = isStreamDeflated(inputStream);
    ObjectMapper mapper = new ObjectMapper();
    try (final InputStream npiStream =
            isCompressedStream ? new InflaterInputStream(inputStream) : inputStream;
        CSVParser csvParser =
            new CSVParser(
                new BufferedReader(new InputStreamReader(npiStream)),
                CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {
      for (CSVRecord record : csvParser) {
        NPIData npiData =
            NPIData.builder()
                .providerOrganizationName(record.get(PROVIDER_ORGANIZATION_NAME_FIELD))
                .entityTypeCode(record.get(ENTITY_TYPE_CODE_FIELD))
                .npi(record.get(NPI_FIELD))
                .taxonomyCode(record.get(TAXONOMY_CODE_FIELD))
                .taxonomyDisplay(record.get(TAXONOMY_DISPLAY_FIELD))
                .providerFirstName(record.get(PROVIDER_FIRST_NAME_FIELD))
                .providerMiddleName(record.get(PROVIDER_MIDDLE_NAME_FIELD))
                .providerLastName(record.get(PROVIDER_LAST_NAME_FIELD))
                .providerNamePrefix(record.get(PROVIDER_PREFIX_FIELD))
                .providerNameSuffix(record.get(PROVIDER_SUFFIX_FIELD))
                .providerCredential(record.get(PROVIDER_CREDENTIAL_FIELD))
                .build();
        String line = mapper.writeValueAsString(npiData);
        npiProcessedData.put(npiData.getNpi(), line);
      }
    }
    return npiProcessedData;
  }

  /**
   * Checks if a stream is deflated. We will read the first two bytes of the stream to compare
   * against the Zlib header (used by DeflaterOutputStream), then reset the stream back to the
   * beginning.
   *
   * @param inputStream The stream to check
   * @return true if the stream is deflated
   * @throws IOException on read error.
   */
  public boolean isStreamDeflated(InputStream inputStream) throws IOException {
    // Mark the current position in the stream.
    inputStream.mark(2);
    // Read the first two bytes
    byte[] bytes = new byte[2];
    int bytesRead = inputStream.read(bytes);
    // Reset the stream to the marked position
    inputStream.reset();
    return (bytesRead == 2 && Arrays.equals(bytes, COMPRESSED_HEADER));
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
