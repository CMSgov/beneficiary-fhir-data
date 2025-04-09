package gov.cms.bfd.server.war;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.bfd.model.rif.npi_fda.FDAData;
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
 * This class will query the fda_data table for drug codes, and return a Map of drugcode -> display
 * if found.
 */
@Service
public class FDADrugCodeDisplayLookup {

  /** DrugCode map. */
  Map<String, String> drugCodeMap;

  /** The entityManager. */
  @PersistenceContext EntityManager entityManager;

  /** The query that will return a list of FDAData. */
  private static final String FDA_DATA_QUERY =
      "select f from FDAData f where f.code in :drugCodeSet";

  /** True if the map data should be pulled from a file instead of the database. */
  private final boolean dataFromFile;

  /**
   * Constructor. If drugCodeFilename can be successfully loaded, we will use it as the datasource.
   * Otherwise, we will query the database.
   *
   * @param drugCodeFilename File to use instead of the database.
   */
  public FDADrugCodeDisplayLookup(
      @Value("${" + SpringConfiguration.PROP_DRUG_CODE_FILE_NAME + "}") String drugCodeFilename) {
    InputStream fdaDataStream = getFileInputStream(drugCodeFilename);
    if (fdaDataStream != null) {
      initializeNpiMap(fdaDataStream);
      dataFromFile = true;
    } else {
      dataFromFile = false;
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
   * Initializes the DrugCodeMap map with the data.
   *
   * @param dataStream The dataStream to use for the map.
   */
  private void initializeNpiMap(InputStream dataStream) {
    drugCodeMap = new HashMap<>();
    String line;
    try (final InputStream stream = dataStream;
        final BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
      ObjectMapper objectMapper = new ObjectMapper();
      while ((line = reader.readLine()) != null) {
        JsonNode rootNode = objectMapper.readTree(line);
        String code = rootNode.path("code").asText();
        FDAData display = objectMapper.readValue(line, FDAData.class);
        drugCodeMap.put(code, display.getDisplay());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Retrieves a Map of drugCode strings from the database.
   *
   * @param drugCodeSet Set of drug codes to enrich.
   * @return an FDAData entity.
   */
  @Transactional
  public Map<String, String> retrieveFDADrugCodeDisplay(Set<String> drugCodeSet) {
    // if dataFromFile is true, we will return the HashMap generated from the file.
    // Otherwise, we query the database.
    if (dataFromFile) {
      return drugCodeMap;
    }
    Query query = entityManager.createQuery(FDA_DATA_QUERY, FDAData.class);
    query.setParameter("drugCodeSet", drugCodeSet);
    List<FDAData> drugCodeData = query.getResultList();
    return drugCodeData.stream().collect(Collectors.toMap(FDAData::getCode, FDAData::getDisplay));
  }
}
