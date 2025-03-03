package gov.cms.bfd.data.npi.lookup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides an NPI org lookup. */
public class NPIOrgLookup {
  private static final Logger LOGGER = LoggerFactory.getLogger(NPIOrgLookup.class);

  /** Hashmap to keep the org names. */
  private Map<String, String> npiOrgHashMap = new HashMap<>();

  /** A field to return the production org lookup. */
  private static NPIOrgLookup fakeNpiOrgLookup;

  /** Zlib compression header. */
  private static final byte[] COMPRESSED_HEADER = {120, -100};

  /**
   * Test-only factory method for creating a {@link NPIOrgLookup } that does not include the fake
   * org name.
   *
   * @return the fake {@link NPIOrgLookup}
   * @throws IOException if there is an issue reading file
   */
  @VisibleForTesting
  public static NPIOrgLookup createTestNpiOrgLookup() throws IOException {
    if (fakeNpiOrgLookup == null) {
      InputStream npiDataStream = getFileInputStream(App.NPI_TESTING_RESOURCE_FILE);
      fakeNpiOrgLookup = new NPIOrgLookup(npiDataStream);
    }

    return fakeNpiOrgLookup;
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
    String line;
    try (final InputStream npiStream =
            isCompressedStream ? new InflaterInputStream(inputStream) : inputStream;
        final BufferedReader reader = new BufferedReader(new InputStreamReader(npiStream))) {
      ObjectMapper objectMapper = new ObjectMapper();
      while ((line = reader.readLine()) != null) {
        JsonNode rootNode = objectMapper.readTree(line);
        String npi = rootNode.path("npi").asText();
        npiProcessedData.put(npi, line);
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
