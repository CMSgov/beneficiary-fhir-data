package gov.cms.bfd.server.war.r4.providers.pac;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.rda.RdaFissClaim;
import gov.cms.bfd.model.rda.RdaMcsClaim;
import gov.cms.bfd.server.war.commons.AbstractResourceProvider;
import gov.cms.bfd.server.war.commons.OffsetLinkBuilder;
import gov.cms.bfd.server.war.commons.OpenAPIContentProvider;
import gov.cms.bfd.server.war.r4.providers.TransformerUtilsV2;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimDao;
import gov.cms.bfd.server.war.r4.providers.pac.common.ResourceTypeV2;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.ClaimResponse;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;

/**
 * Allows for generic processing of resource using common logic. Claims and ClaimResponses have the
 * exact same logic for looking up, transforming, and returning data.
 *
 * @param <T> The specific fhir resource the concrete provider will serve.
 */
public abstract class AbstractR4ResourceProvider<T extends IBaseResource>
    extends AbstractResourceProvider implements IResourceProvider {

  /**
   * A {@link Pattern} that will match the {@link ClaimResponse#getId()}s used in this application,
   * e.g. <code>f-1234</code> or <code>m--1234</code> (for negative IDs).
   */
  private static final Pattern CLAIM_ID_PATTERN = Pattern.compile("([fm])-(-?\\p{Digit}+)");

  /** The metric registry. */
  private final MetricRegistry metricRegistry;
  /** The samhsa matcher. */
  private final R4ClaimSamhsaMatcher samhsaMatcher;

  /** True if old MBI values should be included in queries. */
  private final Boolean oldMbiHashEnabled;

  /** The entity manager. */
  private EntityManager entityManager;

  /** The claim dao for this provider. */
  private ClaimDao claimDao;

  /** The resource type. */
  private Class<T> resourceType;

  /** The enabled source types for this provider. */
  private Set<String> enabledSourceTypes = new HashSet<>();

  /**
   * Initializes the resource provider beans via spring injection. These should be passed from the
   * child class constructor.
   *
   * @param metricRegistry the metric registry bean
   * @param samhsaMatcher the samhsa matcher bean
   * @param oldMbiHashEnabled true if old MBI hash should be used
   */
  protected AbstractR4ResourceProvider(
      MetricRegistry metricRegistry,
      R4ClaimSamhsaMatcher samhsaMatcher,
      Boolean oldMbiHashEnabled) {
    this.metricRegistry = metricRegistry;
    this.samhsaMatcher = samhsaMatcher;
    this.oldMbiHashEnabled = oldMbiHashEnabled;
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

  /** Initiates the provider's dependencies. */
  @PostConstruct
  public void init() {
    claimDao = new ClaimDao(entityManager, metricRegistry, oldMbiHashEnabled);

    setResourceType();
  }

  /**
   * Sets the allowed source types for this resource provider (i.e. FISS/MCS)
   *
   * @param enabledSourceTypes The {@link Set} of allowed source types.
   */
  public void setEnabledSourceTypes(Set<String> enabledSourceTypes) {
    this.enabledSourceTypes = enabledSourceTypes;
  }

  /** {@inheritDoc} */
  public Class<T> getResourceType() {
    return resourceType;
  }

  /** Sets the resource type. */
  @VisibleForTesting
  void setResourceType() {
    Type superClass = this.getClass().getGenericSuperclass();

    if (superClass instanceof ParameterizedType) {
      Type[] params = ((ParameterizedType) superClass).getActualTypeArguments();

      if (params[0] instanceof Class) {
        // unchecked - By principal, it shouldn't be possible for the parameter to not be of type T
        //noinspection unchecked
        resourceType = (Class<T>) params[0];
      } else {
        throw new IllegalStateException("Invalid parameterized type declaration");
      }
    } else {
      throw new IllegalStateException("Missing parameterized type declaration");
    }
  }

  /**
   * Adds support for the FHIR "read" operation, for {@link ClaimResponse}s. The {@link Read}
   * annotation indicates that this method supports the read operation.
   *
   * <p>Read operations take a single parameter annotated with {@link IdParam}, and should return a
   * single resource instance.
   *
   * @param claimId The read operation takes one parameter, which must be of type {@link IdType} and
   *     must be annotated with the {@link IdParam} annotation.
   * @param requestDetails the request details for the read
   * @return Returns a resource matching the specified {@link IdDt}, or <code>null</code> if none
   *     exists.
   */
  @Read
  @Trace
  public T read(@IdParam IdType claimId, RequestDetails requestDetails) {
    if (claimId == null) {
      throw new InvalidRequestException("Resource ID can not be null");
    }
    if (claimId.getVersionIdPartAsLong() != null) {
      throw new InvalidRequestException("Resource ID must not define a version");
    }
    final boolean includeTaxNumbers = returnIncludeTaxNumbers(requestDetails);

    if (claimId == null) throw new IllegalArgumentException("Resource ID can not be null");
    if (claimId.getVersionIdPartAsLong() != null)
      throw new IllegalArgumentException("Resource ID must not define a version.");

    String claimIdText = claimId.getIdPart();
    if (claimIdText == null || claimIdText.trim().isEmpty())
      throw new InvalidRequestException("Resource ID can not be null/blank");

    Matcher claimIdMatcher = CLAIM_ID_PATTERN.matcher(claimIdText);
    if (!claimIdMatcher.matches())
      throw new InvalidRequestException(
          "ID pattern: '"
              + claimIdText
              + "' does not match expected pattern: {singleCharacter}-{claimIdNumber}");

    String claimIdTypeText = claimIdMatcher.group(1);
    Optional<ResourceTypeV2<T, ?>> optional = parseClaimType(claimIdTypeText);
    if (optional.isEmpty()) throw new ResourceNotFoundException(claimId);
    ResourceTypeV2<T, ?> claimIdType = optional.get();
    String claimIdString = claimIdMatcher.group(2);

    Object claimEntity;

    try {
      claimEntity = claimDao.getEntityById(claimIdType, claimIdString);
    } catch (NoResultException e) {
      throw new ResourceNotFoundException(claimId);
    }

    return claimIdType.getTransformer().transform(metricRegistry, claimEntity, includeTaxNumbers);
  }

  /**
   * Implementation specific claim type parsing.
   *
   * @param typeText String to parse representing the claim type.
   * @return The parsed {@link ClaimResponseTypeV2} type.
   */
  @VisibleForTesting
  abstract Optional<ResourceTypeV2<T, ?>> parseClaimType(String typeText);

  /**
   * Creates a Set of {@link ResourceTypeV2} for the given claim types.
   *
   * @param types The types of claims to include
   * @return A Set of {@link ResourceTypeV2} claim types.
   */
  @VisibleForTesting
  @Nonnull
  Set<ResourceTypeV2<T, ?>> parseClaimTypes(@Nonnull TokenAndListParam types) {
    return types.getValuesAsQueryTokens().get(0).getValuesAsQueryTokens().stream()
        .map(TokenParam::getValue)
        .map(String::toLowerCase)
        .filter(enabledSourceTypes::contains)
        .map(getResourceTypeMap()::get)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  /**
   * Returns a {@link Set} of all supported resources types that are currently enabled.
   *
   * @return {@link Set} of all supported resources types that are currently enabled.
   */
  private Set<ResourceTypeV2<T, ?>> getResourceTypes() {
    return getDefinedResourceTypes().stream()
        .filter(type -> enabledSourceTypes.contains(type.getTypeLabel()))
        .collect(Collectors.toSet());
  }

  /**
   * Returns a {@link Set} of all supported resource types.
   *
   * @return {@link Set} of all supported resource types.
   */
  @VisibleForTesting
  abstract Set<ResourceTypeV2<T, ?>> getDefinedResourceTypes();

  /**
   * Returns implementation specific {@link ResourceTypeV2} map.
   *
   * @return The implementation specific {@link ResourceTypeV2} map.
   */
  @VisibleForTesting
  abstract Map<String, ResourceTypeV2<T, ?>> getResourceTypeMap();

  /**
   * Find by patient bundle.
   *
   * @param mbi the patient identifier to search for
   * @param types a list of claim types to include
   * @param startIndex the offset used for result pagination
   * @param hashed a boolean indicating whether the MBI is hashed
   * @param samhsa if {@code true}, exclude all SAMHSA-related resources
   * @param lastUpdated range which to include resources last updated within
   * @param serviceDate range which to include resources completed within
   * @param taxNumbers whether to include tax numbers in the response
   * @param requestDetails the request details
   * @return the bundle
   */
  @Search
  @Trace
  public Bundle findByPatient(
      @RequiredParam(name = "mbi")
          @Description(
              shortDefinition = OpenAPIContentProvider.PAC_MBI_SHORT,
              value = OpenAPIContentProvider.PAC_MBI_VALUE)
          ReferenceParam mbi,
      @OptionalParam(name = "type")
          @Description(
              shortDefinition = OpenAPIContentProvider.PAC_CLAIM_TYPE_SHORT,
              value = OpenAPIContentProvider.PAC_CLAIM_TYPE_VALUE)
          TokenAndListParam types,
      @OptionalParam(name = "startIndex")
          @Description(
              shortDefinition = OpenAPIContentProvider.PATIENT_START_INDEX_SHORT,
              value = OpenAPIContentProvider.PATIENT_START_INDEX_VALUE)
          String startIndex,
      @OptionalParam(name = "isHashed")
          @Description(
              shortDefinition = OpenAPIContentProvider.PAC_IS_HASHED,
              value = OpenAPIContentProvider.PAC_IS_HASHED_VALUE)
          String hashed,
      @OptionalParam(name = "excludeSAMHSA")
          @Description(
              shortDefinition = OpenAPIContentProvider.PAC_EXCLUDE_SAMSHA_SHORT,
              value = OpenAPIContentProvider.PAC_EXCLUDE_SAMSHA_VALUE)
          String samhsa,
      @OptionalParam(name = Constants.PARAM_LASTUPDATED)
          @Description(
              shortDefinition = OpenAPIContentProvider.PATIENT_LAST_UPDATED_SHORT,
              value = OpenAPIContentProvider.PATIENT_LAST_UPDATED_VALUE)
          DateRangeParam lastUpdated,
      @OptionalParam(name = "service-date")
          @Description(
              shortDefinition = OpenAPIContentProvider.PAC_SERVICE_DATE_SHORT,
              value = OpenAPIContentProvider.PAC_SERVICE_DATE_VALUE)
          DateRangeParam serviceDate,
      @OptionalParam(name = "includeTaxNumbers")
          @Description(
              shortDefinition = OpenAPIContentProvider.EOB_INCLUDE_TAX_NUMBERS_SHORT,
              value = OpenAPIContentProvider.EOB_INCLUDE_TAX_NUMBERS_VALUE)
          String taxNumbers,
      RequestDetails requestDetails) {
    if (mbi != null && !StringUtils.isBlank(mbi.getIdPart())) {
      String mbiString = mbi.getIdPart();

      Bundle bundleResource;

      boolean isHashed = !Boolean.FALSE.toString().equalsIgnoreCase(hashed);
      boolean excludeSamhsa = Boolean.TRUE.toString().equalsIgnoreCase(samhsa);
      boolean includeTaxNumbers = returnIncludeTaxNumbers(requestDetails);

      OffsetLinkBuilder paging =
          new OffsetLinkBuilder(
              requestDetails, String.format("/%s?", resourceType.getSimpleName()));

      if (isHashed) {
        TransformerUtilsV2.logMbiHashToMdc(mbiString);
      }

      BundleOptions bundleOptions = new BundleOptions(isHashed, excludeSamhsa, includeTaxNumbers);

      if (types != null) {
        bundleResource =
            createBundleFor(
                parseClaimTypes(types), mbiString, lastUpdated, serviceDate, paging, bundleOptions);
      } else {
        bundleResource =
            createBundleFor(
                getResourceTypes(), mbiString, lastUpdated, serviceDate, paging, bundleOptions);
      }

      return bundleResource;
    } else {
      throw new InvalidRequestException("Missing required field mbi");
    }
  }

  /**
   * Creates a Bundle of resources for the given data using the given {@link ResourceTypeV2}.
   *
   * @param resourceTypes The {@link ResourceTypeV2} data to retrieve.
   * @param mbi The mbi to look up associated data for.
   * @param lastUpdated Date range of desired lastUpdate values to retrieve data for.
   * @param serviceDate Date range of the desired service date to retrieve data for.
   * @param paging Pagination details for the bundle
   * @param bundleOptions Bundle related options that affect the results.
   * @return A Bundle with data found using the provided parameters.
   */
  @VisibleForTesting
  Bundle createBundleFor(
      Set<ResourceTypeV2<T, ?>> resourceTypes,
      String mbi,
      DateRangeParam lastUpdated,
      DateRangeParam serviceDate,
      OffsetLinkBuilder paging,
      BundleOptions bundleOptions) {
    List<T> resources = new ArrayList<>();

    for (ResourceTypeV2<T, ?> type : resourceTypes) {
      List<?> entities;

      entities =
          claimDao.findAllByMbiAttribute(
              type, mbi, bundleOptions.isHashed, lastUpdated, serviceDate);

      resources.addAll(
          entities.stream()
              .filter(e -> !bundleOptions.excludeSamhsa || hasNoSamhsaData(metricRegistry, e))
              .map(
                  e ->
                      type.getTransformer()
                          .transform(metricRegistry, e, bundleOptions.includeTaxNumbers))
              .collect(Collectors.toList()));
    }

    // Enforces a specific sorting for pagination that parities the EOB resource sorting.
    resources.sort(Comparator.comparing(r -> r.getIdElement().getIdPart()));

    Bundle bundle = new Bundle();
    bundle.setTotal(resources.size());

    if (paging.isPagingRequested()) {
      paging.setTotal(resources.size()).addLinks(bundle);
      int endIndex = Math.min(paging.getStartIndex() + paging.getPageSize(), resources.size());
      resources = resources.subList(paging.getStartIndex(), endIndex);
    }

    resources.forEach(
        c -> {
          Bundle.BundleEntryComponent entry = bundle.addEntry();
          entry.setResource((Resource) c);
        });

    return bundle;
  }

  /**
   * Determines if there are no samhsa entries in the claim.
   *
   * @param metricRegistry the metric registry
   * @param entity the claim to check
   * @return {@code true} if there are no samhsa entries in the claim
   */
  @VisibleForTesting
  boolean hasNoSamhsaData(MetricRegistry metricRegistry, Object entity) {
    Claim claim;

    if (entity instanceof RdaFissClaim) {
      claim = FissClaimTransformerV2.transform(metricRegistry, entity, false);
    } else if (entity instanceof RdaMcsClaim) {
      claim = McsClaimTransformerV2.transform(metricRegistry, entity, false);
    } else {
      throw new IllegalArgumentException(
          "Unsupported entity " + entity.getClass().getCanonicalName() + " for samhsa filtering");
    }

    return !samhsaMatcher.test(claim);
  }

  /** Helper class for passing bundle result options. */
  private static class BundleOptions {

    /** Indicates if the given MBI of the search request was hashed. */
    private final boolean isHashed;
    /** Indicates if SAMHSA data should be excluded from the bundle results. */
    private final boolean excludeSamhsa;
    /** Indicates if the tax numbers should be included in the bundle results. */
    private final boolean includeTaxNumbers;

    /**
     * Instantiates a new Bundle options.
     *
     * @param isHashed whether to hash the mbi
     * @param excludeSamhsa whether to exclude samhsa
     * @param includeTaxNumbers whether to include tax numbers
     */
    private BundleOptions(boolean isHashed, boolean excludeSamhsa, boolean includeTaxNumbers) {
      this.isHashed = isHashed;
      this.excludeSamhsa = excludeSamhsa;
      this.includeTaxNumbers = includeTaxNumbers;
    }
  }
}
