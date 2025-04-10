package gov.cms.bfd.server.war;

import gov.cms.bfd.model.rif.npi_fda.NPIData;
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
 * This class will query the npi_data table for an NPI for a set of NPIs, and return a map of NPIs
 * to NPIData entities.
 */
@Service
public class NPIOrgLookup {
  /** Provider Entity. */
  public static String ENTITY_TYPE_CODE_PROVIDER = "1";

  /** Organization Entity. */
  public static String ENTITY_TYPE_CODE_ORGANIZATION = "2";

  /** The entityManager. */
  @PersistenceContext EntityManager entityManager;

  /** The query that will return the NPI Data for an NPI. */
  private static final String NPI_DATA_QUERY = "select n from NPIData n where n.npi in :npiSet";

  /**
   * Retrieves an NPIData entity from the database for a given NPI.
   *
   * @param npiSet Set of NPIs to enrich.
   * @return an NPIData entity.
   */
  @Transactional
  public Map<String, NPIData> retrieveNPIOrgDisplay(Set<String> npiSet) {
    Query query = entityManager.createQuery(NPI_DATA_QUERY);
    query.setParameter("npiSet", npiSet);
    List<NPIData> npiData = query.getResultList();
    return npiData.stream().collect(Collectors.toMap(NPIData::getNpi, entry -> entry));
  }
}
