package gov.cms.bfd.server.war;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.bfd.model.rif.npi_fda.NPIData;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Interface for NPIOrgLookup. This allows us to have both a test implementation, and a prod
 * implementation.
 */
@Component
public class NPIOrgLookup {
  /** Provider Entity. */
  public static String ENTITY_TYPE_CODE_PROVIDER = "1";

  /** Organazation Entity. */
  public static String ENTITY_TYPE_CODE_ORGANIZATION = "2";

  /** NPIData entities mapped to an NPI. */
  Map<String, NPIData> npiMap;

  /** Filename of the test data. */
  private static final String TEST_NPI_FILENAME = "npi_e2e_it.json";

  /** Production implementation of NPIOrgLookup. */
  @PersistenceContext EntityManager entityManager;

  /** The singleton. */
  private static NPIOrgLookup npiOrgLookup;

  /** True if this is a test instance. */
  private final boolean testInstance;

  /**
   * Constructor.
   *
   * @param testInstance True if this should be a test instance.
   */
  public NPIOrgLookup(boolean testInstance) {
    this.testInstance = testInstance;
  }

  /** The query that will return the NPI Data for an NPI. */
  private static final String NPI_DATA_QUERY = "select n from NPIData n where n.npi = :npi";

  /** Initializes the NPIData map with the test data. */
  private void initializeNpiMap() {
    npiMap = new HashMap<>();
    String line;
    try (final InputStream npiStream = getFileInputStream();
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

  /** Gets the input stream for the test data file. */
  private InputStream getFileInputStream() {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream(TEST_NPI_FILENAME);
  }

  /**
   * Retrieves test data for a particular NPI.
   *
   * @param npi The npi to find.
   * @return an the NPIData entity.
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
  public Optional<NPIData> retrieveNPIOrgDisplay(Optional<String> npi) {
    if (testInstance) {
      return retrieveTestData(npi);
    }
    if (npi.isPresent()) {
      Query query = entityManager.createQuery(NPI_DATA_QUERY, NPIData.class);
      query.setParameter("npi", npi.get());
      NPIData npiData = (NPIData) query.getSingleResult();
      return Optional.ofNullable(npiData);
    }
    return Optional.empty();
  }

  /** Creates an instance of this class. */
  public static NPIOrgLookup createNpiOrgLookup() {
    if (npiOrgLookup == null) {
      npiOrgLookup = new NPIOrgLookup(false);
    }
    return npiOrgLookup;
  }

  /** Creates a test instance of this class. */
  public static NPIOrgLookup createTestNpiOrgLookup() {
    if (npiOrgLookup == null) {
      npiOrgLookup = new NPIOrgLookup(true);
    }
    return npiOrgLookup;
  }
}
