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
import com.google.common.base.Strings;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.BeneficiaryHistory;
import gov.cms.bfd.model.rif.BeneficiaryHistory_;
import gov.cms.bfd.model.rif.BeneficiaryMonthly;
import gov.cms.bfd.model.rif.BeneficiaryMonthly_;
import gov.cms.bfd.model.rif.Beneficiary_;
import gov.cms.bfd.server.war.Operation;
import gov.cms.bfd.server.war.commons.CommonHeaders;
import gov.cms.bfd.server.war.commons.LinkBuilder;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import gov.cms.bfd.server.war.commons.OffsetLinkBuilder;
import gov.cms.bfd.server.war.commons.PatientLinkBuilder;
import gov.cms.bfd.server.war.commons.QueryUtils;
import gov.cms.bfd.server.war.commons.RequestHeaders;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.apache.commons.lang3.StringUtils;
import org.hibernate.jpa.QueryHints;
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
public final class R4PatientResourceProvider implements IResourceProvider, CommonHeaders {
  /**
   * The {@link Identifier#getSystem()} values that are supported by {@link #searchByIdentifier}.
   * NOTE: For v2, HICN no longer supported.
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

  /** @param loadedFilterManager the {@link LoadedFilterManager} to use */
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
    if (patientId == null || patientId.getVersionIdPartAsLong() != null) {
      throw new IllegalArgumentException();
    }
    String beneIdText = patientId.getIdPart();
    if (beneIdText == null || beneIdText.trim().isEmpty()) {
      throw new IllegalArgumentException();
    }

    RequestHeaders requestHeader = RequestHeaders.getHeaderWrapper(requestDetails);

    Operation operation = new Operation(Operation.Endpoint.V2_PATIENT);
    operation.setOption("by", "id");
    // there is another method with exclude list: requestHeader.getNVPairs(<excludeHeaders>)
    requestHeader.getNVPairs().forEach((n, v) -> operation.setOption(n, v.toString()));
    operation.publishOperationName();

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Beneficiary> criteria = builder.createQuery(Beneficiary.class);
    Root<Beneficiary> root = criteria.from(Beneficiary.class);

