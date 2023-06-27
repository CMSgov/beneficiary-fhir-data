package gov.cms.bfd.server.war.stu3.providers;

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
import com.google.common.base.Strings;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.data.fda.lookup.FdaDrugCodeDisplayLookup;
import gov.cms.bfd.data.npi.lookup.NPIOrgLookup;
import gov.cms.bfd.server.war.CanonicalOperation;
import gov.cms.bfd.server.war.commons.AbstractResourceProvider;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import gov.cms.bfd.server.war.commons.LoggingUtils;
import gov.cms.bfd.server.war.commons.OffsetLinkBuilder;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * This FHIR {@link IResourceProvider} adds support for STU3 {@link ExplanationOfBenefit} resources,
 * derived from the CCW claims.
 */
@Component
public final class ExplanationOfBenefitResourceProvider extends AbstractResourceProvider
    implements IResourceProvider {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ExplanationOfBenefitResourceProvider.class);

  /** The entity manager. */
  private EntityManager entityManager;
  /** The metric registry. */
  private final MetricRegistry metricRegistry;
  /** The samhsa matcher. */
  private final Stu3EobSamhsaMatcher samhsaMatcher;
  /** The loaded filter manager. */
  private final LoadedFilterManager loadedFilterManager;
  /** The drug code display lookup entity. */
  private final FdaDrugCodeDisplayLookup drugCodeDisplayLookup;
  /** The npi org lookup entity. */
  private final NPIOrgLookup npiOrgLookup;
  /** The ExecutorService entity. */
  private final ExecutorService executorService;
  /** The mock spring application context. */
  private final ApplicationContext appContext;
  /** The transformer for carrier claims. */
  private final CarrierClaimTransformer carrierClaimTransformer;
  /** The transformer for dme claims. */
  private final DMEClaimTransformer dmeClaimTransformer;
  /** The transformer for hha claims. */
  private final HHAClaimTransformer hhaClaimTransformer;
  /** The transformer for hospice claims. */
  private final HospiceClaimTransformer hospiceClaimTransformer;
  /** The transformer for inpatient claims. */
  private final InpatientClaimTransformer inpatientClaimTransformer;
  /** The transformer for outpatient claims. */
  private final OutpatientClaimTransformer outpatientClaimTransformer;
  /** The transformer for part D events claims. */
  private final PartDEventTransformer partDEventTransformer;
  /** The transformer for snf claims. */
  private final SNFClaimTransformer snfClaimTransformer;

  /**
   * A {@link Pattern} that will match the {@link ExplanationOfBenefit#getId()}s used in this
   * application, e.g. <code>pde-1234</code> or <code>pde--1234</code> (for negative IDs).
   */
  private static final Pattern EOB_ID_PATTERN = Pattern.compile("(\\p{Alpha}+)-(-?\\p{Digit}+)");

  /**
   * Instantiates a new {@link ExplanationOfBenefitResourceProvider}.
   *
   * <p>Spring will wire this class during the initial component scan, so this constructor should
   * only be explicitly called by tests.
   *
   * @param appContext the spring application context
   * @param metricRegistry the metric registry bean
   * @param loadedFilterManager the loaded filter manager bean
   * @param samhsaMatcher the samsha matcher bean
   * @param drugCodeDisplayLookup the drug code display lookup bean
   * @param npiOrgLookup the npi org lookup bean
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
  public ExplanationOfBenefitResourceProvider(
      ApplicationContext appContext,
      MetricRegistry metricRegistry,
      LoadedFilterManager loadedFilterManager,
      Stu3EobSamhsaMatcher samhsaMatcher,
      FdaDrugCodeDisplayLookup drugCodeDisplayLookup,
      NPIOrgLookup npiOrgLookup,
      ExecutorService executorService,
      CarrierClaimTransformer carrierClaimTransformer,
      DMEClaimTransformer dmeClaimTransformer,
      HHAClaimTransformer hhaClaimTransformer,
      HospiceClaimTransformer hospiceClaimTransformer,
      InpatientClaimTransformer inpatientClaimTransformer,
      OutpatientClaimTransformer outpatientClaimTransformer,
      PartDEventTransformer partDEventTransformer,
      SNFClaimTransformer snfClaimTransformer) {
    this.appContext = requireNonNull(appContext);
    this.metricRegistry = requireNonNull(metricRegistry);
    this.loadedFilterManager = requireNonNull(loadedFilterManager);
    this.samhsaMatcher = requireNonNull(samhsaMatcher);
    this.drugCodeDisplayLookup = requireNonNull(drugCodeDisplayLookup);
    this.npiOrgLookup = requireNonNull(npiOrgLookup);
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
   * @param requestDetails a {@link RequestDetails} containing the details of the request URL, used
   *     to parse out header values
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
    if (!eobIdType.isPresent()) throw new ResourceNotFoundException(eobId);

    Optional<Boolean> includeTaxNumbers =
        Optional.ofNullable(returnIncludeTaxNumbers(requestDetails));
    String eobIdClaimIdText = eobIdMatcher.group(2);

    CanonicalOperation operation = new CanonicalOperation(CanonicalOperation.Endpoint.V1_EOB);
    operation.setOption("IncludeTaxNumbers", "" + includeTaxNumbers);
    operation.setOption("by", "id");
    operation.publishOperationName();

    CountDownLatch countDownLatch = new CountDownLatch(1);
    var task = appContext.getBean(PatientClaimsEobTaskTransformer.class);
    task.setupTaskParams(
        deriveTransformer(eobIdType.get()),
        eobIdType.get(),
        Long.parseLong(eobIdClaimIdText),
        true,
        Optional.empty(),
        Optional.empty(),
        includeTaxNumbers,
        false,
        countDownLatch);

    ExplanationOfBenefit eob = null;
    Future<PatientClaimsEobTaskTransformer> future = null;
    try {
      future = executorService.submit(task);
      // Wait for the latch to reach zero
      countDownLatch.await();
      PatientClaimsEobTaskTransformer taskResult = future.get();
      if (taskResult.ranSuccessfully()) {
        List<ExplanationOfBenefit> eobs = taskResult.fetchEOBs();
        eob = eobs.get(0);
      } else {
        LOGGER.error(taskResult.getFailure().get().getMessage(), taskResult.getFailure().get());
      }
    } catch (InterruptedException | ExecutionException e) {
      LOGGER.error("Error invoking executor service", e);
    }

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
   *     Stu3EobSamhsaMatcher} to filter out all SAMHSA-related claims from the results
   * @param lastUpdated an {@link OptionalParam} that specifies a date range for the lastUpdated
   *     field.
   * @param serviceDate an {@link OptionalParam} that specifies a date range for {@link
   *     ExplanationOfBenefit}s that completed
   * @param requestDetails a {@link RequestDetails} containing the details of the request URL, used
   *     to parse out pagination values
   * @return Returns a {@link Bundle} of {@link ExplanationOfBenefit}s, which may contain multiple
   *     matching resources, or may also be empty.
   */
  @Search
  @Trace
  public Bundle findByPatient(
      @RequiredParam(name = ExplanationOfBenefit.SP_PATIENT)
          @Description(shortDefinition = "The patient identifier to search for")
          ReferenceParam patient,
      @OptionalParam(name = "type")
          @Description(shortDefinition = "A list of claim types to include")
          TokenAndListParam type,
      @OptionalParam(name = "startIndex")
          @Description(shortDefinition = "The offset used for result pagination")
          String startIndex,
      @OptionalParam(name = "excludeSAMHSA")
          @Description(shortDefinition = "If true, exclude all SAMHSA-related resources")
          String excludeSamhsa,
      @OptionalParam(name = "_lastUpdated")
          @Description(shortDefinition = "Include resources last updated in the given range")
          DateRangeParam lastUpdated,
      @OptionalParam(name = "service-date")
          @Description(shortDefinition = "Include resources that completed in the given range")
          DateRangeParam serviceDate,
      RequestDetails requestDetails) {
    /*
     * startIndex is an optional parameter here because it must be declared in the
     * event it is passed in. However, it is not being used here because it is also
     * contained within requestDetails and parsed out along with other parameters
     * later.
     */

    Long beneficiaryId = Long.parseLong(patient.getIdPart());
    Set<ClaimType> claimTypesRequested = parseTypeParam(type);
    Optional<Boolean> includeTaxNumbers =
        Optional.ofNullable(returnIncludeTaxNumbers(requestDetails));
    OffsetLinkBuilder paging = new OffsetLinkBuilder(requestDetails, "/ExplanationOfBenefit?");

    CanonicalOperation operation = new CanonicalOperation(CanonicalOperation.Endpoint.V1_EOB);
    operation.setOption("by", "patient");
    operation.setOption(
        "types",
        (claimTypesRequested.size() == ClaimType.values().length)
            ? "*"
            : claimTypesRequested.stream()
                .sorted(Comparator.comparing(ClaimType::name))
                .collect(Collectors.toList())
                .toString());
    operation.setOption("IncludeTaxNumbers", "" + includeTaxNumbers);
    operation.setOption("pageSize", paging.isPagingRequested() ? "" + paging.getPageSize() : "*");
    operation.setOption(
        "_lastUpdated", Boolean.toString(lastUpdated != null && !lastUpdated.isEmpty()));
    operation.setOption(
        "service-date", Boolean.toString(serviceDate != null && !serviceDate.isEmpty()));
    operation.publishOperationName();

    List<IBaseResource> eobs = new ArrayList<IBaseResource>();

    // Optimize when the lastUpdated parameter is specified and result set is empty
    if (loadedFilterManager.isResultSetEmpty(beneficiaryId, lastUpdated)) {
      // Add bene_id to MDC logs when _lastUpdated filter is in effect
      LoggingUtils.logBeneIdToMdc(beneficiaryId);
      // Add number of resources to MDC logs
      LoggingUtils.logResourceCountToMdc(0);

      return TransformerUtils.createBundle(paging, eobs, loadedFilterManager.getTransactionTime());
    }

    // See if we have claims data for the beneficiary.
    Integer claimTypesThatHaveData = QueryUtils.availableClaimsData(entityManager, beneficiaryId);
    EnumSet<ClaimType> claimsToProcess =
        ClaimType.fetchClaimsAvailability(claimTypesRequested, claimTypesThatHaveData);

    LOGGER.debug(
        String.format(
            "EnumSet for V1 claims, bene_id %d: %s", beneficiaryId, claimsToProcess.toString()));

    if (claimsToProcess.isEmpty()) {
      return TransformerUtils.createBundle(paging, eobs, loadedFilterManager.getTransactionTime());
    }

    /*
     * The way our JPA/SQL schema is setup, we have to run a separate search for
     * each claim type, then combine the results. It's not super efficient, but it's
     * also not so inefficient that it's worth fixing.
     */
    CountDownLatch countDownLatch = new CountDownLatch(claimsToProcess.size());
    List<Callable<PatientClaimsEobTaskTransformer>> callableTasks = new ArrayList<>();

    for (ClaimType claimType : claimsToProcess) {
      var task = appContext.getBean(PatientClaimsEobTaskTransformer.class);
      task.setupTaskParams(
          deriveTransformer(claimType),
          claimType,
          beneficiaryId,
          false,
          Optional.ofNullable(lastUpdated),
          Optional.ofNullable(serviceDate),
          includeTaxNumbers,
          Boolean.parseBoolean(excludeSamhsa),
          countDownLatch);

      callableTasks.add(task);
    }

    List<Future<PatientClaimsEobTaskTransformer>> futures = null;
    try {
      futures = executorService.invokeAll(callableTasks);
      // Wait for the latch to reach zero
      countDownLatch.await();
    } catch (InterruptedException e) {
      LOGGER.error("Error invoking executor service", e);
    }

    for (Future<PatientClaimsEobTaskTransformer> future : futures) {
      try {
        PatientClaimsEobTaskTransformer taskResult = future.get();
        if (taskResult.ranSuccessfully()) {
          eobs.addAll(taskResult.fetchEOBs());
        } else {
          LOGGER.error(taskResult.getFailure().get().getMessage(), taskResult.getFailure().get());
        }
      } catch (InterruptedException | ExecutionException e) {
        LOGGER.error("Error getting future result", e);
      }
    }

    eobs.sort(ExplanationOfBenefitResourceProvider::compareByClaimIdThenClaimType);

    // Add bene_id to MDC logs
    LoggingUtils.logBeneIdToMdc(beneficiaryId);

    return TransformerUtils.createBundle(paging, eobs, loadedFilterManager.getTransactionTime());
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
     * type). TODO once we have metadata from BLUEBUTTON-XXX on when each claim was
     * first loaded into our DB, we should sort by that.
     */
    ExplanationOfBenefit eob1 = (ExplanationOfBenefit) res1;
    ExplanationOfBenefit eob2 = (ExplanationOfBenefit) res2;
    if (TransformerUtils.getUnprefixedClaimId(eob1)
        == TransformerUtils.getUnprefixedClaimId(eob2)) {
      return TransformerUtils.getClaimType(eob1).compareTo(TransformerUtils.getClaimType(eob2));
    } else {
      return TransformerUtils.getUnprefixedClaimId(eob1)
          .compareTo(TransformerUtils.getUnprefixedClaimId(eob2));
    }
  }

  /**
   * Return the EOB transfromer based on claim type.
   *
   * @param eobIdType the eob claim type
   * @return the transformed explanation of benefit
   */
  private ClaimTransformerInterface deriveTransformer(ClaimType eobIdType) {
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
  static Set<ClaimType> parseTypeParam(TokenAndListParam type) {
    if (type == null)
      type =
          new TokenAndListParam()
              .addAnd(
                  new TokenOrListParam()
                      .add(TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE, null));

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
        if (codingToken.getModifier() != null) {
          throw new InvalidRequestException("Cannot set modifier on field 'type'");
        }

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
          if (claimType.isPresent()) claimTypesInner.add(claimType.get());
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
