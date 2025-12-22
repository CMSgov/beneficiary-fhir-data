package gov.cms.bfd.server.war.stu3.providers;

import static gov.cms.bfd.server.war.commons.StringUtils.parseIntOrBadRequest;
import static gov.cms.bfd.server.war.commons.StringUtils.parseLongOrBadRequest;
import static gov.cms.bfd.server.war.commons.StringUtils.splitOnCommas;
import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnclassifiedServerFailureException;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Strings;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.BeneficiaryHistory;
import gov.cms.bfd.model.rif.entities.BeneficiaryHistory_;
import gov.cms.bfd.model.rif.entities.BeneficiaryMonthly;
import gov.cms.bfd.model.rif.entities.BeneficiaryMonthly_;
import gov.cms.bfd.model.rif.entities.Beneficiary_;
import gov.cms.bfd.server.war.CanonicalOperation;
import gov.cms.bfd.server.war.commons.CommonHeaders;
import gov.cms.bfd.server.war.commons.CommonQueries;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import gov.cms.bfd.server.war.commons.LoggingUtils;
import gov.cms.bfd.server.war.commons.OffsetLinkBuilder;
import gov.cms.bfd.server.war.commons.OpenAPIContentProvider;
import gov.cms.bfd.server.war.commons.PatientLinkBuilder;
import gov.cms.bfd.server.war.commons.QueryUtils;
import gov.cms.bfd.server.war.commons.RequestHeaders;
import gov.cms.bfd.server.war.commons.RetryOnFailoverOrConnectionException;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.SingularAttribute;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Component;

/**
 * This FHIR {@link IResourceProvider} adds support for STU3 {@link Patient} resources, derived from
 * the CCW beneficiaries.
 */
@Component
public class PatientResourceProvider implements IResourceProvider, CommonHeaders {

  /**
   * The {@link Identifier#getSystem()} values that are supported by {@link #searchByIdentifier}.
   */
  private static final List<String> SUPPORTED_HASH_IDENTIFIER_SYSTEMS =
      Arrays.asList(
          TransformerConstants.CODING_BBAPI_BENE_MBI_HASH,
          TransformerConstants.CODING_BBAPI_BENE_HICN_HASH,
          TransformerConstants.CODING_BBAPI_BENE_HICN_HASH_OLD,
          TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED);

  /** The Entity manager. */
  private EntityManager entityManager;

  /** The Metric registry. */
  private final MetricRegistry metricRegistry;

  /** The Loaded filter manager. */
  private final LoadedFilterManager loadedFilterManager;

  /** The beneficiary transformer. */
  private final BeneficiaryTransformer beneficiaryTransformer;

  /** The expected coverage id length. */
  private static final int EXPECTED_COVERAGE_ID_LENGTH = 5;

  /**
   * Instantiates a new {@link PatientResourceProvider}.
   *
   * <p>Spring will wire this class during the initial component scan, so this constructor should
   * only be explicitly called by tests.
   *
   * @param metricRegistry the metric registry
   * @param loadedFilterManager the loaded filter manager
   * @param beneficiaryTransformer the beneficiary transformer
   */
  public PatientResourceProvider(
      MetricRegistry metricRegistry,
      LoadedFilterManager loadedFilterManager,
      BeneficiaryTransformer beneficiaryTransformer) {
    this.metricRegistry = requireNonNull(metricRegistry);
    this.loadedFilterManager = requireNonNull(loadedFilterManager);
    this.beneficiaryTransformer = requireNonNull(beneficiaryTransformer);
  }

