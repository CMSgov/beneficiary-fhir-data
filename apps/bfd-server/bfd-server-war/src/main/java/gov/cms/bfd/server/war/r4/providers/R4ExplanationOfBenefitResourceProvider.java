package gov.cms.bfd.server.war.r4.providers;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.server.war.CanonicalOperation;
import gov.cms.bfd.server.war.commons.AbstractResourceProvider;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import gov.cms.bfd.server.war.commons.LoggingUtils;
import gov.cms.bfd.server.war.commons.OffsetLinkBuilder;
import gov.cms.bfd.server.war.commons.OpenAPIContentProvider;
import gov.cms.bfd.server.war.commons.QueryUtils;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * This FHIR {@link IResourceProvider} adds support for R4 {@link ExplanationOfBenefit} resources,
 * derived from the CCW claims.
 */
@Component
public final class R4ExplanationOfBenefitResourceProvider extends AbstractResourceProvider
    implements IResourceProvider {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(R4ExplanationOfBenefitResourceProvider.class);

  /**
   * A {@link Pattern} that will match the {@link ExplanationOfBenefit#getId()}s used in this
   * application, e.g. <code>pde-1234</code> or <code>pde--1234</code> (for negative IDs).
   */
  private static final Pattern EOB_ID_PATTERN = Pattern.compile("(\\p{Alpha}+)-(-?\\p{Digit}+)");

  /** The entity manager. */
  private EntityManager entityManager;

  /** The metric registry. */
  private final MetricRegistry metricRegistry;

  /** The loaded filter manager. */
  private final LoadedFilterManager loadedFilterManager;

  /** The ExecutorService entity. */
  private final ExecutorService executorService;

  /** spring application context. */
  private final ApplicationContext appContext;

  /** The transformer for carrier claims. */
  private final CarrierClaimTransformerV2 carrierClaimTransformer;

  /** The transformer for dme claims. */
  private final DMEClaimTransformerV2 dmeClaimTransformer;

  /** The transformer for hha claims. */
  private final HHAClaimTransformerV2 hhaClaimTransformer;

  /** The transformer for hospice claims. */
  private final HospiceClaimTransformerV2 hospiceClaimTransformer;

  /** The transformer for inpatient claims. */
  private final InpatientClaimTransformerV2 inpatientClaimTransformer;

  /** The transformer for outpatient claims. */
  private final OutpatientClaimTransformerV2 outpatientClaimTransformer;

  /** The transformer for part D events claims. */
  private final PartDEventTransformerV2 partDEventTransformer;

  /** The transformer for snf claims. */
  private final SNFClaimTransformerV2 snfClaimTransformer;

  /**
   * Instantiates a new {@link R4ExplanationOfBenefitResourceProvider}.
   *
   * <p>Spring will wire this class during the initial component scan, so this constructor should
   * only be explicitly called by tests.
   *
   * @param appContext the spring application context
   * @param metricRegistry the metric registry bean
   * @param loadedFilterManager the loaded filter manager bean
   * @param executorService thread pool for running queries in parallel
   * @param carrierClaimTransformer the carrier claim transformer
   * @param dmeClaimTransformer the dme claim transformer
   * @param hhaClaimTransformer the hha claim transformer
   * @param hospiceClaimTransformer the hospice claim transformer
   * @param inpatientClaimTransformer the inpatient claim transformer
   * @param outpatientClaimTransformer the outpatient claim transformer
   * @param partDEventTransformer the part d event transformer
   * @param snfClaimTransformer the snf claim transformer
   */
  public R4ExplanationOfBenefitResourceProvider(
      ApplicationContext appContext,
      MetricRegistry metricRegistry,
      LoadedFilterManager loadedFilterManager,
      ExecutorService executorService,
      CarrierClaimTransformerV2 carrierClaimTransformer,
      DMEClaimTransformerV2 dmeClaimTransformer,
      HHAClaimTransformerV2 hhaClaimTransformer,
      HospiceClaimTransformerV2 hospiceClaimTransformer,
      InpatientClaimTransformerV2 inpatientClaimTransformer,
      OutpatientClaimTransformerV2 outpatientClaimTransformer,
      PartDEventTransformerV2 partDEventTransformer,
      SNFClaimTransformerV2 snfClaimTransformer) {
    this.appContext = requireNonNull(appContext);
    this.metricRegistry = requireNonNull(metricRegistry);
    this.loadedFilterManager = requireNonNull(loadedFilterManager);
    this.executorService = requireNonNull(executorService);
    this.carrierClaimTransformer = requireNonNull(carrierClaimTransformer);
    this.dmeClaimTransformer = requireNonNull(dmeClaimTransformer);
    this.hhaClaimTransformer = requireNonNull(hhaClaimTransformer);
    this.hospiceClaimTransformer = requireNonNull(hospiceClaimTransformer);
    this.inpatientClaimTransformer = requireNonNull(inpatientClaimTransformer);
    this.outpatientClaimTransformer = requireNonNull(outpatientClaimTransformer);
    this.partDEventTransformer = requireNonNull(partDEventTransformer);
    this.snfClaimTransformer = requireNonNull(snfClaimTransformer);
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
    return ExplanationOfBenefit.class;
  }

  /**
   * Adds support for the FHIR "read" operation, for {@link ExplanationOfBenefit}s. The {@link Read}
   * annotation indicates that this method supports the read operation.
   *
   * <p>Read operations take a single parameter annotated with {@link IdParam}, and should return a
   * single resource instance.
   *
   * @param eobId The read operation takes one parameter, which must be of type {@link IdType} and
   *     must be annotated with the {@link IdParam} annotation.
   * @param requestDetails the request details for the read
   * @return Returns a resource matching the specified {@link IdDt}, or <code>null</code> if none
   *     exists.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  @Read(version = false)
  @Trace
  public ExplanationOfBenefit read(@IdParam IdType eobId, RequestDetails requestDetails) {
    if (eobId == null) {
      throw new InvalidRequestException("Missing required ExplanationOfBenefit ID");
    }
    if (eobId.getVersionIdPartAsLong() != null) {
      throw new InvalidRequestException("ExplanationOfBenefit ID must not define a version");
    }

    String eobIdText = eobId.getIdPart();
    if (eobIdText == null || eobIdText.trim().isEmpty()) {
      throw new InvalidRequestException("Missing required ExplanationOfBenefit ID");
    }

    Matcher eobIdMatcher = EOB_ID_PATTERN.matcher(eobIdText);
    if (!eobIdMatcher.matches()) {
      throw new InvalidRequestException(
          "ExplanationOfBenefit ID pattern: '"
              + eobIdText
              + "' does not match expected pattern: {alphaString}-{idNumber}");
    }

    String eobIdTypeText = eobIdMatcher.group(1);
    Optional<ClaimType> eobIdType = ClaimType.parse(eobIdTypeText);
    if (eobIdType.isEmpty()) throw new ResourceNotFoundException(eobId);
    ClaimType claimType = eobIdType.get();
    String eobIdClaimIdText = eobIdMatcher.group(2);
    boolean includeTaxNumbers = returnIncludeTaxNumbers(requestDetails);
    CanonicalOperation operation = new CanonicalOperation(CanonicalOperation.Endpoint.V2_EOB);
    operation.setOption("IncludeTaxNumbers", "" + includeTaxNumbers);
    operation.setOption("by", "id");
    operation.publishOperationName();

    Class<?> entityClass = eobIdType.get().getEntityClass();
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery criteria = builder.createQuery(entityClass);
    Root root = criteria.from(entityClass);
    eobIdType.get().getEntityLazyAttributes().stream().forEach(a -> root.fetch(a));
    criteria.select(root);
    criteria.where(builder.equal(root.get(claimType.getEntityIdAttribute()), eobIdClaimIdText));

    Object claimEntity = null;
    Timer.Context timerEobQuery =
        CommonTransformerUtils.createMetricsTimer(
            metricRegistry, getClass().getSimpleName(), "query", "eob_by_id");
    try {
      claimEntity = entityManager.createQuery(criteria).getSingleResult();

      // Add number of resources to MDC logs
      LoggingUtils.logResourceCountToMdc(1);
    } catch (NoResultException e) {
      // Add number of resources to MDC logs
      LoggingUtils.logResourceCountToMdc(0);
      throw new ResourceNotFoundException(eobId);
    } finally {
      long eobByIdQueryNanoSeconds = timerEobQuery.stop();
      CommonTransformerUtils.recordQueryInMdc(
          "eob_by_id", eobByIdQueryNanoSeconds, claimEntity == null ? 0 : 1);
    }

    ClaimTransformerInterfaceV2 transformer = deriveTransformer(claimType);
    ExplanationOfBenefit eob = transformer.transform(claimEntity, includeTaxNumbers);

    // Add bene_id to MDC logs
    if (eob.getPatient() != null && !Strings.isNullOrEmpty(eob.getPatient().getReference())) {
      String beneficiaryId = eob.getPatient().getReference().replace("Patient/", "");
      if (!Strings.isNullOrEmpty(beneficiaryId)) {
        LoggingUtils.logBeneIdToMdc(beneficiaryId);
      }
    }
    return eob;
  }

  /**
   * Adds support for the FHIR "search" operation for {@link ExplanationOfBenefit}s, allowing users
   * to search by {@link ExplanationOfBenefit#getPatient()}.
   *
   * <p>The {@link Search} annotation indicates that this method supports the search operation.
   * There may be many different methods annotated with this {@link Search} annotation, to support
   * many different search criteria.
   *
   * @param patient a {@link ReferenceParam} for the {@link ExplanationOfBenefit#getPatient()} to
   *     try and find matches for {@link ExplanationOfBenefit}s
   * @param type a list of {@link ClaimType} to include in the result. Defaults to all types.
   * @param startIndex an {@link OptionalParam} for the startIndex (or offset) used to determine
   *     pagination
   * @param excludeSamhsa an {@link OptionalParam} that, if <code>"true"</code>, will use {@link
   *     R4EobSamhsaMatcher} to filter out all SAMHSA-related claims from the results
   * @param lastUpdated an {@link OptionalParam} that specifies a date range for the lastUpdated
   *     field.
   * @param serviceDate an {@link OptionalParam} that specifies a date range for {@link
   *     ExplanationOfBenefit}s that completed
   * @param taxNumbers an {@link OptionalParam} for whether to include tax numbers in the response
   * @param requestDetails a {@link RequestDetails} containing the details of the request URL, used
   *     to parse out pagination values
   * @return Returns a {@link Bundle} of {@link ExplanationOfBenefit}s, which may contain multiple
   *     matching resources, or may also be empty.
   */
  @Search
  @Trace
  public Bundle findByPatient(
      @RequiredParam(name = ExplanationOfBenefit.SP_PATIENT)
          @Description(
              shortDefinition = OpenAPIContentProvider.PATIENT_SP_RES_ID_SHORT,
              value = OpenAPIContentProvider.PATIENT_SP_RES_ID_VALUE)
          ReferenceParam patient,
      @OptionalParam(name = "type")
          @Description(
              shortDefinition = OpenAPIContentProvider.EOB_CLAIM_TYPE_SHORT,
              value = OpenAPIContentProvider.EOB_CLAIM_TYPE_VALUE)
          TokenAndListParam type,
      @OptionalParam(name = "startIndex")
          @Description(
              shortDefinition = OpenAPIContentProvider.PATIENT_START_INDEX_SHORT,
              value = OpenAPIContentProvider.PATIENT_START_INDEX_VALUE)
          String startIndex,
      @OptionalParam(name = "excludeSAMHSA")
          @Description(
              shortDefinition = OpenAPIContentProvider.EOB_EXCLUDE_SAMSHA_SHORT,
              value = OpenAPIContentProvider.EOB_EXCLUDE_SAMSHA_VALUE)
          String excludeSamhsa,
      @OptionalParam(name = "_lastUpdated")
          @Description(
              shortDefinition = OpenAPIContentProvider.PATIENT_LAST_UPDATED_SHORT,
              value = OpenAPIContentProvider.PATIENT_LAST_UPDATED_VALUE)
          DateRangeParam lastUpdated,
      @OptionalParam(name = "service-date")
          @Description(
              shortDefinition = OpenAPIContentProvider.EOB_SERVICE_DATE_SHORT,
              value = OpenAPIContentProvider.EOB_SERVICE_DATE_VALUE)
          DateRangeParam serviceDate,
      @OptionalParam(name = "includeTaxNumbers")
          @Description(
              shortDefinition = OpenAPIContentProvider.EOB_INCLUDE_TAX_NUMBERS_SHORT,
              value = OpenAPIContentProvider.EOB_INCLUDE_TAX_NUMBERS_VALUE)
          String taxNumbers,
      RequestDetails requestDetails) {

    /*
     * startIndex is an optional parameter here because it must be declared in the
     * event it is passed in. However, it is not being used here because it is also
     * contained within requestDetails and parsed out along with other parameters
     * later.
     */
    OffsetLinkBuilder paging = new OffsetLinkBuilder(requestDetails, "/ExplanationOfBenefit?");
    Long beneficiaryId = Long.parseLong(patient.getIdPart());
    Set<ClaimType> claimTypesRequested = parseTypeParam(type);
    boolean includeTaxNumbers = returnIncludeTaxNumbers(requestDetails);
    boolean filterSamhsa = Boolean.parseBoolean(excludeSamhsa);
    CanonicalOperation operation = new CanonicalOperation(CanonicalOperation.Endpoint.V2_EOB);
    operation.setOption("by", "patient");
    operation.setOption("IncludeTaxNumbers", String.valueOf(includeTaxNumbers));
    operation.setOption(
        "types",
        (claimTypesRequested.size() == ClaimType.values().length)
            ? "*"
            : claimTypesRequested.stream()
                .sorted(Comparator.comparing(ClaimType::name))
                .toList()
                .toString());
    operation.setOption(
        "pageSize", paging.isPagingRequested() ? String.valueOf(paging.getPageSize()) : "*");
    operation.setOption(
        "_lastUpdated", Boolean.toString(lastUpdated != null && !lastUpdated.isEmpty()));
    operation.setOption(
        "service-date", Boolean.toString(serviceDate != null && !serviceDate.isEmpty()));
    operation.publishOperationName();

    // Optimize when the lastUpdated parameter is specified and result set is empty
    if (loadedFilterManager.isResultSetEmpty(beneficiaryId, lastUpdated)) {
      // Add bene_id to MDC logs when _lastUpdated filter is in effect
      LoggingUtils.logBeneIdToMdc(beneficiaryId);
      // Add number of resources to MDC logs
      LoggingUtils.logResourceCountToMdc(0);

      return TransformerUtilsV2.createBundle(
          paging, new ArrayList<IBaseResource>(), loadedFilterManager.getTransactionTime());
    }

    // See if we have any claims data for the beneficiary.
    int claimTypesThatHaveData = QueryUtils.availableClaimsData(entityManager, beneficiaryId);
    Bundle bundle = null;
    if (claimTypesThatHaveData > 0) {
      try {
        bundle =
            processClaimsMask(
                claimTypesThatHaveData,
                claimTypesRequested,
                beneficiaryId,
                paging,
                Optional.ofNullable(lastUpdated),
                Optional.ofNullable(serviceDate),
                filterSamhsa,
                includeTaxNumbers);
      } catch (InvalidRequestException e) {
        // If we're throwing a 400, pass it back up
        throw e;
      } catch (Exception e) {
        LOGGER.error(e.getMessage(), e);
      }
    }
    if (bundle == null) {
      LoggingUtils.logBeneIdToMdc(beneficiaryId);
      LoggingUtils.logResourceCountToMdc(0);
      bundle =
          TransformerUtilsV2.createBundle(
              paging, new ArrayList<IBaseResource>(), loadedFilterManager.getTransactionTime());
    }
    return bundle;
  }

  /**
   * Process the available claims mask value denoting which claims to process in parallel.
   *
   * @param claimTypesThatHaveData an {@link Integer} denoting the claim types to process.
   * @param claimTypesRequested a {@link Set} of {@link ClaimType} denoting requested claim types.
   * @param beneficiaryId a {@link Long} patient bene_id value.
   * @param paging a {@link OffsetLinkBuilder} for the startIndex (or offset) when using pagination.
   * @param lastUpdated a {@link DateRangeParam} denoting inclusion of lastUpdated field.
   * @param serviceDate a {@link DateRangeParam} specifying date range for the {@link
   *     ExplanationOfBenefit}s that completed.
   * @param excludeSamhsa optional {@link Boolean} denoting use of {@link R4EobSamhsaMatcher} *
   *     filtering of all SAMHSA-related claims from the results.
   * @param includeTaxNumbers an {@link Optional} boolean denoting includsio/exclusion of tax
   *     numbers in the response,
   * @return Returns a {@link Bundle} of {@link ExplanationOfBenefit}s, which may contain multiple
   *     matching resources, or may also be empty.
   * @throws InterruptedException when thread processing task is interrupted.
   * @throws ExecutionException when when executor fails to create thread.
   */
  @VisibleForTesting
  private Bundle processClaimsMask(
      int claimTypesThatHaveData,
      Set<ClaimType> claimTypesRequested,
      long beneficiaryId,
      OffsetLinkBuilder paging,
      Optional<DateRangeParam> lastUpdated,
      Optional<DateRangeParam> serviceDate,
      boolean excludeSamhsa,
      boolean includeTaxNumbers)
      throws InterruptedException, ExecutionException {

    EnumSet<ClaimType> claimsToProcess =
        TransformerUtilsV2.fetchClaimsAvailability(claimTypesRequested, claimTypesThatHaveData);
    LOGGER.debug(
        String.format("EnumSet for V2 claims, bene_id %d: %s", beneficiaryId, claimsToProcess));

    // OK to return null, since fallback will create bundle and log things.
    if (claimsToProcess.isEmpty()) {
      return null;
    }

    List<IBaseResource> eobs = new ArrayList<IBaseResource>();

    /*
     * The way our JPA/SQL schema is setup, we have to run a separate search for
     * each claim type, then combine the results. It's not super efficient, but it's
     * also not so inefficient that it's worth fixing.
     */
    List<Callable<PatientClaimsEobTaskTransformerV2>> callableTasks =
        new ArrayList<>(claimsToProcess.size());
    /*
     * We create the task bean by directly invoking the applications Spring
     * ApplicationContext to provide the bean; this is necessary as the tasks
     * will run concurrently and each task will need its own instance of an
     * {@link EntityManager}.
     */
    claimsToProcess.forEach(
        claimType -> {
          PatientClaimsEobTaskTransformerV2 task =
              appContext.getBean(PatientClaimsEobTaskTransformerV2.class);

          task.setupTaskParams(
              deriveTransformer(claimType),
              claimType,
              beneficiaryId,
              lastUpdated,
              serviceDate,
              excludeSamhsa);

          task.setIncludeTaxNumbers(includeTaxNumbers);
          callableTasks.add(task);
        });

    List<Future<PatientClaimsEobTaskTransformerV2>> futures;
    try {
      futures = executorService.invokeAll(callableTasks);
    } catch (InterruptedException e) {
      throw e;
    }

    for (Future<PatientClaimsEobTaskTransformerV2> future : futures) {
      try {
        PatientClaimsEobTaskTransformerV2 taskResult = future.get();
        if (taskResult.ranSuccessfully()) {
          eobs.addAll(taskResult.fetchEOBs());
        } else {
          Throwable taskError = taskResult.getFailure().get();
          throw new RuntimeException(taskError);
        }
      } catch (InterruptedException | ExecutionException e) {
        throw e;
      }
    }
    eobs.sort(R4ExplanationOfBenefitResourceProvider::compareByClaimIdThenClaimType);

    // Add bene_id to MDC logs
    LoggingUtils.logBeneIdToMdc(beneficiaryId);
    return TransformerUtilsV2.createBundle(paging, eobs, loadedFilterManager.getTransactionTime());
  }

  /**
   * Compare two EOB resources by claim id and claim type.
   *
   * @param res1 an {@link ExplanationOfBenefit} to be compared
   * @param res2 an {@link ExplanationOfBenefit} to be compared
   * @return the comparison result
   */
  private static int compareByClaimIdThenClaimType(IBaseResource res1, IBaseResource res2) {
    /*
     * In order for paging to be meaningful (and stable), the claims have to be
     * consistently sorted across different app server instances (in case page 1
     * comes from Server A but page 2 comes from Server B). Right now, we don't have
     * anything "useful" to sort by, so we just sort by claim ID (subsorted by claim
     * type).
     *
     * TODO once we have metadata from BLUEBUTTON-XXX on when each claim was
     * first loaded into our DB, we should sort by that.
     */
    ExplanationOfBenefit eob1 = (ExplanationOfBenefit) res1;
    ExplanationOfBenefit eob2 = (ExplanationOfBenefit) res2;
    if (TransformerUtilsV2.getUnprefixedClaimId(eob1)
        .equals(TransformerUtilsV2.getUnprefixedClaimId(eob2))) {
      return TransformerUtilsV2.getClaimType(eob1).compareTo(TransformerUtilsV2.getClaimType(eob2));
    } else {
      return TransformerUtilsV2.getUnprefixedClaimId(eob1)
          .compareTo(TransformerUtilsV2.getUnprefixedClaimId(eob2));
    }
  }

  /**
   * Return the EOB transfromer based on claim type.
   *
   * @param eobIdType the eob claim type
   * @return the transformed explanation of benefit
   */
  @VisibleForTesting
  private ClaimTransformerInterfaceV2 deriveTransformer(ClaimType eobIdType) {
    switch (eobIdType) {
      case CARRIER:
        return carrierClaimTransformer;
      case DME:
        return dmeClaimTransformer;
      case HHA:
        return hhaClaimTransformer;
      case HOSPICE:
        return hospiceClaimTransformer;
      case INPATIENT:
        return inpatientClaimTransformer;
      case OUTPATIENT:
        return outpatientClaimTransformer;
      case PDE:
        return partDEventTransformer;
      case SNF:
        return snfClaimTransformer;
    }
    return null;
  }

  /**
   * Parses the claim types to return in the search by parsing out the type tokens parameters.
   *
   * @param type a {@link TokenAndListParam} for the "type" field in a search
   * @return The {@link ClaimType}s to be searched, as computed from the specified "type" {@link
   *     TokenAndListParam} search param
   */
  public static Set<ClaimType> parseTypeParam(TokenAndListParam type) {
    if (type == null) {
      type =
          new TokenAndListParam()
              .addAnd(
                  new TokenOrListParam()
                      .add(TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE, null));
    }

    /*
     * This logic kinda' stinks, but HAPI forces us to handle some odd query
     * formulations, e.g. (in postfix notation):
     * "and(or(claimType==FOO, claimType==BAR), or(claimType==FOO))".
     */
    Set<ClaimType> claimTypes = new HashSet<ClaimType>(Arrays.asList(ClaimType.values()));
    for (TokenOrListParam typeToken : type.getValuesAsQueryTokens()) {
      /*
       * Each OR entry is additive: we start with an empty set and add every (valid)
       * ClaimType that's encountered.
       */
      Set<ClaimType> claimTypesInner = new HashSet<ClaimType>();
      for (TokenParam codingToken : typeToken.getValuesAsQueryTokens()) {
        if (codingToken.getModifier() != null) throw new IllegalArgumentException();

        /*
         * Per the FHIR spec (https://www.hl7.org/fhir/search.html), there are lots of
         * edge cases here: we could have null or wildcard or exact system, we can have
         * an exact or wildcard code. All of those need to be handled carefully -- see
         * the spec for details.
         */
        Optional<ClaimType> claimType =
            codingToken.getValue() != null
                ? ClaimType.parse(codingToken.getValue().toLowerCase())
                : Optional.empty();

        if (codingToken.getSystem() != null
            && codingToken.getSystem().equals(TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE)
            && !claimType.isPresent()) {
          claimTypesInner.addAll(Arrays.asList(ClaimType.values()));
        } else if (codingToken.getSystem() == null
            || codingToken.getSystem().equals(TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE)) {
          if (claimType.isPresent()) {
            claimTypesInner.add(claimType.get());
          }
        }
      }

      /*
       * All multiple AND parameters will do is reduce the number of possible matches.
       */
      claimTypes.retainAll(claimTypesInner);
    }

    return claimTypes;
  }
}
