package gov.cms.bfd.server.war.commons;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnclassifiedServerFailureException;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.Beneficiary_;
import gov.cms.bfd.server.sharedutils.BfdMDC;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Common database queries that can be shared between V1 and V2. */
public class CommonQueries {

  /**
   * Database function that checks all claims for a given beneficiaryId and returns a bitwise mask
   * value that shows if a given claim type will have any data.
   */
  public static final String CHECK_CLAIMS_FOR_DATA_SQL =
      "SELECT * FROM ccw.check_claims_mask(:beneIdValue)";

  /**
   * Database function that takes in a patient identifier type and value and returns a bigint
   * BENE_ID if the identifier can find a beneficiaries record. Supported identifier types are: mbi,
   * mbi-hash, hicn-hash.
   */
  public static final String FIND_BENE_ID_FROM_IDENTIFIER_SQL =
      "SELECT * FROM ccw.find_beneficiary(:searchIdType, :searchIdValue)";

  /** preclude construction from outsiders. */
  private CommonQueries() {}

  /**
   * Query database to determine which claim types have data for the specified beneficiary. The
   * database function returns a bitwise mask value that denotes which claim types will have data
   * for the specified beneficiaryId. This represents a fast and efficient way to ignore a requested
   * claim type that ultimately has no data for our beneficiaryId.
   *
   * <p>The database function returns bits as follows:
   *
   * <ul>
   *   <li>CARRIER CLAIMS : bit 0
   *   <li>INPATIENT CLAIMS : bit 1
   *   <li>OUTPATIENT CLAIMS : bit 2
   *   <li>SNF CLAIMS : bit 3
   *   <li>DME CLAIMS : bit 4
   *   <li>HHA CLAIMS : bit 5
   *   <li>HOSPICE CLAIMS : bit 6
   *   <li>PART D CLAIMS : bit 7
   * </ul>
   *
   * <p>For more information on the database function, see:
   * V111__SETUP_CLAIMS_AVAILABILITY_FUNCTION.SQL in the db migration directory.
   *
   * @param entityManager {@link EntityManager} used to query database.
   * @param beneficiaryId used to identify the Beneficiary to check claims for.
   * @return int bitmask denoting which claims have data.
   */
  public static int availableClaimsData(EntityManager entityManager, long beneficiaryId) {
    List<Object> values =
        entityManager
            .createNativeQuery(CHECK_CLAIMS_FOR_DATA_SQL)
            .setParameter("beneIdValue", beneficiaryId)
            .getResultList();
    return (int) (values != null && values.size() > 0 ? values.get(0) : 0);
  }