    // commented out as in V2 code;  keep it that way for now
    // if (requestHeader.isMBIinIncludeIdentifiers())
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
          String.format(
              "bene_by_id.include_%s",
              String.join(
                  "_", (List<String>) requestHeader.getValue(HEADER_NAME_INCLUDE_IDENTIFIERS))),
          beneByIdQueryNanoSeconds,
          beneficiary == null ? 0 : 1);
    }

    // Add bene_id to MDC logs
    MDC.put("bene_id", beneIdText);

    return BeneficiaryTransformerV2.transform(metricRegistry, beneficiary, requestHeader);
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
                .filter(
                    p ->
                        QueryUtils.isInRange(p.getMeta().getLastUpdated().toInstant(), lastUpdated))
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

    RequestHeaders requestHeader = RequestHeaders.getHeaderWrapper(requestDetails);
    Operation operation = new Operation(Operation.Endpoint.V2_PATIENT);
    operation.setOption("by", "id");
    // track all api hdrs
    requestHeader.getNVPairs().forEach((n, v) -> operation.setOption(n, v.toString()));
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
      @OptionalParam(name = "_has:Coverage.rfrncyr")
          @Description(shortDefinition = "Part D reference year")
          TokenParam referenceYear,
      @OptionalParam(name = "cursor")
          @Description(shortDefinition = "The cursor used for result pagination")
          String cursor,
      RequestDetails requestDetails) {
    // Figure out what month they're searching for.
    String contractMonth =
        coverageId.getSystem().substring(coverageId.getSystem().lastIndexOf('/') + 1);
    CcwCodebookVariable partDContractMonth = partDCwVariableFor(contractMonth);
    String contractMonthValue = partDFieldByMonth(partDContractMonth);

    // Figure out which year they're searching for.
    int year = Year.now().getValue();
    if (referenceYear != null && !StringUtils.isEmpty(referenceYear.getValueNotNull())) {
      /*
       * TODO Once AB2D has switched to always specifying the year, the implicit `else` on this
       * needs to become an invalid request.
       */
      try {
        year = Integer.parseInt(referenceYear.getValueNotNull());
      } catch (NumberFormatException e) {
        throw new InvalidRequestException("Invalid contract year specified", e);
      }
    }
    YearMonth ym = YearMonth.of(year, Integer.valueOf(contractMonthValue));

    return searchByCoverageContractAndYearMonth(coverageId, ym.atDay(1), requestDetails);
  }

  public Bundle searchByCoverageContractByFieldName(
      // This is very explicit as a place holder until this kind
      // of relational search is more common.
      TokenParam coverageId, String cursor, RequestDetails requestDetails) {
    checkCoverageId(coverageId);
    RequestHeaders requestHeader = RequestHeaders.getHeaderWrapper(requestDetails);
    PatientLinkBuilder paging = new PatientLinkBuilder(requestDetails.getCompleteUrl());
    checkPageSize(paging);

    Operation operation = new Operation(Operation.Endpoint.V2_PATIENT);
    operation.setOption("by", "coverageContract");
    requestHeader.getNVPairs().forEach((n, v) -> operation.setOption(n, v.toString()));
    operation.publishOperationName();

    List<Beneficiary> matchingBeneficiaries = fetchBeneficiaries(coverageId, requestHeader, paging);
    boolean hasAnotherPage = matchingBeneficiaries.size() > paging.getPageSize();
    if (hasAnotherPage) {
      matchingBeneficiaries = matchingBeneficiaries.subList(0, paging.getPageSize());
      paging = new PatientLinkBuilder(paging, hasAnotherPage);
    }

    List<IBaseResource> patients =
        matchingBeneficiaries.stream()
            .map(
                beneficiary -> {
                  // Null out the unhashed HICNs
                  beneficiary.setHicnUnhashed(Optional.empty());

                  Patient patient =
                      BeneficiaryTransformerV2.transform(
                          metricRegistry, beneficiary, requestHeader);
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
    Map<CcwCodebookVariable, String> mapOfMonth =
        new HashMap<CcwCodebookVariable, String>() {
          {
            put(CcwCodebookVariable.PTDCNTRCT01, "partDContractNumberJanId");
            put(CcwCodebookVariable.PTDCNTRCT02, "partDContractNumberFebId");
            put(CcwCodebookVariable.PTDCNTRCT03, "partDContractNumberMarId");
            put(CcwCodebookVariable.PTDCNTRCT04, "partDContractNumberAprId");
            put(CcwCodebookVariable.PTDCNTRCT05, "partDContractNumberMayId");
            put(CcwCodebookVariable.PTDCNTRCT06, "partDContractNumberJunId");
            put(CcwCodebookVariable.PTDCNTRCT07, "partDContractNumberJulId");
            put(CcwCodebookVariable.PTDCNTRCT08, "partDContractNumberAugId");
            put(CcwCodebookVariable.PTDCNTRCT09, "partDContractNumberSeptId");
            put(CcwCodebookVariable.PTDCNTRCT10, "partDContractNumberOctId");
            put(CcwCodebookVariable.PTDCNTRCT11, "partDContractNumberNovId");
            put(CcwCodebookVariable.PTDCNTRCT12, "partDContractNumberDecId");
          }
        };

    if (mapOfMonth.containsKey(month)) {
      return mapOfMonth.get(month);
    }
    throw new InvalidRequestException(
        "Unsupported extension system: " + month.getVariable().getId().toLowerCase());
  }

  /**
   * Fetch beneficiaries for the PartD coverage parameter. If includeIdentiers are present then the
   * entity mappings are fetched as well
   *
   * @param coverageId coverage type
   * @param requestHeader {@link RequestHeaders} the holder that contains all supported resource
   *     request headers
   * @param paging specified
   * @return the beneficiaries
   */
  private List<Beneficiary> fetchBeneficiaries(
      TokenParam coverageId, RequestHeaders requestHeader, PatientLinkBuilder paging) {
    String contractMonth =
        coverageId.getSystem().substring(coverageId.getSystem().lastIndexOf('/') + 1);
    CcwCodebookVariable partDContractMonth = partDCwVariableFor(contractMonth);
    String contractMonthField = partDFieldFor(partDContractMonth);
    String contractCode = coverageId.getValueNotNull();

    // Fetching with joins is not compatible with setMaxResults as explained in this post:
    // https://stackoverflow.com/questions/53569908/jpa-eager-fetching-and-pagination-best-practices
    // So, in cases where there are joins and paging, we query in two steps: first fetch bene-ids
    // with paging and then fetch full benes with joins.
    boolean useTwoSteps = (requestHeader.isMBIinIncludeIdentifiers() && paging.isPagingRequested());
    if (useTwoSteps) {
      // Fetch ids
      List<String> ids =
          queryBeneficiaryIds(contractMonthField, contractCode, paging)
              .setMaxResults(paging.getPageSize() + 1)
              .getResultList();

      // Fetch the benes using the ids
      return queryBeneficiariesByIds(ids, requestHeader).getResultList();
    } else {
      // Fetch benes and their histories in one query
      return queryBeneficiariesBy(contractMonthField, contractCode, paging, requestHeader)
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
      String field, String value, PatientLinkBuilder paging, RequestHeaders requestHeader) {
    String joinsClause = "";
    boolean passDistinctThrough = false;

    /*
      Because the DISTINCT JPQL keyword has two meanings based on the underlying query type, it’s important
      to pass it through to the SQL statement only for scalar queries where the result set requires duplicates
      to be removed by the database engine.

      For parent-child entity queries where the child collection is using JOIN FETCH, the DISTINCT keyword should
      only be applied after the ResultSet is got from JDBC, therefore avoiding passing DISTINCT to the SQL statement
      that gets executed.
    */

    // BFD379: original V2, no MBI logic here
    if (requestHeader.isMBIinIncludeIdentifiers()) {
      joinsClause += "left join fetch b.medicareBeneficiaryIdHistories ";
    }

    if (paging.isPagingRequested() && !paging.isFirstPage()) {
      String query =
          "select distinct b from Beneficiary b "
              + joinsClause
              + "where b."
              + field
              + " = :value and b.beneficiaryId > :cursor "
              + "order by b.beneficiaryId asc";

      return entityManager
          .createQuery(query, Beneficiary.class)
          .setParameter("value", value)
          .setParameter("cursor", paging.getCursor())
          .setHint("hibernate.query.passDistinctThrough", passDistinctThrough);
    } else {
      String query =
          "select distinct b from Beneficiary b "
              + joinsClause
              + "where b."
              + field
              + " = :value "
              + "order by b.beneficiaryId asc";

      return entityManager
          .createQuery(query, Beneficiary.class)
          .setParameter("value", value)
          .setHint("hibernate.query.passDistinctThrough", passDistinctThrough);
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
      List<String> ids, RequestHeaders requestHeader) {
    String joinsClause = "";
    boolean passDistinctThrough = false;
    // BFD379: no MBI logic in original V2 code here
    if (requestHeader.isMBIinIncludeIdentifiers()) {
      joinsClause += "left join fetch b.medicareBeneficiaryIdHistories ";
    }

    String query =
        "select distinct b from Beneficiary b "
            + joinsClause
            + "where b.beneficiaryId in :ids "
            + "order by b.beneficiaryId asc";
    return entityManager
        .createQuery(query, Beneficiary.class)
        .setParameter("ids", ids)
        .setHint("hibernate.query.passDistinctThrough", passDistinctThrough);
  }

  /**
   * Adds support for the FHIR "search" operation for {@link Patient}s, allowing users to search by
   * {@link Patient#getIdentifier()}. Specifically, the following criteria are supported:
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
    if (identifier.getQueryParameterQualifier() != null) {
      throw new InvalidRequestException(
          "Unsupported query parameter qualifier: " + identifier.getQueryParameterQualifier());
    }

    if (!SUPPORTED_HASH_IDENTIFIER_SYSTEMS.contains(identifier.getSystem()))
      throw new InvalidRequestException("Unsupported identifier system: " + identifier.getSystem());

    RequestHeaders requestHeader = RequestHeaders.getHeaderWrapper(requestDetails);

    Operation operation = new Operation(Operation.Endpoint.V2_PATIENT);
    operation.setOption("by", "identifier");
    requestHeader.getNVPairs().forEach((n, v) -> operation.setOption(n, v.toString()));
    operation.setOption(
        "_lastUpdated", Boolean.toString(lastUpdated != null && !lastUpdated.isEmpty()));
    operation.publishOperationName();

    List<IBaseResource> patients;

    try {
      Patient patient;
      switch (identifier.getSystem()) {
        case TransformerConstants.CODING_BBAPI_BENE_MBI_HASH:
          patient = queryDatabaseByMbiHash(identifier.getValue(), requestHeader);
          break;
        case TransformerConstants.CODING_BBAPI_BENE_HICN_HASH:
        case TransformerConstants.CODING_BBAPI_BENE_HICN_HASH_OLD:
          patient = queryDatabaseByHicnHash(identifier.getValue(), requestHeader);
          break;
        default:
          throw new InvalidRequestException(
              "Unsupported identifier system: " + identifier.getSystem());
      }

      patients =
          QueryUtils.isInRange(patient.getMeta().getLastUpdated().toInstant(), lastUpdated)
              ? Collections.singletonList(patient)
              : Collections.emptyList();
    } catch (NoResultException e) {
      patients = new LinkedList<>();
    }

    OffsetLinkBuilder paging = new OffsetLinkBuilder(requestDetails, "/Patient?");
    return TransformerUtilsV2.createBundle(
        paging, patients, loadedFilterManager.getTransactionTime());
  }

  /**
   * @param hicnHash the {@link Beneficiary#getHicn()} hash value to match
   * @param requestHeader the {@link #RequestHeaders} where resource request headers are
   *     encapsulated
   * @return a FHIR {@link Patient} for the CCW {@link Beneficiary} that matches the specified
   *     {@link Beneficiary#getHicn()} hash value
   * @throws NoResultException A {@link NoResultException} will be thrown if no matching {@link
   *     Beneficiary} can be found
   */
  @Trace
  private Patient queryDatabaseByHicnHash(String hicnHash, RequestHeaders requestHeader) {
    return queryDatabaseByHash(
        hicnHash, "hicn", Beneficiary_.hicn, BeneficiaryHistory_.hicn, requestHeader);
  }

  /**
   * @param mbiHash the {@link Beneficiary#getMbiHash()} ()} hash value to match
   * @param requestHeader the {@link #RequestHeaders} where resource request headers are
   *     encapsulated
   * @return a FHIR {@link Patient} for the CCW {@link Beneficiary} that matches the specified
   *     {@link Beneficiary#getMbiHash()} ()} hash value
   * @throws NoResultException A {@link NoResultException} will be thrown if no matching {@link
   *     Beneficiary} can be found
   */
  @Trace
  private Patient queryDatabaseByMbiHash(String mbiHash, RequestHeaders requestHeader) {
    return queryDatabaseByHash(
        mbiHash, "mbi", Beneficiary_.mbiHash, BeneficiaryHistory_.mbiHash, requestHeader);
  }

  /**
   * @param hash the {@link Beneficiary} hash value to match
   * @param hashType a string to represent the hash type (used for logging purposes)
   * @param requestHeader the {@link #RequestHeaders} where resource request headers are
   *     encapsulated
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
      SingularAttribute<Beneficiary, String> beneficiaryHashField,
      SingularAttribute<BeneficiaryHistory, String> beneficiaryHistoryHashField,
      RequestHeaders requestHeader) {
    if (hash == null || hash.trim().isEmpty()) {
      throw new IllegalArgumentException();
    }

    /*
     * Beneficiaries' MBIs can change over time and those past MBIs may land in
     * BeneficiaryHistory records. Accordingly, we need to search for matching MBIs in both the
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
     * In lieu of that, we run two queries: one to find MBI matches in BeneficiariesHistory,
     * and a second to find BENE_ID or MBI matches in Beneficiaries (with all of their data, so
     * we're ready to return the result). This is bad and dumb but I can't find a better working
     * alternative.
     *
     * (I'll just note that I did also try JPA/Hibernate native SQL queries but couldn't get the
     * joins or fetch groups to work with them.)
     *
     * If we want to fix this, we need to move identifiers out entirely to separate tables:
     * i.e., BeneficiaryMbis. We could then safely query these tables and join them
     * back to Beneficiaries (and hopefully the optimizer will play nice, too).
     */

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    // First, find all matching hashes from BeneficiariesHistory.
    CriteriaQuery<String> beneHistoryMatches = builder.createQuery(String.class);
    Root<BeneficiaryHistory> beneHistoryMatchesRoot =
        beneHistoryMatches.from(BeneficiaryHistory.class);
    beneHistoryMatches
        .select(beneHistoryMatchesRoot.get(BeneficiaryHistory_.beneficiaryId))
        .where(builder.equal(beneHistoryMatchesRoot.get(beneficiaryHistoryHashField), hash));
    List<String> matchingIdsFromBeneHistory = null;
    Long fromHistoryQueryNanoSeconds = null;
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
      fromHistoryQueryNanoSeconds = beneHistoryMatchesTimer.stop();
      TransformerUtilsV2.recordQueryInMdc(
          "bene_by_" + hashType + "." + hashType + "s_from_beneficiarieshistory",
          fromHistoryQueryNanoSeconds,
          matchingIdsFromBeneHistory == null ? 0 : matchingIdsFromBeneHistory.size());
    }

    // Then, find all Beneficiary records that match the hash or those BENE_IDs.
    CriteriaQuery<Beneficiary> beneMatches = builder.createQuery(Beneficiary.class);
    Root<Beneficiary> beneMatchesRoot = beneMatches.from(Beneficiary.class);

    // BFD379: in original V2, if check is commented out
    if (requestHeader.isMBIinIncludeIdentifiers()) {
      beneMatchesRoot.fetch(Beneficiary_.medicareBeneficiaryIdHistories, JoinType.LEFT);
    }

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
    Timer.Context timerQuery =
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
      benesByHashOrIdQueryNanoSeconds = timerQuery.stop();

      TransformerUtilsV2.recordQueryInMdc(
          String.format(
              "bene_by_" + hashType + ".bene_by_" + hashType + "_or_id.include_%s",
              String.join(
                  "_", (List<String>) requestHeader.getValue(HEADER_NAME_INCLUDE_IDENTIFIERS))),
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

    // Null out the unhashed HICNs; in v2 we are ignoring HICNs
    beneficiary.setHicnUnhashed(Optional.empty());

    Patient patient =
        BeneficiaryTransformerV2.transform(metricRegistry, beneficiary, requestHeader);
    return patient;
  }

  /**
   * Following method will bring back the Beneficiary that has the most recent rfrnc_yr since there
   * may be more than bene id in the Beneficiaries table
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
      Arrays.asList("true", "false", "mbi");

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

    if (Strings.isNullOrEmpty(headerValues)) {
      return Arrays.asList("");
    } else
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

  public static final boolean CNST_INCL_IDENTIFIERS_EXPECT_MBI = true;
  public static final boolean CNST_INCL_IDENTIFIERS_NOT_EXPECT_MBI = false;

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

  private static LocalDate getFormattedYearMonth(String contractYear, String contractMonth) {
    if (Strings.isNullOrEmpty(contractYear))
      throw new InvalidRequestException("A null or empty year is not supported");
    if (Strings.isNullOrEmpty(contractMonth))
      throw new InvalidRequestException("A null or empty month is not supported");
    if (contractYear.length() != 4)
      throw new InvalidRequestException("A invalid year is not supported");

    String localDateString = String.format("%s-%s-%s", contractYear, contractMonth, "01");
    return LocalDate.parse(localDateString);
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
  private TypedQuery<Beneficiary> queryBeneficiariesByPartDContractCodeAndYearMonth(
      String contractCode,
      LocalDate yearMonth,
      PatientLinkBuilder paging,
      RequestHeaders requestHeader) {
    String joinsClause = "inner join b.beneficiaryMonthlys bm ";
    boolean passDistinctThrough = false;
    if (requestHeader.isMBIinIncludeIdentifiers()) {
      joinsClause += "left join fetch b.medicareBeneficiaryIdHistories ";
    }

    if (paging.isPagingRequested() && !paging.isFirstPage()) {
      String query =
          "select distinct b from Beneficiary b "
              + joinsClause
              + "where bm.partDContractNumberId = :contractCode and "
              + "bm.yearMonth = :yearMonth "
              + "and b.beneficiaryId > :cursor "
              + "order by b.beneficiaryId asc";

      return entityManager
          .createQuery(query, Beneficiary.class)
          .setParameter("contractCode", contractCode)
          .setParameter("yearMonth", yearMonth)
          .setParameter("cursor", paging.getCursor())
          .setHint("hibernate.query.passDistinctThrough", passDistinctThrough);
    } else {
      String query =
          "select distinct b from Beneficiary b "
              + joinsClause
              + "where bm.partDContractNumberId = :contractCode and "
              + "bm.yearMonth = :yearMonth "
              + "order by b.beneficiaryId asc";

      return entityManager
          .createQuery(query, Beneficiary.class)
          .setParameter("contractCode", contractCode)
          .setParameter("yearMonth", yearMonth)
          .setHint("hibernate.query.passDistinctThrough", passDistinctThrough);
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
  private TypedQuery<String> queryBeneficiaryIdsByPartDContractCodeAndYearMonth(
      String contractCode, LocalDate yearMonth, PatientLinkBuilder paging) {
    if (paging.isPagingRequested() && !paging.isFirstPage()) {
      String query =
          "select b.beneficiaryId from Beneficiary b inner join b.beneficiaryMonthlys bm "
              + "where bm.partDContractNumberId = :contractCode and "
              + "bm.yearMonth = :yearMonth and b.beneficiaryId > :cursor "
              + "order by b.beneficiaryId asc";

      return entityManager
          .createQuery(query, String.class)
          .setParameter("contractCode", contractCode)
          .setParameter("yearMonth", yearMonth)
          .setParameter("cursor", paging.getCursor());
    } else {
      String query =
          "select b.beneficiaryId from Beneficiary b inner join b.beneficiaryMonthlys bm "
              + "where bm.partDContractNumberId = :contractCode and "
              + "bm.yearMonth = :yearMonth "
              + "order by b.beneficiaryId asc";

      return entityManager
          .createQuery(query, String.class)
          .setParameter("contractCode", contractCode)
          .setParameter("yearMonth", yearMonth);
    }
  }

  /**
   * Build a criteria for a beneficiary query using the passed in list of ids
   *
   * @param ids to use
   * @param identifiers to add for many-to-one relations
   * @return the criteria
   */
  private TypedQuery<Beneficiary> queryBeneficiariesByIdsWithBeneficiaryMonthlys(
      List<String> ids, RequestHeaders requestHeader) {
    String joinsClause = "inner join b.beneficiaryMonthlys bm ";
    boolean passDistinctThrough = false;
    if (requestHeader.isMBIinIncludeIdentifiers()) {
      joinsClause += "left join fetch b.medicareBeneficiaryIdHistories ";
    }

    String query =
        "select distinct b from Beneficiary b "
            + joinsClause
            + "where b.beneficiaryId in :ids "
            + "order by b.beneficiaryId asc";
    return entityManager
        .createQuery(query, Beneficiary.class)
        .setParameter("ids", ids)
        .setHint("hibernate.query.passDistinctThrough", passDistinctThrough);
  }

  private String partDFieldByMonth(CcwCodebookVariable month) {

    Map<CcwCodebookVariable, String> mapOfMonth =
        new HashMap<CcwCodebookVariable, String>() {
          {
            put(CcwCodebookVariable.PTDCNTRCT01, "01");
            put(CcwCodebookVariable.PTDCNTRCT02, "02");
            put(CcwCodebookVariable.PTDCNTRCT03, "03");
            put(CcwCodebookVariable.PTDCNTRCT04, "04");
            put(CcwCodebookVariable.PTDCNTRCT05, "05");
            put(CcwCodebookVariable.PTDCNTRCT06, "06");
            put(CcwCodebookVariable.PTDCNTRCT07, "07");
            put(CcwCodebookVariable.PTDCNTRCT08, "08");
            put(CcwCodebookVariable.PTDCNTRCT09, "09");
            put(CcwCodebookVariable.PTDCNTRCT10, "10");
            put(CcwCodebookVariable.PTDCNTRCT11, "11");
            put(CcwCodebookVariable.PTDCNTRCT12, "12");
          }
        };

    if (mapOfMonth.containsKey(month)) {
      return mapOfMonth.get(month);
    }
    throw new InvalidRequestException(
        "Unsupported extension system: " + month.getVariable().getId().toLowerCase());
  }

  @Trace
  private Bundle searchByCoverageContractAndYearMonth(
      // This is very explicit as a place holder until this kind
      // of relational search is more common.
      TokenParam coverageId, LocalDate yearMonth, RequestDetails requestDetails) {
    checkCoverageId(coverageId);
    RequestHeaders requestHeader = RequestHeaders.getHeaderWrapper(requestDetails);

    // This endpoint only supports returning unhashed MBIs (and not HICNs), so verify that was
    // requested.
    if (!requestHeader.isMBIinIncludeIdentifiers() || requestHeader.isHICNinIncludeIdentifiers()) {
      throw new InvalidRequestException(
          String.format(
              "This endpoint requires the '%s: mbi' header.",
              CommonHeaders.HEADER_NAME_INCLUDE_IDENTIFIERS));
    }

    PatientLinkBuilder paging = new PatientLinkBuilder(requestDetails.getCompleteUrl());
    checkPageSize(paging);

    Operation operation = new Operation(Operation.Endpoint.V2_PATIENT);
    operation.setOption("by", "coverageContractForYearMonth");
    requestHeader.getNVPairs().forEach((n, v) -> operation.setOption(n, v.toString()));
    operation.publishOperationName();

    List<Beneficiary> matchingBeneficiaries =
        fetchBeneficiariesByContractAndYearMonth(coverageId, yearMonth, paging);
    boolean hasAnotherPage = matchingBeneficiaries.size() > paging.getPageSize();
    if (hasAnotherPage) {
      matchingBeneficiaries = matchingBeneficiaries.subList(0, paging.getPageSize());
      paging = new PatientLinkBuilder(paging, hasAnotherPage);
    }

    List<IBaseResource> patients =
        matchingBeneficiaries.stream()
            .map(b -> BeneficiaryTransformer.transform(metricRegistry, b, requestHeader))
            .collect(Collectors.toList());

    Bundle bundle =
        TransformerUtilsV2.createBundle(patients, paging, loadedFilterManager.getTransactionTime());
    TransformerUtilsV2.workAroundHAPIIssue1585(requestDetails);
    return bundle;
  }

  /**
   * @param coverageId a {@link TokenParam} specifying the Part D contract ID and the month to match
   *     against (yeah, the combo is weird)
   * @param yearMonth the enrollment month and year to match against
   * @param paging the {@link PatientLinkBuilder} being used for paging
   * @return the {@link Beneficiary}s that match the specified PartD contract ID for the specified
   *     year and month
   */
  @Trace
  private List<Beneficiary> fetchBeneficiariesByContractAndYearMonth(
      TokenParam coverageId, LocalDate yearMonth, PatientLinkBuilder paging) {
    String contractCode = coverageId.getValueNotNull();

    /*
     * Workaround for BFD-1057: The `ORDER BY` required on our "find the bene IDs" query (below)
     * intermittently causes the PostgreSQL query planner to run a table scan, which takes over an
     * hour in prod. This _seems_ to be only occurring when the query would return no results. (Yes,
     * this is odd and we don't entirely trust it.) So, when we're on the first page of results or
     * not paging at all here, we first pull a count of expected matches here to see if there's any
     * reason to even run the next query.
     */
    if (!paging.isPagingRequested() || paging.isFirstPage()) {
      long matchingBeneCount =
          queryBeneCountByPartDContractCodeAndYearMonth(yearMonth, contractCode);
      if (matchingBeneCount <= 0) {
        return Collections.emptyList();
      }
    }

    /*
     * Fetching with joins is not compatible with setMaxResults as explained in this post:
     * https://stackoverflow.com/questions/53569908/jpa-eager-fetching-and-pagination-best-practices
     * So, because we need to use a join, we query in two steps: first fetch bene-ids with paging
     * and then fetch full benes with joins. (Note: We can't run this as a subquery, either, as JPA
     * doesn't support `limit` on those.)
     */

    // Fetch the Beneficiary.id values that we will get results for.
    List<String> ids =
        queryBeneficiaryIdsByPartDContractCodeAndYearMonth(yearMonth, contractCode, paging);
    if (ids.isEmpty()) {
      return Collections.emptyList();
    }

    // Fetch the benes using the ids
    return queryBeneficiariesByIdsWithBeneficiaryMonthlys(ids);
  }

  /**
   * @param yearMonth the {@link BeneficiaryMonthly#getYearMonth()} value to match against
   * @param contractId the {@link BeneficiaryMonthly#getPartDContractNumberId()} value to match
   *     against
   * @return the count of matching {@link Beneficiary#getBeneficiaryId()} values
   */
  @Trace
  private long queryBeneCountByPartDContractCodeAndYearMonth(
      LocalDate yearMonth, String contractId) {
    // Create the query to run.
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Long> beneCountCriteria = builder.createQuery(Long.class);
    Root<BeneficiaryMonthly> beneMonthlyRoot = beneCountCriteria.from(BeneficiaryMonthly.class);
    beneCountCriteria.select(builder.count(beneMonthlyRoot));
    beneCountCriteria.where(
        builder.equal(beneMonthlyRoot.get(BeneficiaryMonthly_.yearMonth), yearMonth),
        builder.equal(beneMonthlyRoot.get(BeneficiaryMonthly_.partDContractNumberId), contractId));

    // Run the query and return the results.
    Optional<Long> matchingBeneCount = Optional.empty();
    Long beneHistoryMatchesTimerQueryNanoSeconds = null;
    Timer.Context matchingBeneCountTimer =
        metricRegistry
            .timer(
                MetricRegistry.name(
                    getClass().getSimpleName(),
                    "query",
                    "bene_count_by_year_month_part_d_contract_id"))
            .time();
    try {
      matchingBeneCount =
          Optional.of(entityManager.createQuery(beneCountCriteria).getSingleResult());
      return matchingBeneCount.get();
    } finally {
      beneHistoryMatchesTimerQueryNanoSeconds = matchingBeneCountTimer.stop();
      TransformerUtilsV2.recordQueryInMdc(
          "bene_count_by_year_month_part_d_contract_id",
          beneHistoryMatchesTimerQueryNanoSeconds,
          matchingBeneCount.isPresent() ? 1 : 0);
    }
  }

  /**
   * @param yearMonth the {@link BeneficiaryMonthly#getYearMonth()} value to match against
   * @param contractId the {@link BeneficiaryMonthly#getPartDContractNumberId()} value to match
   *     against
   * @param paging the {@link PatientLinkBuilder} being used for paging
   * @return the {@link List} of matching {@link Beneficiary#getBeneficiaryId()} values
   */
  @Trace
  private List<String> queryBeneficiaryIdsByPartDContractCodeAndYearMonth(
      LocalDate yearMonth, String contractId, PatientLinkBuilder paging) {
    // Create the query to run.
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<String> beneIdCriteria = builder.createQuery(String.class);
    Root<BeneficiaryMonthly> beneMonthlyRoot = beneIdCriteria.from(BeneficiaryMonthly.class);
    beneIdCriteria.select(
        beneMonthlyRoot.get(BeneficiaryMonthly_.parentBeneficiary).get(Beneficiary_.beneficiaryId));

    List<Predicate> wherePredicates = new ArrayList<>();
    wherePredicates.add(
        builder.equal(beneMonthlyRoot.get(BeneficiaryMonthly_.yearMonth), yearMonth));
    wherePredicates.add(
        builder.equal(beneMonthlyRoot.get(BeneficiaryMonthly_.partDContractNumberId), contractId));
    if (paging.isPagingRequested() && !paging.isFirstPage()) {
      wherePredicates.add(
          builder.greaterThan(
              beneMonthlyRoot
                  .get(BeneficiaryMonthly_.parentBeneficiary)
                  .get(Beneficiary_.beneficiaryId),
              paging.getCursor()));
    }
    beneIdCriteria.where(
        builder.and(wherePredicates.toArray(new Predicate[wherePredicates.size()])));
    beneIdCriteria.orderBy(builder.asc(beneMonthlyRoot.get(BeneficiaryMonthly_.parentBeneficiary)));

    // Run the query and return the results.
    List<String> matchingBeneIds = null;
    Long beneHistoryMatchesTimerQueryNanoSeconds = null;
    Timer.Context beneIdMatchesTimer =
        metricRegistry
            .timer(
                MetricRegistry.name(
                    getClass().getSimpleName(),
                    "query",
                    "bene_ids_by_year_month_part_d_contract_id"))
            .time();
    try {
      matchingBeneIds =
          entityManager
              .createQuery(beneIdCriteria)
              .setMaxResults(paging.getPageSize() + 1)
              .getResultList();
      return matchingBeneIds;
    } finally {
      beneHistoryMatchesTimerQueryNanoSeconds = beneIdMatchesTimer.stop();
      TransformerUtilsV2.recordQueryInMdc(
          "bene_ids_by_year_month_part_d_contract_id",
          beneHistoryMatchesTimerQueryNanoSeconds,
          matchingBeneIds == null ? 0 : matchingBeneIds.size());
    }
  }

  /**
   * Query the DB for and return the matching {@link Beneficiary}s
   *
   * @param ids the {@link Beneficiary#getBeneficiaryId()} values to match against
   * @return the matching {@link Beneficiary}s
   */
  @Trace
  private List<Beneficiary> queryBeneficiariesByIdsWithBeneficiaryMonthlys(List<String> ids) {
    // Create the query to run.
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Beneficiary> beneCriteria = builder.createQuery(Beneficiary.class).distinct(true);
    Root<Beneficiary> beneRoot = beneCriteria.from(Beneficiary.class);
    beneRoot.fetch(Beneficiary_.medicareBeneficiaryIdHistories, JoinType.LEFT);
    beneCriteria.where(beneRoot.get(Beneficiary_.beneficiaryId).in(ids));

    // Run the query and return the results.
    List<Beneficiary> matchingBenes = null;
    Long beneMatchesTimerQueryNanoSeconds = null;
    Timer.Context beneIdTimer =
        metricRegistry
            .timer(
                MetricRegistry.name(
                    getClass().getSimpleName(), "query", "benes_by_year_month_part_d_contract_id"))
            .time();
    try {
      matchingBenes =
          entityManager
              .createQuery(beneCriteria)
              .setHint(QueryHints.HINT_PASS_DISTINCT_THROUGH, false)
              .getResultList();
      return matchingBenes;
    } finally {
      beneMatchesTimerQueryNanoSeconds = beneIdTimer.stop();
      TransformerUtilsV2.recordQueryInMdc(
          "benes_by_year_month_part_d_contract_id",
          beneMatchesTimerQueryNanoSeconds,
          matchingBenes == null ? 0 : matchingBenes.size());
    }
  }
}
