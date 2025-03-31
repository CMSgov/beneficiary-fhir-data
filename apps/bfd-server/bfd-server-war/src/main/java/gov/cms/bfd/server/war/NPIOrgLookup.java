package gov.cms.bfd.server.war;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.bfd.model.rif.npi_fda.NPIData;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
  static final String TEST_NPI_FILENAME = "npi_e2e_it.json";

  /** The entityManager. */
  @PersistenceContext EntityManager entityManager;

  /** True if the data is from a file. */
  private final boolean dataFromFile;

  /** The query that will return the NPI Data for an NPI. */
  private static final String NPI_DATA_QUERY = "select n from NPIData n where n.npi in :npiSet";

  /**
   * Constructor. If orgFileName can be successfully loaded, we will use it as the datasource.
   * Otherwise, we will query the database.
   *
   * @param orgFileName File name to use for test purposes.
   */
  public NPIOrgLookup(
      @Value("${" + SpringConfiguration.PROP_ORG_FILE_NAME + "}") String orgFileName) {
    InputStream npiDataStream = getFileInputStream(orgFileName);
    if (npiDataStream != null) {
      initializeNpiMap(npiDataStream);
      dataFromFile = true;
    } else {
      dataFromFile = false;
    }
  }

  /**
   * Initializes the NPIData map with the data.
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
   * Gets the input stream for the data file.
   *
   * @param filename The file to stream.
   * @return the input stream for the test file.
   */
  private InputStream getFileInputStream(String filename) {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
  }

  /**
   * Retrieves an NPIData entity from the database for a given NPI.
   *
   * @param npiSet Set of NPIs to enrich.
   * @return an NPIData entity.
   */
  @Transactional
  public Map<String, NPIData> retrieveNPIOrgDisplay(Set<String> npiSet) {
    // if dataFromFile is true, we will return the HashMap generated from the file.
    // Otherwise, we query the database.
    if (dataFromFile) {
      return npiMap;
    }
    Query query = entityManager.createQuery(NPI_DATA_QUERY);
    query.setParameter("npiSet", npiSet);
    List<NPIData> npiData = query.getResultList();
    return npiData.stream().collect(Collectors.toMap(NPIData::getNpi, entry -> entry));
  }
}
