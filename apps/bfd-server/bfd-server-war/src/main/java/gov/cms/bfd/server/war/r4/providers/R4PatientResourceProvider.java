package gov.cms.bfd.server.war.r4.providers;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.BeneficiaryHistory;
import gov.cms.bfd.model.rif.BeneficiaryHistory_;
import gov.cms.bfd.model.rif.Beneficiary_;
import gov.cms.bfd.server.war.Operation;
import gov.cms.bfd.server.war.commons.LinkBuilder;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import gov.cms.bfd.server.war.commons.OffsetLinkBuilder;
import gov.cms.bfd.server.war.commons.PatientLinkBuilder;
import gov.cms.bfd.server.war.commons.QueryUtils;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * This FHIR {@link IResourceProvider} adds support for R4 {@link Patient} resources, derived from
 * the CCW beneficiaries.
 */
@Component
public final class R4PatientResourceProvider implements IResourceProvider {
  /**
   * The {@link Identifier#getSystem()} values that are supported by {@link #searchByIdentifier}.
   */
  private static final List<String> SUPPORTED_HASH_IDENTIFIER_SYSTEMS =
      Arrays.asList(
          TransformerConstants.CODING_BBAPI_BENE_MBI_HASH,
          TransformerConstants.CODING_BBAPI_BENE_HICN_HASH,
          TransformerConstants.CODING_BBAPI_BENE_HICN_HASH_OLD);

  private EntityManager entityManager;
  private MetricRegistry metricRegistry;
  private LoadedFilterManager loadedFilterManager;