  /**
   * Search database for a BENE_ID using a combination of search type and search value. Supports use
   * case where a beneficiary's MBI and/or HICN may have changed by looking at both current
   * (beneficiaries) and historical (beneficiaries_history) data for a match.
   *
   * <p>For more information on the database function, see:
   * V116__FIND_BENEFICIARY_IDENTIFIER_FUNCTION.SQL in the db migration directory.
   *
   * @param entityManager {@link EntityManager} used to query database.
   * @param metricRegistry {@link MetricRegistry} used to setup/capture performance metrics.
   * @param searchType used to denote what the searchValue represents; values are: mbi, mbi-hash,
   *     hich-hash.
   * @param searchValue identifier value used to search for a BENE_ID.
   * @param callerClassName class name of caller; used metrics tracking.
   * @return long BENE_ID for a beneficiary or zero if not found.
   */
  public static long findBeneficiaryIdentifier(
      EntityManager entityManager,
      MetricRegistry metricRegistry,
      String searchType,
      String searchValue,
      String callerClassName) {

    Timer.Context beneHistoryTimer =
        CommonTransformerUtils.createMetricsTimer(
            metricRegistry, callerClassName, "query", "bene_by_mbi", "bene_by_mbi_or_id");
    /*
     * the function returns a String with comma-delimited values; done this way to avoid having
     * to register a Hibernate custom data type handler for an Array of bigint values.
     */
    List<Long> values = null;
    try {
      List<Object> rawValues =
          entityManager
              .createNativeQuery(FIND_BENE_ID_FROM_IDENTIFIER_SQL)
              .setParameter("searchIdType", searchType)
              .setParameter("searchIdValue", searchValue)
              .getResultList();

      // the db function returns a string that ostensibly could contain
      // comma-seperated BENE_ID values due to the possibility of 'hash
      // collisions' for either MBI or HICN. This anomaly will eventually
      // go away and the function will simply return a long (bigint) when
      // support for hashed MBI and hashed HICN lookups are retired. At that
      // point only actual MBI value lookups will be supported and if we have
      // more than one BENE_ID per MBI_NUM then Medicare has a BIG problem!

      if (rawValues != null && rawValues.size() > 0 && rawValues.get(0) != null) {
        String arrayVals = (String) rawValues.get(0);
        values =
            Arrays.asList(arrayVals.split(",")).stream()
                .map(String::trim)
                .map(Long::parseLong)
                .collect(Collectors.toList());
      }
    } catch (Exception e) {
      throw e;
    } finally {
      long queryNanoSeconds = beneHistoryTimer.stop();
      CommonTransformerUtils.recordQueryInMdc(
          "bene_by_mbi.bene_by_mbi_or_id", queryNanoSeconds, values != null ? values.size() : 0);
      beneHistoryTimer.close();
    }

    if (values == null || values.size() < 1) {
      throw new NoResultException("Failed to find BENE_ID for: " + searchType);
    } else if (values.size() > 1) {
      BfdMDC.put(
          "database_query_by_hash_collision_distinct_bene_ids", Long.toString(values.size()));
      throw new UnclassifiedServerFailureException(
          404,
          "By hash query found more than one distinct BENE_ID: "
              + values.size()
              + ", DistinctBeneIdsList: "
              + values);
    }
    return values.getFirst();
  }

  /**
   * fetch a Beneficiary model object from the database for a BENE_ID (primary key).
   *
   * @param entityManager {@link EntityManager} used to query database.
   * @param metricRegistry {@link MetricRegistry} used to setup/capture performance metrics.
   * @param beneId primary key identifier.
   * @param includeIdentifiers boolean denoting whether to include historical identifiers
   * @param callerClassName {@link String} class name provided by caller
   * @param mdcContext {@link String} meta data provide by caller to provide context for MDC
   * @return {@link Beneficiary}.
   * @throws ResourceNotFoundException if record not found.
   */
  public static Beneficiary findBeneficiary(
      EntityManager entityManager,
      MetricRegistry metricRegistry,
      long beneId,
      boolean includeIdentifiers,
      String callerClassName,
      String mdcContext) {

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Beneficiary> criteriaQuery = builder.createQuery(Beneficiary.class);
    Root<Beneficiary> root = criteriaQuery.from(Beneficiary.class);

    if (includeIdentifiers) {
      root.fetch(Beneficiary_.beneficiaryHistories, JoinType.LEFT);
    }
    // TODO : the following left join of skippedRifRecords needs to be removed as part of BFD-3241
    root.fetch(Beneficiary_.skippedRifRecords, JoinType.LEFT);

    criteriaQuery.select(root);
    criteriaQuery.where(builder.equal(root.get(Beneficiary_.beneficiaryId), beneId));

    Timer.Context timerContext =
        CommonTransformerUtils.createMetricsTimer(
            metricRegistry, callerClassName, "query", "bene_by_mbi_or_id");

    Beneficiary beneficiary = null;
    try {
      beneficiary = entityManager.createQuery(criteriaQuery).getSingleResult();
      // Add bene_id to MDC logs
      LoggingUtils.logBeneIdToMdc(beneId);
      // Add number of resources to MDC logs
      LoggingUtils.logResourceCountToMdc(1);
    } catch (NoResultException e) {
      // Add number of resources to MDC logs
      LoggingUtils.logResourceCountToMdc(0);
      throw e;
    } finally {
      long queryNanoSeconds = timerContext.stop();

      CommonTransformerUtils.recordQueryInMdc(
          mdcContext, queryNanoSeconds, beneficiary == null ? 0 : 1);
      timerContext.close();
    }
    return beneficiary;
  }
}
