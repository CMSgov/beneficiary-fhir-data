package gov.cms.bfd.server.war.stu3.providers;

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
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.server.war.Operation;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import gov.cms.bfd.server.war.commons.OffsetLinkBuilder;
import gov.cms.bfd.server.war.commons.QueryUtils;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
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
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * This FHIR {@link IResourceProvider} adds support for STU3 {@link ExplanationOfBenefit} resources,
 * derived from the CCW claims.
 */
@Component
public final class ExplanationOfBenefitResourceProvider implements IResourceProvider {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ExplanationOfBenefitResourceProvider.class);

  /**
   * A {@link Pattern} that will match the {@link ExplanationOfBenefit#getId()}s used in this
   * application, e.g. <code>pde-1234</code> or <code>pde--1234</code> (for negative IDs).
   */
  private static final Pattern EOB_ID_PATTERN = Pattern.compile("(\\p{Alpha}+)-(-?\\p{Alnum}+)");

  public static final String HEADER_NAME_INCLUDE_TAX_NUMBERS = "IncludeTaxNumbers";

  private EntityManager entityManager;
  private MetricRegistry metricRegistry;
  private SamhsaMatcher samhsaMatcher;
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

  /** @param samhsaMatcher the {@link SamhsaMatcher} to use */
  @Inject
  public void setSamhsaFilterer(SamhsaMatcher samhsaMatcher) {
    this.samhsaMatcher = samhsaMatcher;
  }

  /** @param loadedFilterManager the {@link LoadedFilterManager} to use */
  @Inject
  public void setLoadedFilterManager(LoadedFilterManager loadedFilterManager) {
    this.loadedFilterManager = loadedFilterManager;
  }

  /** @see ca.uhn.fhir.rest.server.IResourceProvider#getResourceType() */
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
    if (eobId == null) throw new IllegalArgumentException();
    if (eobId.getVersionIdPartAsLong() != null) throw new IllegalArgumentException();

    String eobIdText = eobId.getIdPart();
    if (eobIdText == null || eobIdText.trim().isEmpty()) throw new IllegalArgumentException();

    Matcher eobIdMatcher = EOB_ID_PATTERN.matcher(eobIdText);
    if (!eobIdMatcher.matches())
      throw new IllegalArgumentException("Unsupported ID pattern: " + eobIdText);

    String eobIdTypeText = eobIdMatcher.group(1);
    Optional<ClaimType> eobIdType = ClaimType.parse(eobIdTypeText);

    Boolean includeTaxNumbers = returnIncludeTaxNumbers(requestDetails);

    if (!eobIdType.isPresent()) throw new ResourceNotFoundException(eobId);
    String eobIdClaimIdText = eobIdMatcher.group(2);

    Operation operation = new Operation(Operation.Endpoint.V1_EOB);
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
    } catch (NoResultException e) {
      throw new ResourceNotFoundException(eobId);
    } finally {
      eobByIdQueryNanoSeconds = timerEobQuery.stop();
      TransformerUtils.recordQueryInMdc(
          "eob_by_id", eobByIdQueryNanoSeconds, claimEntity == null ? 0 : 1);
    }

    ExplanationOfBenefit eob =
        eobIdType
            .get()
            .getTransformer()
            .transform(metricRegistry, claimEntity, Optional.of(includeTaxNumbers));
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
   *     SamhsaMatcher} to filter out all SAMHSA-related claims from the results
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

    String beneficiaryId = patient.getIdPart();
    Set<ClaimType> claimTypes = parseTypeParam(type);
    Boolean includeTaxNumbers = returnIncludeTaxNumbers(requestDetails);

    OffsetLinkBuilder paging = new OffsetLinkBuilder(requestDetails, "/ExplanationOfBenefit?");

    Operation operation = new Operation(Operation.Endpoint.V1_EOB);
    operation.setOption("by", "patient");
    operation.setOption(
        "types",
        (claimTypes.size() == ClaimType.values().length)
            ? "*"
            : claimTypes.stream()
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
      return TransformerUtils.createBundle(paging, eobs, loadedFilterManager.getTransactionTime());
    }

    /*
     * The way our JPA/SQL schema is setup, we have to run a separate search for
     * each claim type, then combine the results. It's not super efficient, but it's
     * also not so inefficient that it's worth fixing.
     */
    if (claimTypes.contains(ClaimType.CARRIER))
      eobs.addAll(
          transformToEobs(
              ClaimType.CARRIER,
              findClaimTypeByPatient(ClaimType.CARRIER, beneficiaryId, lastUpdated, serviceDate),
              Optional.of(includeTaxNumbers)));
    if (claimTypes.contains(ClaimType.DME))
      eobs.addAll(
          transformToEobs(
              ClaimType.DME,
              findClaimTypeByPatient(ClaimType.DME, beneficiaryId, lastUpdated, serviceDate),
              Optional.of(includeTaxNumbers)));
    if (claimTypes.contains(ClaimType.HHA))
      eobs.addAll(
          transformToEobs(
              ClaimType.HHA,
              findClaimTypeByPatient(ClaimType.HHA, beneficiaryId, lastUpdated, serviceDate),
              Optional.of(includeTaxNumbers)));
    if (claimTypes.contains(ClaimType.HOSPICE))
      eobs.addAll(
          transformToEobs(
              ClaimType.HOSPICE,
              findClaimTypeByPatient(ClaimType.HOSPICE, beneficiaryId, lastUpdated, serviceDate),
              Optional.of(includeTaxNumbers)));
    if (claimTypes.contains(ClaimType.INPATIENT))
      eobs.addAll(
          transformToEobs(
              ClaimType.INPATIENT,
              findClaimTypeByPatient(ClaimType.INPATIENT, beneficiaryId, lastUpdated, serviceDate),
              Optional.of(includeTaxNumbers)));
    if (claimTypes.contains(ClaimType.OUTPATIENT))
      eobs.addAll(
          transformToEobs(
              ClaimType.OUTPATIENT,
              findClaimTypeByPatient(ClaimType.OUTPATIENT, beneficiaryId, lastUpdated, serviceDate),
              Optional.of(includeTaxNumbers)));
    if (claimTypes.contains(ClaimType.PDE))
      eobs.addAll(
          transformToEobs(
              ClaimType.PDE,
              findClaimTypeByPatient(ClaimType.PDE, beneficiaryId, lastUpdated, serviceDate),
              Optional.of(includeTaxNumbers)));
    if (claimTypes.contains(ClaimType.SNF))
      eobs.addAll(
          transformToEobs(
              ClaimType.SNF,
              findClaimTypeByPatient(ClaimType.SNF, beneficiaryId, lastUpdated, serviceDate),
              Optional.of(includeTaxNumbers)));

    if (Boolean.parseBoolean(excludeSamhsa)) filterSamhsa(eobs);

    eobs.sort(ExplanationOfBenefitResourceProvider::compareByClaimIdThenClaimType);

    // Add bene_id to MDC logs
    MDC.put("bene_id", beneficiaryId);

    return TransformerUtils.createBundle(paging, eobs, loadedFilterManager.getTransactionTime());
  }

  /*
   * @param eob1 an {@link ExplanationOfBenefit} to be compared
   *
   * @param eob2 an {@link ExplanationOfBenefit} to be compared
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
   * @param claimType the {@link ClaimType} to find
   * @param patientId the {@link Beneficiary#getBeneficiaryId()} to filter by
   * @param lastUpdated the update time to filter by
   * @return the matching claim/event entities
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  @Trace
  private <T> List<T> findClaimTypeByPatient(
      ClaimType claimType,
      String patientId,
      DateRangeParam lastUpdated,
      DateRangeParam serviceDate) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery criteria = builder.createQuery((Class) claimType.getEntityClass());
    Root root = criteria.from(claimType.getEntityClass());
    claimType.getEntityLazyAttributes().stream().forEach(a -> root.fetch(a));
    criteria.select(root).distinct(true);

    // Search for a beneficiary's records. Use lastUpdated if present
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
      TransformerUtils.recordQueryInMdc(
          String.format("eobs_by_bene_id.%s", claimType.name().toLowerCase()),
          eobsByBeneIdQueryNanoSeconds,
          claimEntities == null ? 0 : claimEntities.size());
    }

    if (claimEntities != null && serviceDate != null && !serviceDate.isEmpty()) {
      final Date lowerBound = serviceDate.getLowerBoundAsInstant();
      final Date upperBound = serviceDate.getUpperBoundAsInstant();
      final java.util.function.Predicate<LocalDate> lowerBoundCheck =
          lowerBound == null
              ? (date) -> true
              : (date) ->
                  compareLocalDate(
                      date,
                      lowerBound.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                      serviceDate.getLowerBound().getPrefix());
      final java.util.function.Predicate<LocalDate> upperBoundCheck =
          upperBound == null
              ? (date) -> true
              : (date) ->
                  compareLocalDate(
                      date,
                      upperBound.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
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
   * @param claimType the {@link ClaimType} being transformed
   * @param claims the claims/events to transform
   * @return the transformed {@link ExplanationOfBenefit} instances, one for each specified
   *     claim/event
   */
  @Trace
  private List<ExplanationOfBenefit> transformToEobs(
      ClaimType claimType, List<?> claims, Optional<Boolean> includeTaxNumbers) {
    return claims.stream()
        .map(c -> claimType.getTransformer().transform(metricRegistry, c, includeTaxNumbers))
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
   * ParamPrefixEnum}
   *
   * @param a
   * @param b
   * @param prefix prefix to use. Supported: {@link ParamPrefixEnum#GREATERTHAN_OR_EQUALS}, {@link
   *     ParamPrefixEnum#GREATERTHAN}, {@link ParamPrefixEnum#LESSTHAN_OR_EQUALS}, {@link
   *     ParamPrefixEnum#LESSTHAN}
   * @return true if the comparison between a and b returned true.
   * @throws {@link IllegalArgumentException} if caller supplied an unsupported prefix
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
        throw new IllegalArgumentException(String.format("Unsupported prefix supplied %s", prefix));
    }
  }

  /**
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

  /**
   * @param requestDetails a {@link RequestDetails} containing the details of the request URL, used
   *     to parse out the HTTP header that controls this setting
   * @return <code>true</code> if {@link CarrierClaimColumn#TAX_NUM} and {@link
   *     DMEClaimColumn#TAX_NUM} should be mapped and included in the results, <code>false</code> if
   *     not (defaults to <code>false</code>)
   */
  public static boolean returnIncludeTaxNumbers(RequestDetails requestDetails) {
    /*
     * Note: headers can be multi-valued and so calling the enticing-looking `getHeader(...)` method
     * is often a bad idea, as it will often do the wrong thing.
     */
    List<String> headerValues = requestDetails.getHeaders(HEADER_NAME_INCLUDE_TAX_NUMBERS);

    if (headerValues == null || headerValues.isEmpty()) {
      return false;
    } else if (headerValues.size() == 1) {
      String headerValue = headerValues.get(0);
      if ("true".equalsIgnoreCase(headerValue)) {
        return true;
      } else if ("false".equalsIgnoreCase(headerValue)) {
        return false;
      }
    }

    throw new InvalidRequestException(
        "Unsupported " + HEADER_NAME_INCLUDE_TAX_NUMBERS + " header value: " + headerValues);
  }
}