  /** @param entityManager a JPA {@link EntityManager} connected to the application's database */
  @PersistenceContext
  public void setEntityManager(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /** @param metricRegistry the {@link MetricRegistry} to use */
  @Inject
  public void setMetricRegistry(MetricRegistry metricRegistry) {
    this.metricRegistry = metricRegistry;
  }

  /** @param loadedFilterManager the {@link R4LoadedFilterManager} to use */
  @Inject
  public void setLoadedFilterManager(LoadedFilterManager loadedFilterManager) {
    this.loadedFilterManager = loadedFilterManager;
  }

  /** @see ca.uhn.fhir.rest.server.IResourceProvider#getResourceType() */
  @Override
  public Class<? extends IBaseResource> getResourceType() {
    return Patient.class;
  }

  /**
   * Adds support for the FHIR "read" operation, for {@link Patient}s. The {@link Read} annotation
   * indicates that this method supports the read operation.
   *
   * <p>Read operations take a single parameter annotated with {@link IdParam}, and should return a
   * single resource instance.
   *
   * @param patientId The read operation takes one parameter, which must be of type {@link IdType}
   *     and must be annotated with the {@link IdParam} annotation.
   * @param requestDetails a {@link RequestDetails} containing the details of the request URL, used
   *     to parse out pagination values
   * @return Returns a resource matching the specified {@link IdDt}, or <code>null</code> if none
   *     exists.
   */
  @Read(version = false)
  @Trace
  public Patient read(@IdParam IdType patientId, RequestDetails requestDetails) {
    if (patientId == null) throw new IllegalArgumentException();
    if (patientId.getVersionIdPartAsLong() != null) throw new IllegalArgumentException();

    String beneIdText = patientId.getIdPart();
    if (beneIdText == null || beneIdText.trim().isEmpty()) throw new IllegalArgumentException();

    List<String> includeIdentifiersValues = returnIncludeIdentifiersValues(requestDetails);

    Operation operation = new Operation(Operation.Endpoint.V2_PATIENT);
    operation.setOption("by", "id");
    operation.setOption("IncludeIdentifiers", includeIdentifiersValues.toString());
    operation.publishOperationName();

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Beneficiary> criteria = builder.createQuery(Beneficiary.class);
    Root<Beneficiary> root = criteria.from(Beneficiary.class);

    if (hasHICN(includeIdentifiersValues))
      root.fetch(Beneficiary_.beneficiaryHistories, JoinType.LEFT);

    // if (hasMBI(includeIdentifiersValues))
    root.fetch(Beneficiary_.medicareBeneficiaryIdHistories, JoinType.LEFT);

    criteria.select(root);
    criteria.where(builder.equal(root.get(Beneficiary_.beneficiaryId), beneIdText));

    Beneficiary beneficiary = null;
    Long beneByIdQueryNanoSeconds = null;
    Timer.Context timerBeneQuery =
        metricRegistry
            .timer(MetricRegistry.name(getClass().getSimpleName(), "query", "bene_by_id"))
            .time();
    try {
      beneficiary = entityManager.createQuery(criteria).getSingleResult();
    } catch (NoResultException e) {
      throw new ResourceNotFoundException(patientId);
    } finally {
      beneByIdQueryNanoSeconds = timerBeneQuery.stop();

      TransformerUtilsV2.recordQueryInMdc(
          String.format("bene_by_id.include_%s", String.join("_", includeIdentifiersValues)),
          beneByIdQueryNanoSeconds,
          beneficiary == null ? 0 : 1);
    }

    // Null out the unhashed HICNs if we're not supposed to be returning them
    if (!hasHICN(includeIdentifiersValues)) {
      beneficiary.setHicnUnhashed(Optional.empty());
    }

    Patient patient =
        BeneficiaryTransformerV2.transform(metricRegistry, beneficiary, includeIdentifiersValues);
    return patient;
  }

  /**
   * Adds support for the FHIR "search" operation for {@link Patient}s, allowing users to search by
   * {@link Patient#getId()}.
   *
   * <p>The {@link Search} annotation indicates that this method supports the search operation.
   * There may be many different methods annotated with this {@link Search} annotation, to support
   * many different search criteria.
   *
   * @param logicalId a {@link TokenParam} (with no system, per the spec) for the {@link
   *     Patient#getId()} to try and find a matching {@link Patient} for
   * @param startIndex an {@link OptionalParam} for the startIndex (or offset) used to determine
   *     pagination
   * @param lastUpdated an {@link OptionalParam} to filter the results based on the passed date
   *     range
   * @param requestDetails a {@link RequestDetails} containing the details of the request URL, used
   *     to parse out pagination values
   * @return Returns a {@link List} of {@link Patient}s, which may contain multiple matching
   *     resources, or may also be empty.
   */
  @Search
  @Trace
  public Bundle searchByLogicalId(
      @RequiredParam(name = Patient.SP_RES_ID)
          @Description(shortDefinition = "The patient identifier to search for")
          TokenParam logicalId,
      @OptionalParam(name = "startIndex")
          @Description(shortDefinition = "The offset used for result pagination")
          String startIndex,
      @OptionalParam(name = "_lastUpdated")
          @Description(shortDefinition = "Include resources last updated in the given range")
          DateRangeParam lastUpdated,
      RequestDetails requestDetails) {
    if (logicalId.getQueryParameterQualifier() != null)
      throw new InvalidRequestException(
          "Unsupported query parameter qualifier: " + logicalId.getQueryParameterQualifier());
    if (logicalId.getSystem() != null && !logicalId.getSystem().isEmpty())
      throw new InvalidRequestException(
          "Unsupported query parameter system: " + logicalId.getSystem());
    if (logicalId.getValueNotNull().isEmpty())
      throw new InvalidRequestException(
          "Unsupported query parameter value: " + logicalId.getValue());

    List<IBaseResource> patients;
    if (loadedFilterManager.isResultSetEmpty(logicalId.getValue(), lastUpdated)) {
      patients = Collections.emptyList();
    } else {
      try {
        patients =
            Optional.of(read(new IdType(logicalId.getValue()), requestDetails))
                .filter(p -> QueryUtils.isInRange(p.getMeta().getLastUpdated(), lastUpdated))
                .map(p -> Collections.singletonList((IBaseResource) p))
                .orElse(Collections.emptyList());
      } catch (ResourceNotFoundException e) {
        patients = Collections.emptyList();
      }
    }

    /*
     * Publish the operation name. Note: This is a bit later than we'd normally do this, as we need
     * to override the operation name that was published by the possible call to read(...), above.
     */
    Operation operation = new Operation(Operation.Endpoint.V2_PATIENT);
    operation.setOption("by", "id");
    operation.setOption(
        "IncludeIdentifiers", returnIncludeIdentifiersValues(requestDetails).toString());
    operation.setOption(
        "_lastUpdated", Boolean.toString(lastUpdated != null && !lastUpdated.isEmpty()));
    operation.publishOperationName();

    OffsetLinkBuilder paging = new OffsetLinkBuilder(requestDetails, "/Patient?");
    Bundle bundle =
        TransformerUtilsV2.createBundle(paging, patients, loadedFilterManager.getTransactionTime());
    return bundle;
  }

  @Search
  public Bundle searchByCoverageContract(
      // This is very explicit as a place holder until this kind
      // of relational search is more common.
      @RequiredParam(name = "_has:Coverage.extension")
          @Description(shortDefinition = "Part D coverage type")
          TokenParam coverageId,
      @OptionalParam(name = "cursor")
          @Description(shortDefinition = "The cursor used for result pagination")
          String cursor,
      RequestDetails requestDetails) {
    checkCoverageId(coverageId);
    List<String> includeIdentifiersValues = returnIncludeIdentifiersValues(requestDetails);
    PatientLinkBuilder paging = new PatientLinkBuilder(requestDetails.getCompleteUrl());
    checkPageSize(paging);

    Operation operation = new Operation(Operation.Endpoint.V2_PATIENT);
    operation.setOption("by", "coverageContract");
    operation.setOption("IncludeIdentifiers", includeIdentifiersValues.toString());
    operation.publishOperationName();

    List<Beneficiary> matchingBeneficiaries =
        fetchBeneficiaries(coverageId, includeIdentifiersValues, paging);

    boolean hasAnotherPage = matchingBeneficiaries.size() > paging.getPageSize();
    if (hasAnotherPage) {
      matchingBeneficiaries = matchingBeneficiaries.subList(0, paging.getPageSize());
      paging = new PatientLinkBuilder(paging, hasAnotherPage);
    }

    List<IBaseResource> patients =
        matchingBeneficiaries.stream()
            .map(
                beneficiary -> {
                  // Null out the unhashed HICNs if we're not supposed to be returning them
                  if (!hasHICN(includeIdentifiersValues)) {
                    beneficiary.setHicnUnhashed(Optional.empty());
                  }

                  Patient patient =
                      BeneficiaryTransformerV2.transform(
                          metricRegistry, beneficiary, includeIdentifiersValues);
                  return patient;
                })
            .collect(Collectors.toList());

    Bundle bundle =
        TransformerUtilsV2.createBundle(patients, paging, loadedFilterManager.getTransactionTime());
    TransformerUtilsV2.workAroundHAPIIssue1585(requestDetails);
    return bundle;
  }

  private CcwCodebookVariable partDCwVariableFor(String system) {
    try {
      return CcwCodebookVariable.valueOf(system.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new InvalidRequestException("Unsupported extension system: " + system);
    }
  }

  private String partDFieldFor(CcwCodebookVariable month) {
    if (month == CcwCodebookVariable.PTDCNTRCT01) return "partDContractNumberJanId";
    if (month == CcwCodebookVariable.PTDCNTRCT02) return "partDContractNumberFebId";
    if (month == CcwCodebookVariable.PTDCNTRCT03) return "partDContractNumberMarId";
    if (month == CcwCodebookVariable.PTDCNTRCT04) return "partDContractNumberAprId";
    if (month == CcwCodebookVariable.PTDCNTRCT05) return "partDContractNumberMayId";
    if (month == CcwCodebookVariable.PTDCNTRCT06) return "partDContractNumberJunId";
    if (month == CcwCodebookVariable.PTDCNTRCT07) return "partDContractNumberJulId";
    if (month == CcwCodebookVariable.PTDCNTRCT08) return "partDContractNumberAugId";
    if (month == CcwCodebookVariable.PTDCNTRCT09) return "partDContractNumberSeptId";
    if (month == CcwCodebookVariable.PTDCNTRCT10) return "partDContractNumberOctId";
    if (month == CcwCodebookVariable.PTDCNTRCT11) return "partDContractNumberNovId";
    if (month == CcwCodebookVariable.PTDCNTRCT12) return "partDContractNumberDecId";
    throw new InvalidRequestException(
        "Unsupported extension system: " + month.getVariable().getId().toLowerCase());
  }

  /**
   * Fetch beneficiaries for the PartD coverage parameter. If includeIdentiers are present then the
   * entity mappings are fetched as well
   *
   * @param coverageId coverage type
   * @param includedIdentifiers list from the includeIdentifier header
   * @param paging specified
   * @return the beneficiaries
   */
  private List<Beneficiary> fetchBeneficiaries(
      TokenParam coverageId, List<String> includedIdentifiers, PatientLinkBuilder paging) {
    String contractMonth =
        coverageId.getSystem().substring(coverageId.getSystem().lastIndexOf('/') + 1);
    CcwCodebookVariable partDContractMonth = partDCwVariableFor(contractMonth);
    String contractMonthField = partDFieldFor(partDContractMonth);
    String contractCode = coverageId.getValueNotNull();

    // Fetching with joins is not compatible with setMaxResults as explained in this post:
    // https://stackoverflow.com/questions/53569908/jpa-eager-fetching-and-pagination-best-practices
    // So, in cases where there are joins and paging, we query in two steps: first fetch bene-ids
    // with paging and then fetch full benes with joins.
    boolean useTwoSteps =
        (hasHICN(includedIdentifiers) || hasMBI(includedIdentifiers)) && paging.isPagingRequested();
    if (useTwoSteps) {
      // Fetch ids
      List<String> ids =
          queryBeneficiaryIds(contractMonthField, contractCode, paging)
              .setMaxResults(paging.getPageSize() + 1)
              .getResultList();

      // Fetch the benes using the ids
      return queryBeneficiariesByIds(ids, includedIdentifiers).getResultList();
    } else {
      // Fetch benes and their histories in one query
      return queryBeneficiariesBy(contractMonthField, contractCode, paging, includedIdentifiers)
          .setMaxResults(paging.getPageSize() + 1)
          .getResultList();
    }
  }

  /**
   * Build a criteria for a general Beneficiary query
   *
   * @param field to match on
   * @param value to match on
   * @param paging to use for the result set
   * @param identifiers to add for many-to-one relations
   * @return the criteria
   */
  private TypedQuery<Beneficiary> queryBeneficiariesBy(
      String field, String value, PatientLinkBuilder paging, List<String> identifiers) {
    String joinsClause = "";
    joinsClause += "left join fetch b.medicareBeneficiaryIdHistories ";
    if (hasHICN(identifiers)) joinsClause += "left join fetch b.beneficiaryHistories ";

    if (paging.isPagingRequested() && !paging.isFirstPage()) {
      String query =
          "select b from Beneficiary b "
              + joinsClause
              + "where b."
              + field
              + " = :value and b.beneficiaryId > :cursor "
              + "order by b.beneficiaryId asc";

      return entityManager
          .createQuery(query, Beneficiary.class)
          .setParameter("value", value)
          .setParameter("cursor", paging.getCursor());
    } else {
      String query =
          "select b from Beneficiary b "
              + joinsClause
              + "where b."
              + field
              + " = :value "
              + "order by b.beneficiaryId asc";

      return entityManager.createQuery(query, Beneficiary.class).setParameter("value", value);
    }
  }

  /**
   * Build a criteria for a general beneficiaryId query
   *
   * @param field to match on
   * @param value to match on
   * @param paging to use for the result set
   * @return the criteria
   */
  private TypedQuery<String> queryBeneficiaryIds(
      String field, String value, PatientLinkBuilder paging) {
    if (paging.isPagingRequested() && !paging.isFirstPage()) {
      String query =
          "select b.beneficiaryId from Beneficiary b "
              + "where b."
              + field
              + " = :value and b.beneficiaryId > :cursor "
              + "order by b.beneficiaryId asc";

      return entityManager
          .createQuery(query, String.class)
          .setParameter("value", value)
          .setParameter("cursor", paging.getCursor());
    } else {
      String query =
          "select b.beneficiaryId from Beneficiary b "
              + "where b."
              + field
              + " = :value "
              + "order by b.beneficiaryId asc";

      return entityManager.createQuery(query, String.class).setParameter("value", value);
    }
  }

  /**
   * Build a criteria for a beneficiary query using the passed in list of ids
   *
   * @param ids to use
   * @param identifiers to add for many-to-one relations
   * @return the criteria
   */
  private TypedQuery<Beneficiary> queryBeneficiariesByIds(
      List<String> ids, List<String> identifiers) {
    String joinsClause = "";
    joinsClause += "left join fetch b.medicareBeneficiaryIdHistories ";
    if (hasHICN(identifiers)) joinsClause += "left join fetch b.beneficiaryHistories ";

    String query =
        "select distinct b from Beneficiary b "
            + joinsClause
            + "where b.beneficiaryId in :ids "
            + "order by b.beneficiaryId asc";
    return entityManager.createQuery(query, Beneficiary.class).setParameter("ids", ids);
  }

  /**
   * Adds support for the FHIR "search" operation for {@link Patient}s, allowing users to search by
   * {@link Patient#getIdentifier()}. Specifically, the following criteria are supported:
   *
   * <ul>
   *   <li>Matching a {@link Beneficiary#getHicn()} hash value: when {@link TokenParam#getSystem()}
   *       matches one of the {@link #SUPPORTED_HASH_IDENTIFIER_SYSTEMS} entries.
   * </ul>
   *
   * <p>Searches that don't match one of the above forms are not supported.
   *
   * <p>The {@link Search} annotation indicates that this method supports the search operation.
   * There may be many different methods annotated with this {@link Search} annotation, to support
   * many different search criteria.
   *
   * @param identifier an {@link Identifier} {@link TokenParam} for the {@link
   *     Patient#getIdentifier()} to try and find a matching {@link Patient} for
   * @param startIndex an {@link OptionalParam} for the startIndex (or offset) used to determine
   *     pagination
   * @param lastUpdated an {@link OptionalParam} to filter the results based on the passed date
   *     range
   * @param requestDetails a {@link RequestDetails} containing the details of the request URL, used
   *     to parse out pagination values
   * @return Returns a {@link List} of {@link Patient}s, which may contain multiple matching
   *     resources, or may also be empty.
   */
  @Search
  @Trace
  public Bundle searchByIdentifier(
      @RequiredParam(name = Patient.SP_IDENTIFIER)
          @Description(shortDefinition = "The patient identifier to search for")
          TokenParam identifier,
      @OptionalParam(name = "startIndex")
          @Description(shortDefinition = "The offset used for result pagination")
          String startIndex,
      @OptionalParam(name = "_lastUpdated")
          @Description(shortDefinition = "Include resources last updated in the given range")
          DateRangeParam lastUpdated,
      RequestDetails requestDetails) {
    if (identifier.getQueryParameterQualifier() != null)
      throw new InvalidRequestException(
          "Unsupported query parameter qualifier: " + identifier.getQueryParameterQualifier());

    if (!SUPPORTED_HASH_IDENTIFIER_SYSTEMS.contains(identifier.getSystem()))
      throw new InvalidRequestException("Unsupported identifier system: " + identifier.getSystem());

    List<String> includeIdentifiersValues = returnIncludeIdentifiersValues(requestDetails);

    Operation operation = new Operation(Operation.Endpoint.V2_PATIENT);
    operation.setOption("by", "identifier");
    operation.setOption("IncludeIdentifiers", includeIdentifiersValues.toString());
    operation.setOption(
        "_lastUpdated", Boolean.toString(lastUpdated != null && !lastUpdated.isEmpty()));
    operation.publishOperationName();

    List<IBaseResource> patients;
    try {
      Patient patient;
      switch (identifier.getSystem()) {
        case TransformerConstants.CODING_BBAPI_BENE_HICN_HASH:
        case TransformerConstants.CODING_BBAPI_BENE_HICN_HASH_OLD:
          patient = queryDatabaseByHicnHash(identifier.getValue(), includeIdentifiersValues);
          break;
        case TransformerConstants.CODING_BBAPI_BENE_MBI_HASH:
          patient = queryDatabaseByMbiHash(identifier.getValue(), includeIdentifiersValues);
          break;
        default:
          throw new InvalidRequestException(
              "Unsupported identifier system: " + identifier.getSystem());
      }

      patients =
          QueryUtils.isInRange(patient.getMeta().getLastUpdated(), lastUpdated)
              ? Collections.singletonList(patient)
              : Collections.emptyList();
    } catch (NoResultException e) {
      patients = new LinkedList<>();
    }

    OffsetLinkBuilder paging = new OffsetLinkBuilder(requestDetails, "/Patient?");
    Bundle bundle =
        TransformerUtilsV2.createBundle(paging, patients, loadedFilterManager.getTransactionTime());
    return bundle;
  }

  /**
   * @param hicnHash the {@link Beneficiary#getHicn()} hash value to match
   * @param includeIdentifiersValues the {@link #returnIncludeIdentifiersValues(RequestDetails)}
   *     value to use
   * @return a FHIR {@link Patient} for the CCW {@link Beneficiary} that matches the specified
   *     {@link Beneficiary#getHicn()} hash value
   * @throws NoResultException A {@link NoResultException} will be thrown if no matching {@link
   *     Beneficiary} can be found
   */
  @Trace
  private Patient queryDatabaseByHicnHash(String hicnHash, List<String> includeIdentifiersValues) {
    return queryDatabaseByHash(
        hicnHash, "hicn", includeIdentifiersValues, Beneficiary_.hicn, BeneficiaryHistory_.hicn);
  }

  /**
   * @param mbiHash the {@link Beneficiary#getMbiHash()} ()} hash value to match
   * @param includeIdentifiersValues the {@link #returnIncludeIdentifiersValues(RequestDetails)}
   *     value to use
   * @return a FHIR {@link Patient} for the CCW {@link Beneficiary} that matches the specified
   *     {@link Beneficiary#getMbiHash()} ()} hash value
   * @throws NoResultException A {@link NoResultException} will be thrown if no matching {@link
   *     Beneficiary} can be found
   */
  @Trace
  private Patient queryDatabaseByMbiHash(String mbiHash, List<String> includeIdentifiersValues) {
    return queryDatabaseByHash(
        mbiHash,
        "mbi",
        includeIdentifiersValues,
        Beneficiary_.mbiHash,
        BeneficiaryHistory_.mbiHash);
  }

  /**
   * @param hash the {@link Beneficiary} hash value to match
   * @param hashType a string to represent the hash type (used for logging purposes)
   * @param includeIdentifiersValues the {@link #returnIncludeIdentifiersValues(RequestDetails)}
   *     value to use
   * @param beneficiaryHashField the JPA location of the beneficiary hash field
   * @param beneficiaryHistoryHashField the JPA location of the beneficiary history hash field
   * @return a FHIR {@link Patient} for the CCW {@link Beneficiary} that matches the specified
   *     {@link Beneficiary} hash value
   * @throws NoResultException A {@link NoResultException} will be thrown if no matching {@link
   *     Beneficiary} can be found
   */
  @Trace
  private Patient queryDatabaseByHash(
      String hash,
      String hashType,
      List<String> includeIdentifiersValues,
      SingularAttribute<Beneficiary, String> beneficiaryHashField,
      SingularAttribute<BeneficiaryHistory, String> beneficiaryHistoryHashField) {
    if (hash == null || hash.trim().isEmpty()) throw new IllegalArgumentException();

    /*
     * Beneficiaries' HICN/MBIs can change over time and those past HICN/MBIs may land in
     * BeneficiaryHistory records. Accordingly, we need to search for matching HICN/MBIs in both the
     * Beneficiary and the BeneficiaryHistory records.
     *
     * There's no sane way to do this in a single query with JPA 2.1, it appears: JPA doesn't
     * support UNIONs and it doesn't support subqueries in FROM clauses. That said, the ideal query
     * would look like this:
     *
     * SELECT * FROM ( SELECT DISTINCT "beneficiaryId" FROM "Beneficiaries" WHERE "hicn" =
     * :'hicn_hash' UNION SELECT DISTINCT "beneficiaryId" FROM "BeneficiariesHistory" WHERE "hicn" =
     * :'hicn_hash') AS matching_benes INNER JOIN "Beneficiaries" ON matching_benes."beneficiaryId"
     * = "Beneficiaries"."beneficiaryId" LEFT JOIN "BeneficiariesHistory" ON
     * "Beneficiaries"."beneficiaryId" = "BeneficiariesHistory"."beneficiaryId" LEFT JOIN
     * "MedicareBeneficiaryIdHistory" ON "Beneficiaries"."beneficiaryId" =
     * "MedicareBeneficiaryIdHistory"."beneficiaryId";
     *
     * ... with the returned columns and JOINs being dynamic, depending on IncludeIdentifiers.
     *
     * In lieu of that, we run two queries: one to find HICN/MBI matches in BeneficiariesHistory,
     * and a second to find BENE_ID or HICN/MBI matches in Beneficiaries (with all of their data, so
     * we're ready to return the result). This is bad and dumb but I can't find a better working
     * alternative.
     *
     * (I'll just note that I did also try JPA/Hibernate native SQL queries but couldn't get the
     * joins or fetch groups to work with them.)
     *
     * If we want to fix this, we need to move identifiers out entirely to separate tables:
     * BeneficiaryHicns and BeneficiaryMbis. We could then safely query these tables and join them
     * back to Beneficiaries (and hopefully the optimizer will play nice, too).
     */

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    // First, find all matching hashes from BeneficiariesHistory.
    CriteriaQuery<String> beneHistoryMatches = builder.createQuery(String.class);
    Root<BeneficiaryHistory> beneHistoryMatchesRoot =
        beneHistoryMatches.from(BeneficiaryHistory.class);
    beneHistoryMatches.select(beneHistoryMatchesRoot.get(BeneficiaryHistory_.beneficiaryId));
    beneHistoryMatches.where(
        builder.equal(beneHistoryMatchesRoot.get(beneficiaryHistoryHashField), hash));
    List<String> matchingIdsFromBeneHistory = null;
    Long hicnsFromHistoryQueryNanoSeconds = null;
    Timer.Context beneHistoryMatchesTimer =
        metricRegistry
            .timer(
                MetricRegistry.name(
                    getClass().getSimpleName(),
                    "query",
                    "bene_by_" + hashType,
                    hashType + "s_from_beneficiarieshistory"))
            .time();
    try {
      matchingIdsFromBeneHistory = entityManager.createQuery(beneHistoryMatches).getResultList();
    } finally {
      hicnsFromHistoryQueryNanoSeconds = beneHistoryMatchesTimer.stop();
      TransformerUtilsV2.recordQueryInMdc(
          "bene_by_" + hashType + "." + hashType + "s_from_beneficiarieshistory",
          hicnsFromHistoryQueryNanoSeconds,
          matchingIdsFromBeneHistory == null ? 0 : matchingIdsFromBeneHistory.size());
    }

    // Then, find all Beneficiary records that match the hash or those BENE_IDs.
    CriteriaQuery<Beneficiary> beneMatches = builder.createQuery(Beneficiary.class);
    Root<Beneficiary> beneMatchesRoot = beneMatches.from(Beneficiary.class);

    if (hasHICN(includeIdentifiersValues))
      beneMatchesRoot.fetch(Beneficiary_.beneficiaryHistories, JoinType.LEFT);

    // if (hasMBI(includeIdentifiersValues))
    beneMatchesRoot.fetch(Beneficiary_.medicareBeneficiaryIdHistories, JoinType.LEFT);

    beneMatches.select(beneMatchesRoot);
    Predicate beneHashMatches = builder.equal(beneMatchesRoot.get(beneficiaryHashField), hash);
    if (matchingIdsFromBeneHistory != null && !matchingIdsFromBeneHistory.isEmpty()) {
      Predicate beneHistoryHashMatches =
          beneMatchesRoot.get(Beneficiary_.beneficiaryId).in(matchingIdsFromBeneHistory);
      beneMatches.where(builder.or(beneHashMatches, beneHistoryHashMatches));
    } else {
      beneMatches.where(beneHashMatches);
    }
    List<Beneficiary> matchingBenes = Collections.emptyList();
    Long benesByHashOrIdQueryNanoSeconds = null;
    Timer.Context timerHicnQuery =
        metricRegistry
            .timer(
                MetricRegistry.name(
                    getClass().getSimpleName(),
                    "query",
                    "bene_by_" + hashType,
                    "bene_by_" + hashType + "_or_id"))
            .time();
    try {
      matchingBenes = entityManager.createQuery(beneMatches).getResultList();
    } finally {
      benesByHashOrIdQueryNanoSeconds = timerHicnQuery.stop();

      TransformerUtilsV2.recordQueryInMdc(
          String.format(
              "bene_by_" + hashType + ".bene_by_" + hashType + "_or_id.include_%s",
              String.join("_", includeIdentifiersValues)),
          benesByHashOrIdQueryNanoSeconds,
          matchingBenes.size());
    }

    // Then, if we found more than one distinct BENE_ID, or none, throw an error.
    long distinctBeneIds =
        matchingBenes.stream()
            .map(Beneficiary::getBeneficiaryId)
            .filter(Objects::nonNull)
            .distinct()
            .count();
    Beneficiary beneficiary = null;
    if (distinctBeneIds <= 0) {
      throw new NoResultException();
    } else if (distinctBeneIds > 1) {
      MDC.put("database_query.by_hash.collision.distinct_bene_ids", Long.toString(distinctBeneIds));
      throw new ResourceNotFoundException(
          "By hash query found more than one distinct BENE_ID: " + Long.toString(distinctBeneIds));
    } else if (distinctBeneIds == 1) {
      beneficiary = matchingBenes.get(0);
    }

    // Null out the unhashed HICNs if we're not supposed to be returning them
    if (!hasHICN(includeIdentifiersValues)) {
      beneficiary.setHicnUnhashed(Optional.empty());
    }

    Patient patient =
        BeneficiaryTransformerV2.transform(metricRegistry, beneficiary, includeIdentifiersValues);
    return patient;
  }

  /**
   * Following method will bring back the Beneficiary that has the most recent rfrnc_yr since the
   * hicn points to more than one bene id in the Beneficiaries table
   *
   * @param duplicateBenes of matching Beneficiary records the {@link
   *     Beneficiary#getBeneficiaryId()} value to match
   * @return a FHIR {@link Beneficiary} for the CCW {@link Beneficiary} that matches the specified
   *     {@link Beneficiary#getHicn()} hash value
   */
  @Trace
  private Beneficiary selectBeneWithLatestReferenceYear(List<Beneficiary> duplicateBenes) {
    BigDecimal maxReferenceYear = new BigDecimal(-0001);
    String maxReferenceYearMatchingBeneficiaryId = null;

    // loop through matching bene ids looking for max rfrnc_yr
    for (Beneficiary duplicateBene : duplicateBenes) {
      // bene record found but reference year is null - still process
      if (!duplicateBene.getBeneEnrollmentReferenceYear().isPresent()) {
        duplicateBene.setBeneEnrollmentReferenceYear(Optional.of(new BigDecimal(0)));
      }
      // bene reference year is > than prior reference year
      if (duplicateBene.getBeneEnrollmentReferenceYear().get().compareTo(maxReferenceYear) > 0) {
        maxReferenceYear = duplicateBene.getBeneEnrollmentReferenceYear().get();
        maxReferenceYearMatchingBeneficiaryId = duplicateBene.getBeneficiaryId();
      }
    }

    return entityManager.find(Beneficiary.class, maxReferenceYearMatchingBeneficiaryId);
  }

  /**
   * The header key used to determine which header should be used. See {@link
   * #returnIncludeIdentifiersValues(RequestDetails)} for details.
   */
  public static final String HEADER_NAME_INCLUDE_IDENTIFIERS = "IncludeIdentifiers";

  /**
   * The List of valid values for the {@link #HEADER_NAME_INCLUDE_IDENTIFIERS} header. See {@link
   * #returnIncludeIdentifiersValues(RequestDetails)} for details.
   */
  public static final List<String> VALID_HEADER_VALUES_INCLUDE_IDENTIFIERS =
      Arrays.asList("true", "false", "hicn", "mbi");

  /**
   * Return a valid List of values for the IncludeIdenfifiers header
   *
   * @param requestDetails a {@link RequestDetails} containing the details of the request URL, used
   *     to parse out include identifiers values
   * @return List of validated header values against the VALID_HEADER_VALUES_INCLUDE_IDENTIFIERS
   *     list.
   */
  public static List<String> returnIncludeIdentifiersValues(RequestDetails requestDetails) {
    String headerValues = requestDetails.getHeader(HEADER_NAME_INCLUDE_IDENTIFIERS);

    if (headerValues == null || headerValues == "") return Arrays.asList("");
    else
      // Return values split on a comma with any whitespace, valid, distict, and sort
      return Arrays.asList(headerValues.toLowerCase().split("\\s*,\\s*")).stream()
          .peek(
              c -> {
                if (!VALID_HEADER_VALUES_INCLUDE_IDENTIFIERS.contains(c))
                  throw new InvalidRequestException(
                      "Unsupported " + HEADER_NAME_INCLUDE_IDENTIFIERS + " header value: " + c);
              })
          .distinct()
          .sorted()
          .collect(Collectors.toList());
  }

  /**
   * Check if HICN is in {@link #HEADER_NAME_INCLUDE_IDENTIFIERS} header values.
   *
   * @param includeIdentifiersValues a list of header values.
   * @return Returns true if includes unhashed hicn
   */
  public static boolean hasHICN(List<String> includeIdentifiersValues) {
    return includeIdentifiersValues.contains("hicn") || includeIdentifiersValues.contains("true");
  }

  /**
   * Check if MBI is in {@link #HEADER_NAME_INCLUDE_IDENTIFIERS} header values.
   *
   * @param includeIdentifiersValues a list of header values.
   * @return Returns true if includes unhashed mbi
   */
  public static boolean hasMBI(List<String> includeIdentifiersValues) {
    return includeIdentifiersValues.contains("mbi") || includeIdentifiersValues.contains("true");
  }

  /**
   * Check if MBI Hash is in {@link #HEADER_NAME_INCLUDE_IDENTIFIERS} header values.
   *
   * @param includeIdentifiersValues a list of header values.
   * @return Returns true if includes hashed mbi
   */
  public static boolean hasMBIHash(List<String> includeIdentifiersValues) {
    return includeIdentifiersValues.contains("mbi-hash")
        || includeIdentifiersValues.contains("true");
  }

  /**
   * Check that coverageId value is valid
   *
   * @param coverageId
   * @throws InvalidRequestException if invalid coverageId
   */
  public static void checkCoverageId(TokenParam coverageId) {
    if (coverageId.getQueryParameterQualifier() != null)
      throw new InvalidRequestException(
          "Unsupported query parameter qualifier: " + coverageId.getQueryParameterQualifier());
    if (coverageId.getValueNotNull().length() != 5)
      throw new InvalidRequestException(
          "Unsupported query parameter value: " + coverageId.getValueNotNull());
  }

  /**
   * Check that the page size is valid
   *
   * @param paging to check
   */
  public static void checkPageSize(LinkBuilder paging) {
    if (paging.getPageSize() == 0) throw new InvalidRequestException("A zero count is unsupported");
    if (paging.getPageSize() < 0) throw new InvalidRequestException("A negative count is invalid");
  }
}
