package gov.cms.bfd.server.war;

import gov.cms.bfd.model.rif.npi_fda.FDAData;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * This class will query the fda_data table for drug codes, and return a Map of drugcodes to display
 * strings. if found.
 */
@Service
public class FDADrugCodeDisplayLookup {

  /** The entityManager. */
  @PersistenceContext EntityManager entityManager;

  /** The query that will return a list of FDAData. */
  private static final String FDA_DATA_QUERY =
      "select f from FDAData f where f.code in :drugCodeSet";

  /**
   * Retrieves a Map of drugCode strings from the database.
   *
   * @param drugCodeSet Set of drug codes to enrich.
   * @return an FDAData entity.
   */
  @Transactional
  public Map<String, String> retrieveFDADrugCodeDisplay(Set<String> drugCodeSet) {
    Query query = entityManager.createQuery(FDA_DATA_QUERY, FDAData.class);
    query.setParameter("drugCodeSet", drugCodeSet);
    List<FDAData> drugCodeData = query.getResultList();
    return drugCodeData.stream().collect(Collectors.toMap(FDAData::getCode, FDAData::getDisplay));
  }
}
