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
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Strings;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.data.fda.lookup.FdaDrugCodeDisplayLookup;
import gov.cms.bfd.data.npi.lookup.NPIOrgLookup;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.server.war.Operation;
import gov.cms.bfd.server.war.commons.AbstractResourceProvider;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import gov.cms.bfd.server.war.commons.LoggingUtils;
import gov.cms.bfd.server.war.commons.OffsetLinkBuilder;
import gov.cms.bfd.server.war.commons.QueryUtils;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.commons.TransformerContext;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private MetricRegistry metricRegistry;
  /** The samhsa matcher. */
  private R4EobSamhsaMatcher samhsaMatcher;
  /** The loaded filter manager. */
  private LoadedFilterManager loadedFilterManager;
  /** The drug code display lookup entity. */
  private FdaDrugCodeDisplayLookup drugCodeDisplayLookup;
  /** The npi org lookup entity. */
  private NPIOrgLookup npiOrgLookup;

  /**
   * Sets the {@link #entityManager}.
   *
   * @param entityManager a JPA {@link EntityManager} connected to the application's database
   */
  @PersistenceContext
  public void setEntityManager(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /**
   * Sets the {@link #metricRegistry}.
   *
   * @param metricRegistry the {@link MetricRegistry} to use
   */
  @Inject
  public void setMetricRegistry(MetricRegistry metricRegistry) {
    this.metricRegistry = metricRegistry;
  }

  /**
   * Sets the {@link #samhsaMatcher}.
   *
   * @param samhsaMatcher the {@link R4EobSamhsaMatcher} to use
   */
  @Inject
  public void setSamhsaFilterer(R4EobSamhsaMatcher samhsaMatcher) {
    this.samhsaMatcher = samhsaMatcher;
  }

  /**
   * Sets the {@link #loadedFilterManager}.
   *
   * @param loadedFilterManager the {@link LoadedFilterManager} to use
   */
  @Inject
  public void setLoadedFilterManager(LoadedFilterManager loadedFilterManager) {
    this.loadedFilterManager = loadedFilterManager;
  }

  /**
   * Sets the {@link #drugCodeDisplayLookup}.
   *
   * @param drugCodeDisplayLookup the {@link FdaDrugCodeDisplayLookup} to use
   */
  @Inject
  public void setdrugCodeDisplayLookup(FdaDrugCodeDisplayLookup drugCodeDisplayLookup) {
    this.drugCodeDisplayLookup = drugCodeDisplayLookup;
  }

  /**
   * Sets the {@link #npiOrgLookup}.
   *
   * @param npiOrgLookup the {@link NPIOrgLookup} to use
   */
  @Inject
  public void setNpiOrgLookup(NPIOrgLookup npiOrgLookup) {
    this.npiOrgLookup = npiOrgLookup;
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
    Optional<ClaimTypeV2> eobIdType = ClaimTypeV2.parse(eobIdTypeText);
    if (eobIdType.isEmpty()) throw new ResourceNotFoundException(eobId);
    String eobIdClaimIdText = eobIdMatcher.group(2);
    boolean includeTaxNumbers = returnIncludeTaxNumbers(requestDetails);
    Operation operation = new Operation(Operation.Endpoint.V2_EOB);
    operation.setOption("IncludeTaxNumbers", "" + includeTaxNumbers);
    operation.setOption("by", "id");
    operation.publishOperationName();

    Class<?> entityClass = eobIdType.get().getEntityClass();
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery criteria = builder.createQuery(entityClass);
    Root root = criteria.from(entityClass);
    eobIdType.get().getEntityLazyAttributes().stream().forEach(a -> root.fetch(a));
    criteria.select(root);
    criteria.where(
        builder.equal(root.get(eobIdType.get().getEntityIdAttribute()), eobIdClaimIdText));

    Object claimEntity = null;
    Long eobByIdQueryNanoSeconds = null;
    Timer.Context timerEobQuery =
        metricRegistry
            .timer(MetricRegistry.name(getClass().getSimpleName(), "query", "eob_by_id"))
            .time();
    try {
      claimEntity = entityManager.createQuery(criteria).getSingleResult();

      // Add number of resources to MDC logs
      LoggingUtils.logResourceCountToMdc(1);
    } catch (NoResultException e) {
      // Add number of resources to MDC logs
      LoggingUtils.logResourceCountToMdc(0);

      throw new ResourceNotFoundException(eobId);
    } finally {
      eobByIdQueryNanoSeconds = timerEobQuery.stop();
      TransformerUtilsV2.recordQueryInMdc(
          "eob_by_id", eobByIdQueryNanoSeconds, claimEntity == null ? 0 : 1);
    }

    ExplanationOfBenefit eob =
        eobIdType
            .get()
            .getTransformer()
            .transform(
                new TransformerContext(
                    metricRegistry,
                    Optional.of(includeTaxNumbers),
                    drugCodeDisplayLookup,
                    npiOrgLookup),
                claimEntity);

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
   * @param type a list of {@link ClaimTypeV2} to include in the result. Defaults to all types.
   * @param startIndex an {@link OptionalParam} for the startIndex (or offset) used to determine
   *     pagination
   * @param excludeSamhsa an {@link OptionalParam} that, if <code>"true"</code>, will use {@link
   *     R4EobSamhsaMatcher} to filter out all SAMHSA-related claims from the results
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
    Set<ClaimTypeV2> claimTypes = parseTypeParam(type);
    OffsetLinkBuilder paging = new OffsetLinkBuilder(requestDetails, "/ExplanationOfBenefit?");

    boolean includeTaxNumbers = returnIncludeTaxNumbers(requestDetails);
    Operation operation = new Operation(Operation.Endpoint.V2_EOB);
    operation.setOption("by", "patient");
    operation.setOption("IncludeTaxNumbers", "" + includeTaxNumbers);
    operation.setOption(
        "types",
        (claimTypes.size() == ClaimTypeV2.values().length)
            ? "*"
            : claimTypes.stream()
                .sorted(Comparator.comparing(ClaimTypeV2::name))
                .collect(Collectors.toList())
                .toString());
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

      return TransformerUtilsV2.createBundle(
          paging, eobs, loadedFilterManager.getTransactionTime());
    }

    /*
     * The way our JPA/SQL schema is setup, we have to run a separate search for
     * each claim type, then combine the results. It's not super efficient, but it's
     * also not so inefficient that it's worth fixing.
     */
    if (claimTypes.contains(ClaimTypeV2.CARRIER)) {
      eobs.addAll(
          transformToEobs(
              ClaimTypeV2.CARRIER,
              findClaimTypeByPatient(ClaimTypeV2.CARRIER, beneficiaryId, lastUpdated, serviceDate),
              Optional.of(includeTaxNumbers),
              drugCodeDisplayLookup,
              npiOrgLookup));
    }

    if (claimTypes.contains(ClaimTypeV2.DME)) {
      eobs.addAll(
          transformToEobs(
              ClaimTypeV2.DME,
              findClaimTypeByPatient(ClaimTypeV2.DME, beneficiaryId, lastUpdated, serviceDate),
              Optional.of(includeTaxNumbers),
              drugCodeDisplayLookup,
              npiOrgLookup));
    }

    if (claimTypes.contains(ClaimTypeV2.HHA)) {
      eobs.addAll(
          transformToEobs(
              ClaimTypeV2.HHA,
              findClaimTypeByPatient(ClaimTypeV2.HHA, beneficiaryId, lastUpdated, serviceDate),
              Optional.of(includeTaxNumbers),
              drugCodeDisplayLookup,
              npiOrgLookup));
    }

    if (claimTypes.contains(ClaimTypeV2.HOSPICE)) {
      eobs.addAll(
          transformToEobs(
              ClaimTypeV2.HOSPICE,
              findClaimTypeByPatient(ClaimTypeV2.HOSPICE, beneficiaryId, lastUpdated, serviceDate),
              Optional.of(includeTaxNumbers),
              drugCodeDisplayLookup,
              npiOrgLookup));
    }

    if (claimTypes.contains(ClaimTypeV2.INPATIENT)) {
      eobs.addAll(
          transformToEobs(
              ClaimTypeV2.INPATIENT,
              findClaimTypeByPatient(
                  ClaimTypeV2.INPATIENT, beneficiaryId, lastUpdated, serviceDate),
              Optional.of(includeTaxNumbers),
              drugCodeDisplayLookup,
              npiOrgLookup));
    }

    if (claimTypes.contains(ClaimTypeV2.OUTPATIENT)) {
      eobs.addAll(
          transformToEobs(
              ClaimTypeV2.OUTPATIENT,
              findClaimTypeByPatient(
                  ClaimTypeV2.OUTPATIENT, beneficiaryId, lastUpdated, serviceDate),
              Optional.of(includeTaxNumbers),
              drugCodeDisplayLookup,
              npiOrgLookup));
    }

    if (claimTypes.contains(ClaimTypeV2.PDE)) {
      eobs.addAll(
          transformToEobs(
              ClaimTypeV2.PDE,
              findClaimTypeByPatient(ClaimTypeV2.PDE, beneficiaryId, lastUpdated, serviceDate),
              Optional.of(includeTaxNumbers),
              drugCodeDisplayLookup,
              npiOrgLookup));
    }

    if (claimTypes.contains(ClaimTypeV2.SNF)) {
      eobs.addAll(
          transformToEobs(
              ClaimTypeV2.SNF,
              findClaimTypeByPatient(ClaimTypeV2.SNF, beneficiaryId, lastUpdated, serviceDate),
              Optional.of(includeTaxNumbers),
              drugCodeDisplayLookup,
              npiOrgLookup));
    }

    if (Boolean.parseBoolean(excludeSamhsa)) {
      filterSamhsa(eobs);
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
     * type). TODO once we have metadata from BLUEBUTTON-XXX on when each claim was
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
   * Find claim type by patient list.
   *
   * @param <T> the type parameter
   * @param claimType the {@link ClaimTypeV2} to find
   * @param patientId the {@link Beneficiary#getBeneficiaryId()} to filter by
   * @param lastUpdated the update time to filter by
   * @param serviceDate the service date
   * @return the matching claim/event entities
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  @Trace
  private <T> List<T> findClaimTypeByPatient(
      ClaimTypeV2 claimType,
      Long patientId,
      DateRangeParam lastUpdated,
      DateRangeParam serviceDate) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery criteria = builder.createQuery((Class) claimType.getEntityClass());
    Root root = criteria.from(claimType.getEntityClass());
    claimType.getEntityLazyAttributes().stream().forEach(a -> root.fetch(a));
    criteria.select(root).distinct(true);

    Predicate wherePredicate =
        builder.equal(root.get(claimType.getEntityBeneficiaryIdAttribute()), patientId);

    if (lastUpdated != null && !lastUpdated.isEmpty()) {
      Predicate predicate = QueryUtils.createLastUpdatedPredicate(builder, root, lastUpdated);
      wherePredicate = builder.and(wherePredicate, predicate);
    }
    criteria.where(wherePredicate);

    List<T> claimEntities = null;
    Long eobsByBeneIdQueryNanoSeconds = null;
    Timer.Context timerEobQuery =
        metricRegistry
            .timer(
                MetricRegistry.name(
                    metricRegistry.getClass().getSimpleName(),
                    "query",
                    "eobs_by_bene_id",
                    claimType.name().toLowerCase()))
            .time();
    try {
      claimEntities = entityManager.createQuery(criteria).getResultList();
    } finally {
      eobsByBeneIdQueryNanoSeconds = timerEobQuery.stop();
      TransformerUtilsV2.recordQueryInMdc(
          String.format("eobs_by_bene_id_%s", claimType.name().toLowerCase()),
          eobsByBeneIdQueryNanoSeconds,
          claimEntities == null ? 0 : claimEntities.size());
    }

    if (claimEntities != null && serviceDate != null && !serviceDate.isEmpty()) {
      final Instant lowerBound =
          serviceDate.getLowerBoundAsInstant() != null
              ? serviceDate.getLowerBoundAsInstant().toInstant()
              : null;
      final Instant upperBound =
          serviceDate.getUpperBoundAsInstant() != null
              ? serviceDate.getUpperBoundAsInstant().toInstant()
              : null;
      final java.util.function.Predicate<LocalDate> lowerBoundCheck =
          lowerBound == null
              ? (date) -> true
              : (date) ->
                  compareLocalDate(
                      date,
                      lowerBound.atZone(ZoneId.systemDefault()).toLocalDate(),
                      serviceDate.getLowerBound().getPrefix());
      final java.util.function.Predicate<LocalDate> upperBoundCheck =
          upperBound == null
              ? (date) -> true
              : (date) ->
                  compareLocalDate(
                      date,
                      upperBound.atZone(ZoneId.systemDefault()).toLocalDate(),
                      serviceDate.getUpperBound().getPrefix());
      return claimEntities.stream()
          .filter(
              entity ->
                  lowerBoundCheck.test(claimType.getServiceEndAttributeFunction().apply(entity))
                      && upperBoundCheck.test(
                          claimType.getServiceEndAttributeFunction().apply(entity)))
          .collect(Collectors.toList());
    }
    return claimEntities;
  }

  /**
   * Transform a list of claims to a list of {@link ExplanationOfBenefit} objects.
   *
   * <p>TODO: This should likely not exist in the provider class and be moved somewhere else like a
   * transformer class
   *
   * @param claimType the {@link ClaimTypeV2} being transformed
   * @param claims the claims/events to transform
   * @param includeTaxNumbers whether to include tax numbers in the response
   * @param drugCodeDisplayLookup the drug code display lookup
   * @param npiOrgLookup the npi org lookup
   * @return the transformed {@link ExplanationOfBenefit} instances, one for each specified
   *     claim/event
   */
  @Trace
  private List<ExplanationOfBenefit> transformToEobs(
      ClaimTypeV2 claimType,
      List<?> claims,
      Optional<Boolean> includeTaxNumbers,
      FdaDrugCodeDisplayLookup drugCodeDisplayLookup,
      NPIOrgLookup npiOrgLookup) {
    return claims.stream()
        .map(
            c ->
                claimType
                    .getTransformer()
                    .transform(
                        new TransformerContext(
                            metricRegistry, includeTaxNumbers, drugCodeDisplayLookup, npiOrgLookup),
                        c))
        .collect(Collectors.toList());
  }

  /**
   * Removes all SAMHSA-related claims from the specified {@link List} of {@link
   * ExplanationOfBenefit} resources.
   *
   * @param eobs the {@link List} of {@link ExplanationOfBenefit} resources (i.e. claims) to filter
   */
  private void filterSamhsa(List<IBaseResource> eobs) {
    ListIterator<IBaseResource> eobsIter = eobs.listIterator();
    while (eobsIter.hasNext()) {
      ExplanationOfBenefit eob = (ExplanationOfBenefit) eobsIter.next();
      if (samhsaMatcher.test(eob)) eobsIter.remove();
    }
  }

  /**
   * Compares {@link LocalDate} a against {@link LocalDate} using the supplied {@link
   * ParamPrefixEnum}.
   *
   * @param a the first item to compare
   * @param b the second item to compare
   * @param prefix prefix to use. Supported: {@link ParamPrefixEnum#GREATERTHAN_OR_EQUALS}, {@link
   *     ParamPrefixEnum#GREATERTHAN}, {@link ParamPrefixEnum#LESSTHAN_OR_EQUALS}, {@link
   *     ParamPrefixEnum#LESSTHAN}
   * @return true if the comparison between a and b returned true
   * @throws IllegalArgumentException if caller supplied an unsupported prefix
   */
  private boolean compareLocalDate(
      @Nullable LocalDate a, @Nullable LocalDate b, ParamPrefixEnum prefix) {
    if (a == null || b == null) {
      return false;
    }
    switch (prefix) {
      case GREATERTHAN_OR_EQUALS:
        return !a.isBefore(b);
      case GREATERTHAN:
        return a.isAfter(b);
      case LESSTHAN_OR_EQUALS:
        return !a.isAfter(b);
      case LESSTHAN:
        return a.isBefore(b);
      default:
        throw new InvalidRequestException(String.format("Unsupported prefix supplied: %s", prefix));
    }
  }

  /**
   * Parses the claim types to return in the search by parsing out the type tokens parameters.
   *
   * @param type a {@link TokenAndListParam} for the "type" field in a search
   * @return The {@link ClaimTypeV2}s to be searched, as computed from the specified "type" {@link
   *     TokenAndListParam} search param
   */
  static Set<ClaimTypeV2> parseTypeParam(TokenAndListParam type) {
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
    Set<ClaimTypeV2> claimTypes = new HashSet<ClaimTypeV2>(Arrays.asList(ClaimTypeV2.values()));
    for (TokenOrListParam typeToken : type.getValuesAsQueryTokens()) {
      /*
       * Each OR entry is additive: we start with an empty set and add every (valid)
       * ClaimType that's encountered.
       */
      Set<ClaimTypeV2> claimTypesInner = new HashSet<ClaimTypeV2>();
      for (TokenParam codingToken : typeToken.getValuesAsQueryTokens()) {
        if (codingToken.getModifier() != null) throw new IllegalArgumentException();

        /*
         * Per the FHIR spec (https://www.hl7.org/fhir/search.html), there are lots of
         * edge cases here: we could have null or wildcard or exact system, we can have
         * an exact or wildcard code. All of those need to be handled carefully -- see
         * the spec for details.
         */
        Optional<ClaimTypeV2> claimType =
            codingToken.getValue() != null
                ? ClaimTypeV2.parse(codingToken.getValue().toLowerCase())
                : Optional.empty();

        if (codingToken.getSystem() != null
            && codingToken.getSystem().equals(TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE)
            && !claimType.isPresent()) {
          claimTypesInner.addAll(Arrays.asList(ClaimTypeV2.values()));
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