  /**
   * Sets the {@link #entityManager}.
   *
   * @param entityManager a JPA {@link EntityManager} connected to the application's database
   */
  @PersistenceContext
  public void setEntityManager(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /** {@inheritDoc} */
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
  @RetryOnFailoverOrConnectionException
  public Patient read(@IdParam IdType patientId, RequestDetails requestDetails) {
    if (patientId == null || patientId.getIdPart() == null) {
      throw new InvalidRequestException("Missing required patient ID");
    }
    if (patientId.getVersionIdPartAsLong() != null) {
      throw new InvalidRequestException("Patient ID must not define a version");
    }
    Long beneficiaryId;
    try {
      beneficiaryId = patientId.getIdPartAsLong();
    } catch (NumberFormatException e) {
      throw new InvalidRequestException("Patient ID must be a number");
    }
    RequestHeaders requestHeader = RequestHeaders.getHeaderWrapper(requestDetails);
    CanonicalOperation operation = new CanonicalOperation(CanonicalOperation.Endpoint.V1_PATIENT);
    operation.setOption("by", "id");
    requestHeader.getNVPairs().forEach((n, v) -> operation.setOption(n, v.toString()));
    operation.publishOperationName();
    boolean includeIdentifiers =
        (requestHeader.isHICNinIncludeIdentifiers() || requestHeader.isMBIinIncludeIdentifiers());

    Beneficiary beneficiary = null;
    try {
      beneficiary =
          CommonQueries.findBeneficiary(
              entityManager,
              metricRegistry,
              beneficiaryId,
              includeIdentifiers,
              getClass().getSimpleName(),
              String.format(
                  "bene_by_id_include_%s",
                  String.join(
                      "_",
                      (List<String>) requestHeader.getValue(HEADER_NAME_INCLUDE_IDENTIFIERS))));

      // Null out the unhashed HICNs if we're not supposed to be returning them
      if (!requestHeader.isHICNinIncludeIdentifiers()) {
        beneficiary.setHicnUnhashed(Optional.empty());
      }
      // Null out the unhashed MBIs if we're not supposed to be returning
      if (!requestHeader.isMBIinIncludeIdentifiers()) {
        beneficiary.setMedicareBeneficiaryId(Optional.empty());
      }
    } catch (NoResultException e) {
      // Add number of resources to MDC logs
      LoggingUtils.logResourceCountToMdc(0);
      throw new ResourceNotFoundException(patientId);
    }
    return beneficiaryTransformer.transform(beneficiary, requestHeader);
  }

  /**
   * Search by coverage contract.
   *
   * @param coverageId the coverage id
   * @param referenceYear the reference year
   * @param cursor the cursor for paging
   * @param count the count for paging
   * @param requestDetails the request details
   * @return the bundle representing the results
   */
  @Search
  @RetryOnFailoverOrConnectionException
  public Bundle searchByCoverageContract(
      // This is very explicit as a placeholder until this kind
      // of relational search is more common.
      @RequiredParam(name = "_has:Coverage.extension")
          @Description(
              shortDefinition = OpenAPIContentProvider.PATIENT_PARTD_CONTRACT_SHORT,
              value = OpenAPIContentProvider.PATIENT_PARTD_CONTRACT_VALUE)
          TokenParam coverageId,
      @OptionalParam(name = "_has:Coverage.rfrncyr")
          @Description(
              shortDefinition = OpenAPIContentProvider.PATIENT_PARTD_REFYR_SHORT,
              value = OpenAPIContentProvider.PATIENT_PARTD_REFYR_VALUE)
          TokenParam referenceYear,
      @OptionalParam(name = "cursor")
          @Description(
              shortDefinition = OpenAPIContentProvider.PATIENT_PARTD_CURSOR_SHORT,
              value = OpenAPIContentProvider.PATIENT_PARTD_CURSOR_VALUE)
          String cursor,
      @OptionalParam(name = Constants.PARAM_COUNT)
          @Description(
              shortDefinition = OpenAPIContentProvider.COUNT_SHORT,
              value = OpenAPIContentProvider.COUNT_VALUE)
          String count,
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
      year = parseIntOrBadRequest(referenceYear.getValueNotNull(), "Contract Year");
    }
    YearMonth ym = YearMonth.of(year, Integer.valueOf(contractMonthValue));

    return searchByCoverageContractAndYearMonth(coverageId, ym.atDay(1), requestDetails);
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
   * @param count an {@link OptionalParam} for the count used in pagination
   * @param lastUpdated an {@link OptionalParam} to filter the results based on the passed date
   *     range
   * @param requestDetails a {@link RequestDetails} containing the details of the request URL, used
   *     to parse out pagination values
   * @return Returns a {@link List} of {@link Patient}s, which may contain multiple matching
   *     resources, or may also be empty.
   */
  @Search
  @RetryOnFailoverOrConnectionException
  public Bundle searchByLogicalId(
      @RequiredParam(name = Patient.SP_RES_ID)
          @Description(
              shortDefinition = OpenAPIContentProvider.PATIENT_SP_RES_ID_SHORT,
              value = OpenAPIContentProvider.PATIENT_SP_RES_ID_VALUE)
          TokenParam logicalId,
      @OptionalParam(name = "startIndex")
          @Description(
              shortDefinition = OpenAPIContentProvider.PATIENT_START_INDEX_SHORT,
              value = OpenAPIContentProvider.PATIENT_START_INDEX_VALUE)
          String startIndex,
      @OptionalParam(name = Constants.PARAM_COUNT)
          @Description(
              shortDefinition = OpenAPIContentProvider.COUNT_SHORT,
              value = OpenAPIContentProvider.COUNT_VALUE)
          String count,
      @OptionalParam(name = "_lastUpdated")
          @Description(
              shortDefinition = OpenAPIContentProvider.PATIENT_LAST_UPDATED_VALUE,
              value = OpenAPIContentProvider.PATIENT_LAST_UPDATED_VALUE)
          DateRangeParam lastUpdated,
      RequestDetails requestDetails) {

    if (logicalId.getQueryParameterQualifier() != null) {
      throw new InvalidRequestException(
          "Unsupported query parameter qualifier: " + logicalId.getQueryParameterQualifier());
    }
    /*
     * Only allow a system identifier for http://hl7.org/fhir/sid/us-mbi; while we
     * can support HTTP GET here because we'll obfuscate the MBI_NUM prior to any
     * logging, we may want to throw an Exception if not a POST request.
     */
    boolean unhashedMbiLookup =
        (logicalId.getSystem() != null
            && logicalId
                .getSystem()
                .equals(TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED));

    if (unhashedMbiLookup) {
      if (RequestTypeEnum.POST != requestDetails.getRequestType()) {
        throw new InvalidRequestException(
            String.format(
                "Search query by '%s' is only supported in POST request",
                TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED));
      }
    } else if (logicalId.getSystem() != null && !logicalId.getSystem().isEmpty()) {
      throw new InvalidRequestException(
          "System is unsupported here and should not be set (" + logicalId.getSystem() + ")");
    }
    if (logicalId.getValueNotNull().isEmpty()) {
      throw new InvalidRequestException("Missing required id value");
    }

    RequestHeaders requestHeader = RequestHeaders.getHeaderWrapper(requestDetails);
    long beneId = 0;
    List<IBaseResource> patients;
    if (unhashedMbiLookup) {
      patients = fetchPatientResourceByIdentifer(logicalId, lastUpdated, requestHeader);
      if (!patients.isEmpty()) {
        Patient patient = (Patient) patients.get(0);
        Optional<Identifier> id =
            patient.getIdentifier().stream()
                .filter(i -> TransformerConstants.CODING_BBAPI_BENE_ID.equals(i.getSystem()))
                .findFirst();
        beneId = id.isPresent() ? Long.parseLong(id.get().getValue()) : 0;
      }
    } else {
      beneId = parseLongOrBadRequest(logicalId.getValue(), "Patient ID");
      if (loadedFilterManager.isResultSetEmpty(beneId, lastUpdated)) {
        // Add bene_id to MDC logs when _lastUpdated filter is in effect
        LoggingUtils.logBeneIdToMdc(beneId);
        // Add number of resources to MDC logs
        LoggingUtils.logResourceCountToMdc(0);
        patients = Collections.emptyList();
      } else {
        try {
          patients =
              Optional.of(read(new IdType(logicalId.getValue()), requestDetails))
                  .filter(
                      p ->
                          QueryUtils.isInRange(
                              p.getMeta().getLastUpdated().toInstant(), lastUpdated))
                  .map(p -> Collections.singletonList((IBaseResource) p))
                  .orElse(Collections.emptyList());
        } catch (UnclassifiedServerFailureException e) {
          throw new ResourceNotFoundException(e.getMessage());
        } catch (NoResultException | ResourceNotFoundException e) {
          patients = Collections.emptyList();
        }
      }
    }

    /*
     * Publish the operation name. Note: This is a bit later than we'd normally do this, as we need
     * to override the operation name that was published by the possible call to read(...), above.
     */
    CanonicalOperation operation = new CanonicalOperation(CanonicalOperation.Endpoint.V1_PATIENT);
    operation.setOption("by", "id");
    // track all api hdrs
    requestHeader.getNVPairs().forEach((n, v) -> operation.setOption(n, v.toString()));
    operation.setOption(
        "_lastUpdated", Boolean.toString(lastUpdated != null && !lastUpdated.isEmpty()));
    operation.publishOperationName();

    OffsetLinkBuilder paging = new OffsetLinkBuilder(requestDetails, "/Patient?");
    Bundle bundle =
        TransformerUtils.createBundle(paging, patients, loadedFilterManager.getTransactionTime());

    // Add bene_id to MDC logs
    LoggingUtils.logBeneIdToMdc(beneId);
    return bundle;
  }

  /**
   * Search by coverage contract and year month.
   *
   * @param coverageId the coverage id
   * @param yearMonth the year month
   * @param requestDetails the request details
   * @return the search results
   */
  private Bundle searchByCoverageContractAndYearMonth(
      // This is very explicit as a placeholder until this kind
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

    PatientLinkBuilder paging = new PatientLinkBuilder(requestDetails);

    CanonicalOperation operation = new CanonicalOperation(CanonicalOperation.Endpoint.V1_PATIENT);
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
            .map(b -> beneficiaryTransformer.transform(b, requestHeader))
            .collect(Collectors.toList());

    Bundle bundle =
        TransformerUtils.createBundle(patients, paging, loadedFilterManager.getTransactionTime());
    TransformerUtils.workAroundHAPIIssue1585(requestDetails);

    // Add bene_id to MDC logs
    LoggingUtils.logBenesToMdc(bundle);

    return bundle;
  }

  /**
   * Get the {@link CcwCodebookVariable} value for the specified system string.
   *
   * <p>TODO: Move this out of here into a shared/generic location and rename method
   *
   * @param system the system to find the {@link CcwCodebookVariable} for
   * @return the ccw codebook variable
   * @throws InvalidRequestException (http 400 error) if the system did not match a known {@link
   *     CcwCodebookVariable}
   */
  private CcwCodebookVariable partDCwVariableFor(String system) {
    try {
      return CcwCodebookVariable.valueOf(system.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new InvalidRequestException("Unsupported extension system: " + system);
    }
  }

  /**
   * Gets the part D contract month field from the given {@link CcwCodebookVariable}.
   *
   * <p>TODO: This could be moved somewhere else; also should this hardcoded map exist in a more
   * central location?
   *
   * @param month the part d contract variable to look for as a {@link CcwCodebookVariable}
   * @return the string representing the part d contract month
   * @throws InvalidRequestException if the {@link CcwCodebookVariable} is not one of the supported
   *     part d contract values
   */
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

  /**
   * Fetch beneficiaries by contract and year-month.
   *
   * @param coverageId a {@link TokenParam} specifying the Part D contract ID and the month to match
   *     against (yeah, the combo is weird)
   * @param yearMonth the enrollment month and year to match against
   * @param paging the {@link PatientLinkBuilder} being used for paging
   * @return the {@link Beneficiary}s that match the specified PartD contract ID for the specified
   *     year and month
   */
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
      boolean matchingBeneExists =
          queryBeneExistsByPartDContractCodeAndYearMonth(yearMonth, contractCode);
      if (!matchingBeneExists) {
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
    List<Long> ids =
        queryBeneficiaryIdsByPartDContractCodeAndYearMonth(yearMonth, contractCode, paging);
    if (ids.isEmpty()) {
      return Collections.emptyList();
    }

    // Fetch the benes using the ids
    return queryBeneficiariesByIdsWithBeneficiaryMonthlys(ids);
  }

  /**
   * Query bene exists by part d contract code and year-month.
   *
   * @param yearMonth the {@link BeneficiaryMonthly#getYearMonth()} value to match against
   * @param contractId the {@link BeneficiaryMonthly#getPartDContractNumberId()} value to match
   *     against
   * @return true if the {@link BeneficiaryMonthly} exists
   */
  private boolean queryBeneExistsByPartDContractCodeAndYearMonth(
      LocalDate yearMonth, String contractId) {
    // Create the query to run.
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<BeneficiaryMonthly> beneExistsCriteria =
        builder.createQuery(BeneficiaryMonthly.class);
    Root<BeneficiaryMonthly> beneMonthlyRoot = beneExistsCriteria.from(BeneficiaryMonthly.class);

    Subquery<Integer> beneExistsSubquery = beneExistsCriteria.subquery(Integer.class);
    Root<BeneficiaryMonthly> beneMonthlyRootSubquery =
        beneExistsSubquery.from(BeneficiaryMonthly.class);

    beneExistsSubquery
        .select(builder.literal(1))
        .where(
            builder.equal(beneMonthlyRootSubquery.get(BeneficiaryMonthly_.yearMonth), yearMonth),
            builder.equal(
                beneMonthlyRootSubquery.get(BeneficiaryMonthly_.partDContractNumberId),
                contractId));

    beneExistsCriteria.select(beneMonthlyRoot).where(builder.exists(beneExistsSubquery));

    // Run the query and return the results.
    boolean matchingBeneExists = false;
    Timer.Context matchingBeneExistsTimer =
        CommonTransformerUtils.createMetricsTimer(
            metricRegistry,
            getClass().getSimpleName(),
            "query",
            "bene_exists_by_year_month_part_d_contract_id");
    try {
      matchingBeneExists =
          entityManager.createQuery(beneExistsCriteria).setMaxResults(1).getResultList().stream()
              .findFirst()
              .isPresent();
      return matchingBeneExists;
    } finally {
      long beneHistoryMatchesTimerQueryNanoSeconds = matchingBeneExistsTimer.stop();
      CommonTransformerUtils.recordQueryInMdc(
          "bene_exists_by_year_month_part_d_contract_id",
          beneHistoryMatchesTimerQueryNanoSeconds,
          matchingBeneExists ? 1 : 0);
      // Close the resource
      matchingBeneExistsTimer.close();
    }
  }

  /**
   * Query beneficiary ids by part d contract code and year-month.
   *
   * @param yearMonth the {@link BeneficiaryMonthly#getYearMonth()} value to match against
   * @param contractId the {@link BeneficiaryMonthly#getPartDContractNumberId()} value to match
   *     against
   * @param paging the {@link PatientLinkBuilder} being used for paging
   * @return the {@link List} of matching {@link Beneficiary#getBeneficiaryId()} values
   */
  private List<Long> queryBeneficiaryIdsByPartDContractCodeAndYearMonth(
      LocalDate yearMonth, String contractId, PatientLinkBuilder paging) {
    // Create the query to run.
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Long> beneIdCriteria = builder.createQuery(Long.class);
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
    List<Long> matchingBeneIds = null;
    Timer.Context beneIdMatchesTimer =
        CommonTransformerUtils.createMetricsTimer(
            metricRegistry,
            getClass().getSimpleName(),
            "query",
            "bene_ids_by_year_month_part_d_contract_id");
    try {
      matchingBeneIds =
          entityManager
              .createQuery(beneIdCriteria)
              .setMaxResults(paging.getQueryMaxSize())
              .getResultList();
      return matchingBeneIds;
    } finally {
      long beneHistoryMatchesTimerQueryNanoSeconds = beneIdMatchesTimer.stop();
      CommonTransformerUtils.recordQueryInMdc(
          "bene_ids_by_year_month_part_d_contract_id",
          beneHistoryMatchesTimerQueryNanoSeconds,
          matchingBeneIds == null ? 0 : matchingBeneIds.size());
      beneIdMatchesTimer.close();
    }
  }

  /**
   * Query the DB for and return the matching {@link Beneficiary}s.
   *
   * @param ids the {@link Beneficiary#getBeneficiaryId()} values to match against
   * @return the matching {@link Beneficiary}s
   */
  private List<Beneficiary> queryBeneficiariesByIdsWithBeneficiaryMonthlys(List<Long> ids) {
    // Create the query to run.
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Beneficiary> beneCriteria = builder.createQuery(Beneficiary.class).distinct(true);

    Root<Beneficiary> beneRoot = beneCriteria.from(Beneficiary.class);
    beneRoot.fetch(Beneficiary_.beneficiaryHistories, JoinType.LEFT);
    beneCriteria.where(beneRoot.get(Beneficiary_.beneficiaryId).in(ids));

    // Run the query and return the results.
    List<Beneficiary> matchingBenes = null;
    Timer.Context beneIdTimer =
        CommonTransformerUtils.createMetricsTimer(
            metricRegistry,
            getClass().getSimpleName(),
            "query",
            "benes_by_year_month_part_d_contract_id");
    try {
      matchingBenes = entityManager.createQuery(beneCriteria).getResultList();
      return matchingBenes;
    } finally {
      long beneMatchesTimerQueryNanoSeconds = beneIdTimer.stop();
      CommonTransformerUtils.recordQueryInMdc(
          "benes_by_year_month_part_d_contract_id",
          beneMatchesTimerQueryNanoSeconds,
          matchingBenes == null ? 0 : matchingBenes.size());
      beneIdTimer.close();
    }
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
   * <p>This method supports both HTTP GET with URL parameters, and HTTP POST with parameters
   * specified within the POST body.
   *
   * @param identifier an {@link Identifier} {@link TokenParam} for the {@link
   *     Patient#getIdentifier()} to try and find a matching {@link Patient} for
   * @param startIndex an {@link OptionalParam} for the startIndex (or offset) used to determine
   *     pagination
   * @param count an {@link OptionalParam} for the count used in pagination
   * @param lastUpdated an {@link OptionalParam} to filter the results based on the passed date
   *     range
   * @param requestDetails a {@link RequestDetails} containing the details of the request URL, used
   *     to parse out pagination values
   * @return Returns a {@link List} of {@link Patient}s, which may contain multiple matching
   *     resources, or may also be empty.
   */
  @Search
  @RetryOnFailoverOrConnectionException
  public Bundle searchByIdentifier(
      @RequiredParam(name = Patient.SP_IDENTIFIER)
          @Description(shortDefinition = "The patient identifier to search for")
          TokenParam identifier,
      @OptionalParam(name = "startIndex")
          @Description(shortDefinition = "The offset used for result pagination")
          String startIndex,
      @OptionalParam(name = Constants.PARAM_COUNT)
          @Description(
              shortDefinition = OpenAPIContentProvider.COUNT_SHORT,
              value = OpenAPIContentProvider.COUNT_VALUE)
          String count,
      @OptionalParam(name = "_lastUpdated")
          @Description(shortDefinition = "Include resources last updated in the given range")
          DateRangeParam lastUpdated,
      RequestDetails requestDetails) {

    if (!SUPPORTED_HASH_IDENTIFIER_SYSTEMS.contains(identifier.getSystem())) {
      throw new InvalidRequestException("Unsupported identifier system: " + identifier.getSystem());
    }
    /*
     * Only allow a system identifier for http://hl7.org/fhir/sid/us-mbi; while we
     * can support HTTP GET here because we'll obfuscate the MBI_NUM prior to any
     * logging, we may want to throw an Exception if not a POST request.
     */
    boolean unhashedMbiLookup =
        (identifier.getSystem() != null
            && identifier
                .getSystem()
                .equals(TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED));

    if (unhashedMbiLookup && RequestTypeEnum.POST != requestDetails.getRequestType()) {
      throw new InvalidRequestException(
          String.format(
              "Search query by '%s' is only supported in POST request",
              TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED));
    }
    if (identifier.getQueryParameterQualifier() != null) {
      throw new InvalidRequestException(
          "Unsupported query parameter qualifier: " + identifier.getQueryParameterQualifier());
    }

    RequestHeaders requestHeader = RequestHeaders.getHeaderWrapper(requestDetails);
    CanonicalOperation operation = new CanonicalOperation(CanonicalOperation.Endpoint.V1_PATIENT);
    operation.setOption("by", "identifier");
    requestHeader.getNVPairs().forEach((n, v) -> operation.setOption(n, v.toString()));
    operation.setOption(
        "_lastUpdated", Boolean.toString(lastUpdated != null && !lastUpdated.isEmpty()));
    operation.publishOperationName();

    List<IBaseResource> patients =
        fetchPatientResourceByIdentifer(identifier, lastUpdated, requestHeader);

    OffsetLinkBuilder paging = new OffsetLinkBuilder(requestDetails, "/Patient?");
    Bundle bundle =
        TransformerUtils.createBundle(paging, patients, loadedFilterManager.getTransactionTime());

    // Add bene_id to MDC logs
    LoggingUtils.logBenesToMdc(bundle);
    return bundle;
  }

  /**
   * Handle processing for FHIR "search" operation for {@link Patient}s; allows users to search by
   * {@link Patient#getIdentifier()}. Specifically, the following criteria are supported:
   *
   * @param identifier a {@link TokenParam} for the {@link Patient#getIdentifier()} to try and find
   *     a matching {@link Patient} for
   * @param lastUpdated an {@link DateRangeParam} to filter the results based on the passed date
   *     range
   * @param requestHeader the {@link RequestHeaders} from the service call.
   * @return Returns a {@link List} of {@link Patient}s, which may contain multiple matching
   *     resources, or may also be empty.
   */
  private List<IBaseResource> fetchPatientResourceByIdentifer(
      TokenParam identifier, DateRangeParam lastUpdated, RequestHeaders requestHeader) {

    if (!SUPPORTED_HASH_IDENTIFIER_SYSTEMS.contains(identifier.getSystem())) {
      throw new InvalidRequestException("Unsupported identifier system: " + identifier.getSystem());
    }
    List<IBaseResource> patients = null;
    try {
      Patient patient;
      switch (identifier.getSystem()) {
        case TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED:
          patient =
              queryDatabaseByHashOrMbi(
                  identifier.getValue(),
                  "mbi",
                  Beneficiary_.medicareBeneficiaryId,
                  BeneficiaryHistory_.medicareBeneficiaryId,
                  requestHeader);
          break;
        case TransformerConstants.CODING_BBAPI_BENE_MBI_HASH:
          patient =
              queryDatabaseByHashOrMbi(
                  identifier.getValue(),
                  "mbi-hash",
                  Beneficiary_.mbiHash,
                  BeneficiaryHistory_.mbiHash,
                  requestHeader);
          break;
        case TransformerConstants.CODING_BBAPI_BENE_HICN_HASH:
        case TransformerConstants.CODING_BBAPI_BENE_HICN_HASH_OLD:
          patient =
              queryDatabaseByHashOrMbi(
                  identifier.getValue(),
                  "hicn-hash",
                  Beneficiary_.hicn,
                  BeneficiaryHistory_.hicn,
                  requestHeader);
          break;
        default:
          throw new InvalidRequestException(
              "Unsupported identifier system: " + identifier.getSystem());
      }

      patients =
          QueryUtils.isInRange(patient.getMeta().getLastUpdated().toInstant(), lastUpdated)
              ? Collections.singletonList(patient)
              : Collections.emptyList();
    } catch (NonUniqueResultException e) {
      throw new ResourceNotFoundException(e.getMessage());
    } catch (NoResultException e) {
      patients = new LinkedList<>();
    }
    return patients;
  }

  /**
   * Queries the database by the specified hash type.
   *
   * @param lookupValue the {@link Beneficiary} lookup value to match
   * @param lookupType a string to represent the lookup type (used for logging purposes)
   * @param beneficiaryField the JPA location of the beneficiary field
   * @param beneficiaryHistoryField the JPA location of the beneficiary history field
   * @param requestHeader the {@link RequestHeaders} where resource request headers are encapsulated
   * @return a FHIR {@link Patient} for the CCW {@link Beneficiary} that matches the specified
   *     {@link Beneficiary} hash value
   * @throws NoResultException A {@link NoResultException} will be thrown if no matching {@link
   *     Beneficiary} can be found
   */
  private Patient queryDatabaseByHashOrMbi(
      String lookupValue,
      String lookupType,
      SingularAttribute<Beneficiary, String> beneficiaryField,
      SingularAttribute<BeneficiaryHistory, String> beneficiaryHistoryField,
      RequestHeaders requestHeader) {
    if (lookupValue == null || lookupValue.trim().isEmpty()) {
      throw new InvalidRequestException("lookup value cannot be null/empty");
    }
    boolean includeIdentifiers =
        (requestHeader.isHICNinIncludeIdentifiers() || requestHeader.isMBIinIncludeIdentifiers());
    long beneId =
        CommonQueries.findBeneficiaryIdentifier(
            entityManager, metricRegistry, lookupType, lookupValue, getClass().getSimpleName());

    Beneficiary beneficiary =
        CommonQueries.findBeneficiary(
            entityManager,
            metricRegistry,
            beneId,
            includeIdentifiers,
            getClass().getSimpleName(),
            String.format(
                "bene_by_id_include_%s",
                String.join(
                    "_", (List<String>) requestHeader.getValue(HEADER_NAME_INCLUDE_IDENTIFIERS))));

    // Null out the unhashed HICNs if we're not supposed to be returning them
    if (!requestHeader.isHICNinIncludeIdentifiers()) {
      beneficiary.setHicnUnhashed(Optional.empty());
    }
    // Null out the unhashed MBIs if we're not supposed to be returning
    if (!requestHeader.isMBIinIncludeIdentifiers()) {
      beneficiary.setMedicareBeneficiaryId(Optional.empty());
    }

    return beneficiaryTransformer.transform(beneficiary, requestHeader);
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
   * Return a valid List of values for the IncludeIdenfifiers header.
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
    }
    // Return values split on a comma with any whitespace, valid, distict, and sort
    return Arrays.asList(splitOnCommas(headerValues.toLowerCase())).stream()
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
   * Check that coverageId value is valid.
   *
   * @param coverageId the coverage id
   * @throws InvalidRequestException if invalid coverageId
   */
  public static void checkCoverageId(TokenParam coverageId) {
    if (coverageId.getQueryParameterQualifier() != null) {
      throw new InvalidRequestException(
          "Unsupported query parameter qualifier: " + coverageId.getQueryParameterQualifier());
    }
    if (coverageId.getValueNotNull().length() != EXPECTED_COVERAGE_ID_LENGTH) {
      throw new InvalidRequestException(
          String.format(
              "Coverage id is not expected length; value %s is not expected length %s",
              coverageId.getValueNotNull(), EXPECTED_COVERAGE_ID_LENGTH));
    }
  }
}
