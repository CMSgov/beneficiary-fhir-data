package gov.cms.bfd.server.war.commons;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.Beneficiary_;
import gov.cms.bfd.server.sharedutils.BfdMDC;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

/** Common database queries that can be shared between V1 & V2. */
public class CommonQueries {

  /**
   * Database function that checks all claims for a given beneficiaryId and returns a bitwise mask
   * value that shows if a given claim type will have any data.
   */
  public static final String CHECK_CLAIMS_FOR_DATA_SQL =
      "SELECT * FROM check_claims_mask(:beneIdValue)";

  /**
   * Database function that takes in a patient identifier type and value and returns a bigint
   * BENE_ID if the identifier can find a beneficiaries record. Supported identifier types are: mbi,
   * mbi-hash, hicn-hash.
   */
  public static final String FIND_BENE_ID_FROM_IDENTIFIER_SQL =
      "SELECT * FROM find_beneficiary(:searchIdType, :searchIdValue)";

  /** preclude construction from outsiders. */
  private CommonQueries() {}

  /**
   * Query database to determine which claim types have data for the specified beneficiary. The
   * database function returns a bitwise mask value that denotes which claim types will have data
   * for the specified beneficiaryId. This represents a fast and efficient way to ignore a requested
   * claim type that ultimately has no data for our beneficiaryId.
   *
   * <p>The database function returns bits as follows: CARRIER_CLAIMS : bit 0 INPATIENT CLAIMS : bit
   * 1 OUTPATIENT CLAIMS : bit 2 SNF CLAIMS : bit 3 DME CLAIMS : bit 4 HHA CLAIMS : bit 5 HOSPICE
   * CLAIMS : bit 6 PART D CLAIMS : bit 7
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
   * @param entityManager {@link EntityManager} used to query database.
   * @param timerContext {@link Timer.Context} used to capture performance metrics.
   * @param searchType used to denote what the searchValue represents; values are: mbi, mbi-hash,
   *     hich-hash.
   * @param searchValue identifier value used to search for a ENE_ID.
   * @return long BENE_ID for a beneficiary or zero if not found.
   */
  public static long findBeneficiaryIdentifier(
      EntityManager entityManager,
      Timer.Context timerContext,
      String searchType,
      String searchValue) {

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

      if (rawValues != null && rawValues.get(0) != null) {
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
      long queryNanoSeconds = timerContext.stop();
      CommonTransformerUtils.recordQueryInMdc(
          "bene_by_" + searchType + "_" + searchType + "s_from_beneficiarieshistory",
          queryNanoSeconds,
          values != null ? values.size() : 0);
    }

    if (values == null || values.size() < 1) {
      throw new NoResultException("Failed to find BENE_ID for: " + searchType);
    } else if (values.size() > 1) {
      BfdMDC.put(
          "database_query_by_hash_collision_distinct_bene_ids", Long.toString(values.size()));
      throw new ResourceNotFoundException(
          "By hash query found more than one distinct BENE_ID: "
              + values.size()
              + ", DistinctBeneIdsList: "
              + values);
    }
    return values.get(0);
  }

  /**
   * fetch a Beneficiary model object from the database for a BENE_ID (primary key).
   *
   * @param entityManager {@link EntityManager} used to query database.
   * @param timerContext {@link Timer.Context} used to capture performance metrics.
   * @param beneId primary key identifier.
   * @param includeIdentifiers boolean denoting whether to include historical identifiers
   * @param queryIdentifier {@link String} MDC query identifier.
   * @return {@link Beneficiary}.
   * @throws ResourceNotFoundException if record not found.
   */
  public static Beneficiary findBeneficiary(
      EntityManager entityManager,
      Timer.Context timerContext,
      long beneId,
      boolean includeIdentifiers,
      String queryIdentifier) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Beneficiary> criteriaQuery = builder.createQuery(Beneficiary.class);
    Root<Beneficiary> root = criteriaQuery.from(Beneficiary.class);

    if (includeIdentifiers) {
      root.fetch(Beneficiary_.beneficiaryHistories, JoinType.LEFT);
    }
    // TODO : the following left join of skippedRifRecords needs to be removed as part of final
    // cleanup
    root.fetch(Beneficiary_.skippedRifRecords, JoinType.LEFT);

    criteriaQuery.select(root);
    criteriaQuery.where(builder.equal(root.get(Beneficiary_.beneficiaryId), beneId));

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
      throw new ResourceNotFoundException("Unknown beneficiaryId: " + beneId);
    } finally {
      long queryNanoSeconds = timerContext.stop();

      CommonTransformerUtils.recordQueryInMdc(
          queryIdentifier, queryNanoSeconds, beneficiary == null ? 0 : 1);
    }
    return beneficiary;
  }
}
