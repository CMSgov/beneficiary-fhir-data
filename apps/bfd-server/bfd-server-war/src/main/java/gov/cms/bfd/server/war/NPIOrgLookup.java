package gov.cms.bfd.server.war;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.bfd.model.rif.npi_fda.NPIData;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * This class will query the npi_data table for an NPI, and return an NPIData entity if found. There
 * is also the option to create a test version of the class, which will use test data stored in a
 * file.
 */
@Service
public class NPIOrgLookup {
  /** Provider Entity. */
  public static String ENTITY_TYPE_CODE_PROVIDER = "1";

  /** Organization Entity. */
  public static String ENTITY_TYPE_CODE_ORGANIZATION = "2";

  /** NPIData entities mapped to an NPI. */
  Map<String, NPIData> npiMap;

  /** Filename of the test data. */
  private static final String TEST_NPI_FILENAME = "npi_e2e_it.json";

  /** The entityManager. */
  @PersistenceContext EntityManager entityManager;

  /** The instance of NPIOrgLookup that will be returned for testing. */
  private static NPIOrgLookup npiOrgLookup;

  /** True if this is a test instance. */
  private final boolean testInstance;

  /** The query that will return the NPI Data for an NPI. */
  private static final String NPI_DATA_QUERY = "select n from NPIData n where n.npi = :npi";

  /**
   * Constructor. If orgFileName can be successfully loaded, then this is treated as a test
   * instance. Otherwise, we can assume that this is a production instance.
   *
   * @param orgFileName File name to use for test purposes.
   */
  public NPIOrgLookup(
      @Value("${" + SpringConfiguration.PROP_ORG_FILE_NAME + "}") String orgFileName) {
    InputStream npiDataStream = getFileInputStream(orgFileName);
    if (npiDataStream != null) {
      initializeNpiMap(npiDataStream);
      testInstance = true;
    } else {
      testInstance = false;
    }
  }

  private void initializeNpiMap() {
    final InputStream npiStream = getFileInputStream(TEST_NPI_FILENAME);
    initializeNpiMap(npiStream);
  }

  /**
   * Initializes the NPIData map with the test data.
   *
   * @param dataStream The dataStream to use for the map.
   */
  private void initializeNpiMap(InputStream dataStream) {
    npiMap = new HashMap<>();
    String line;
    try (final InputStream npiStream = dataStream;
        final BufferedReader reader = new BufferedReader(new InputStreamReader(npiStream))) {
      ObjectMapper objectMapper = new ObjectMapper();
      while ((line = reader.readLine()) != null) {
        JsonNode rootNode = objectMapper.readTree(line);
        String npi = rootNode.path("npi").asText();
        NPIData npiData = objectMapper.readValue(line, NPIData.class);
        npiMap.put(npi, npiData);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Gets the input stream for the test data file.
   *
   * @param filename The file to stream.
   * @return the input stream for the test file.
   */
  private InputStream getFileInputStream(String filename) {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
  }

  /**
   * Retrieves test data for a particular NPI.
   *
   * @param npi The npi to find.
   * @return the NPIData entity.
   */
  private Optional<NPIData> retrieveTestData(Optional<String> npi) {
    if (npiMap == null) {
      initializeNpiMap();
    }
    if (npi.isPresent() && npiMap.containsKey(npi.get())) {
      return Optional.of(npiMap.get(npi.get()));
    } else {
      return Optional.empty();
    }
  }

  /**
   * Retrieves an NPIData entity from the database for a given NPI.
   *
   * @param npi The provider or organization's NPI.
   * @return an NPIData entity.
   */
  @Transactional
  public Optional<NPIData> retrieveNPIOrgDisplay(Optional<String> npi) {
    // if testInstance is true, we will return a value from the HashMap.
    // Otherwise, we query the database.
    if (testInstance) {
      return retrieveTestData(npi);
    }
    if (npi.isPresent()) {
      Query query = entityManager.createQuery(NPI_DATA_QUERY, NPIData.class);
      query.setParameter("npi", npi.get());
      try {
        NPIData npiData = (NPIData) query.getSingleResult();
        return Optional.ofNullable(npiData);
      } catch (NoResultException e) {
        return Optional.empty();
      }
    }
    return Optional.empty();
  }

  /**
   * Creates a test instance of this class.
   *
   * @return Returns an npiOrgLookup for testing.
   */
  public static NPIOrgLookup createTestNpiOrgLookup() {
    if (npiOrgLookup == null) {
      npiOrgLookup = new NPIOrgLookup(TEST_NPI_FILENAME);
    }
    return npiOrgLookup;
  }
}
