package gov.cms.bfd.server.war.r4.providers.pac;

import static java.util.Objects.requireNonNull;

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
import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.server.sharedutils.BfdMDC;
import gov.cms.bfd.server.war.SamhsaV2InterceptorShadow;
import gov.cms.bfd.server.war.commons.AbstractResourceProvider;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.OffsetLinkBuilder;
import gov.cms.bfd.server.war.commons.OpenAPIContentProvider;
import gov.cms.bfd.server.war.commons.RetryOnFailoverOrConnectionException;
import gov.cms.bfd.server.war.commons.SecurityTagsDao;
import gov.cms.bfd.server.war.r4.providers.TransformerUtilsV2;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimDao;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import gov.cms.bfd.server.war.r4.providers.pac.common.ResourceTransformer;
import gov.cms.bfd.server.war.r4.providers.pac.common.ResourceTypeV2;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.util.Strings;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
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
   * e.g. <code>f-1234</code> or <code>m--1234</code> (for negative IDs). e.g. <code>
   * f-LTA0M2EyNWU2YjM0MmRmOTczY2YyYjU</code> or <code>m--00009127422efa</code> (for negative IDs).
   *
   * <p>Observation of data on ENVs, prod, prod-sbx, test: mcs_claim table, column idr_clm_hd_idn
   * char(15): -00009127422efa, -100000103 fiss_claim table, column claim_id char(43):
   * LTA0M2EyNWU2YjM0MmRmOTczY2YyYjU, MjIzMzYzMDQzOTIyMDdDQUFZICAgICAwMTAxMQ
   */
  private static final Pattern CLAIM_ID_PATTERN = Pattern.compile("^([fm])-(-?\\p{Alnum}+)$");

  // note, per observation: the mcs claim id (column idr_clm_hd_idn) is not digit only as shown
  // above sample values

  /** The metric registry. */
  private final MetricRegistry metricRegistry;

  /** The samhsa matcher. */
  private final R4ClaimSamhsaMatcher samhsaMatcher;

  /** v2SamhsaConsentSimulation. */
  private final SamhsaV2InterceptorShadow samhsaV2InterceptorShadow;

  /** True if old MBI values should be included in queries. */
  private final Boolean oldMbiHashEnabled;

  /** The entity manager. */
  private EntityManager entityManager;

  /** The claim dao for this provider. */
  private ClaimDao claimDao;

  /** The resource type. */
  private Class<T> resourceType;

  /** The enabled source types for this provider. */
  private final Set<String> enabledSourceTypes;

  /** The fiss transformer. */
  private final ResourceTransformer<T> fissTransformer;

  /** The mcs transformer. */
  private final ResourceTransformer<T> mcsTransformer;

  /** Flag to control whether SAMHSA shadow filtering should be applied. */
  private final boolean samhsaV2Shadow;

  private final SecurityTagsDao securityTagsDao;

  /**
   * Initializes the resource provider beans via spring injection. These should be passed from the
   * child class constructor.
   *
   * @param metricRegistry the metric registry bean
   * @param samhsaMatcher the samhsa matcher bean
   * @param oldMbiHashEnabled true if old MBI hash should be used
   * @param fissTransformer the fiss transformer
   * @param mcsTransformer the mcs transformer
   * @param claimSourceTypeNames determines the type of claim sources to enable for constructing PAC
   * @param samhsaV2Shadow the samhsa V2 Shadow flag
   * @param securityTagsDao security Tags Dao
   * @param samhsaV2InterceptorShadow the v2SamhsaConsentSimulation resources ({@link
   *     org.hl7.fhir.r4.model.Claim} / {@link org.hl7.fhir.r4.model.ClaimResponse}
   */
  protected AbstractR4ResourceProvider(
      MetricRegistry metricRegistry,
      R4ClaimSamhsaMatcher samhsaMatcher,
      Boolean oldMbiHashEnabled,
      ResourceTransformer<T> fissTransformer,
      ResourceTransformer<T> mcsTransformer,
      String claimSourceTypeNames,
      SamhsaV2InterceptorShadow samhsaV2InterceptorShadow,
      SecurityTagsDao securityTagsDao,
      boolean samhsaV2Shadow) {
    this.metricRegistry = metricRegistry;
    this.samhsaMatcher = samhsaMatcher;
    this.oldMbiHashEnabled = oldMbiHashEnabled;
    this.fissTransformer = requireNonNull(fissTransformer);
    this.mcsTransformer = requireNonNull(mcsTransformer);
    this.samhsaV2InterceptorShadow = samhsaV2InterceptorShadow;
    this.securityTagsDao = securityTagsDao;
    this.samhsaV2Shadow = samhsaV2Shadow;

    requireNonNull(claimSourceTypeNames);
    enabledSourceTypes =
        Stream.of(claimSourceTypeNames.split(","))
            .filter(Strings::isNotBlank)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
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
    claimDao = new ClaimDao(entityManager, metricRegistry, oldMbiHashEnabled, securityTagsDao);

    setResourceType();
  }

  /** {@inheritDoc} */
  @Override
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
  @RetryOnFailoverOrConnectionException
  public T read(@IdParam IdType claimId, RequestDetails requestDetails) {
    if (claimId == null) {
      throw new InvalidRequestException("Resource ID can not be null");
    }
    if (claimId.getVersionIdPartAsLong() != null) {
      throw new InvalidRequestException("Resource ID must not define a version");
    }
    final boolean includeTaxNumbers = returnIncludeTaxNumbers(requestDetails);

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
    ImmutablePair<String, ResourceTypeV2<T, ?>> claimIdObj =
        getClaimIdType(claimIdMatcher, claimId);

    ClaimWithSecurityTags<?> claimEntity;

    try {
      claimEntity = claimDao.getEntityById(claimIdObj.getRight(), claimIdObj.getLeft());
    } catch (NoResultException e) {
      throw new ResourceNotFoundException(claimId);
    }

    Mbi claimEntityMbi = getClaimEntityMbi(claimIdObj.getRight(), claimEntity.getClaimEntity());
    if (claimEntityMbi != null) logMbiIdentifiersToMdc(claimEntityMbi);

    return transformEntity(claimIdObj.getRight(), claimEntity, includeTaxNumbers);
  }

  /**
   * Helper to extract id type and id strFing.
   *
   * @param claimIdMatcher - regex with matching groups
   * @param claimId - the full claim id object with type string 'f' or 'm' as prefix
   * @return the pair of id string and id resource type
   * @throws IllegalStateException if the type string missing
   * @throws ResourceNotFoundException if the type string is not tied to a resource (RDA)
   */
  private ImmutablePair<String, ResourceTypeV2<T, ?>> getClaimIdType(
      Matcher claimIdMatcher, IdType claimId) {
    // group(1) - fiss type 'f' or mcs type 'm'
    // group(2) - fiss claim id or mcs field - alpha numeric expected with possible leading '-' for
    // mcs id field
    String claimIdTypeStr = claimIdMatcher.group(1);
    // robust by check or assert
    if (claimIdTypeStr == null || claimIdTypeStr.isEmpty())
      throw new IllegalStateException(
          "Missing fiss or mcs claim id type info." + claimId.getIdPart());
    Optional<ResourceTypeV2<T, ?>> optional = parseClaimType(claimIdTypeStr);
    if (optional.isEmpty()) throw new ResourceNotFoundException(claimId);
    String claimIdValStr = claimIdMatcher.group(2);
    if (claimIdValStr == null || claimIdValStr.isEmpty())
      throw new IllegalStateException("Missing fiss or mcs claim id value." + claimId.getIdPart());
    return new ImmutablePair<String, ResourceTypeV2<T, ?>>(claimIdValStr, optional.get());
  }

  /**
   * Transforms the pac claim entity to the specified type.
   *
   * @param claimIdType the claim type
   * @param claimEntity the claim entity to transform
   * @param includeTaxNumbers whether to include tax numbers
   * @return the transformed claim
   */
  private T transformEntity(
      ResourceTypeV2<T, ?> claimIdType,
      ClaimWithSecurityTags<?> claimEntity,
      boolean includeTaxNumbers) {

    if (claimIdType.getTypeLabel().equals("fiss")) {
      return fissTransformer.transform(claimEntity, includeTaxNumbers);
    } else if (claimIdType.getTypeLabel().equals("mcs")) {
      return mcsTransformer.transform(claimEntity, includeTaxNumbers);
    } else {
      throw new InvalidRequestException("Invalid claim id type, cannot get claim data");
    }
  }

  /**
   * Returns the {@link Mbi} associated with the given {@link RdaFissClaim} or {@link RdaMcsClaim},
   * depending on resource type.
   *
   * @param <T> generic type extending {@link IBaseResource}
   * @param claimIdType the {@link ResourceTypeV2} indicating what the claim's type is (either fiss
   *     or mcs)
   * @param claimEntity the claim entity itself; either a {@link RdaFissClaim} or {@link
   *     RdaMcsClaim}
   * @return the {@link Mbi} associated with the given claim entity
   * @throws IllegalArgumentException if the given {@link ResourceTypeV2} does not indicate either a
   *     FISS or MCS claim ID type
   */
  private <T extends IBaseResource> @Nullable Mbi getClaimEntityMbi(
      ResourceTypeV2<T, ?> claimIdType, Object claimEntity) {

    return switch (claimIdType.getTypeLabel()) {
      case "fiss" -> ((RdaFissClaim) claimEntity).getMbiRecord();
      case "mcs" -> ((RdaMcsClaim) claimEntity).getMbiRecord();
      default ->
          throw new IllegalArgumentException(
              "Invalid claim ID type '" + claimIdType.getTypeLabel() + "', cannot get claim data");
    };
  }

  /**
   * Logs relevant identifiers (hash, ID) from a given {@link Mbi} to the MDC.
   *
   * @param mbi the {@link Mbi} to log
   */
  private void logMbiIdentifiersToMdc(Mbi mbi) {
    requireNonNull(mbi);

    BfdMDC.put(BfdMDC.MBI_HASH, mbi.getHash());
    BfdMDC.put(BfdMDC.MBI_ID, mbi.getMbiId().toString());
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
   * @param count the count used for result pagination
   * @param hashed a boolean indicating whether the MBI is hashed
   * @param samhsa if {@code true}, exclude all SAMHSA-related resources
   * @param lastUpdated range which to include resources last updated within
   * @param serviceDate range which to include resources completed within
   * @param taxNumbers whether to include tax numbers in the response
   * @param requestDetails the request details
   * @return the bundle
   */
  @Search
  @RetryOnFailoverOrConnectionException
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
      @OptionalParam(name = Constants.PARAM_COUNT)
          @Description(
              shortDefinition = OpenAPIContentProvider.COUNT_SHORT,
              value = OpenAPIContentProvider.COUNT_VALUE)
          String count,
      @OptionalParam(name = "isHashed")
          @Description(
              shortDefinition = OpenAPIContentProvider.PAC_IS_HASHED,
              value = OpenAPIContentProvider.PAC_IS_HASHED_VALUE)
          String hashed,
      @OptionalParam(name = EXCLUDE_SAMHSA)
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
      boolean excludeSamhsa = CommonTransformerUtils.shouldFilterSamhsa(samhsa, requestDetails);
      boolean includeTaxNumbers = returnIncludeTaxNumbers(requestDetails);

      OffsetLinkBuilder paging =
          new OffsetLinkBuilder(
              requestDetails, String.format("/%s?", resourceType.getSimpleName()));

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
    var entitiesWithType =
        resourceTypes.stream()
            // There may be multiple resource types which will each result in a list of claim
            // entities. So, to ensure that we have a flat list of entities to their type, we use
            // flatMap
            .flatMap(
                type ->
                    claimDao
                        .findAllByMbiAttribute(
                            type, mbi, bundleOptions.isHashed, lastUpdated, serviceDate)
                        .stream()
                        .map(e -> new ImmutablePair<>(e, type)))
            .toList();

    // Log nonsensitive MBI identifiers for a given Claim/ClaimResponse request for use in
    // historical analysis
    entitiesWithType.stream()
        .map(pair -> getClaimEntityMbi(pair.right, pair.left.getClaimEntity()))
        .filter(Objects::nonNull)
        // We choose the first MBI from the first, valid claim entity (technically, all entities
        // should fit these criteria or something is very wrong) in the Stream as the MBI
        // will be the same for all returned claims, so there is no reason to evaluate the entire
        // Stream
        .findFirst()
        .ifPresent(this::logMbiIdentifiersToMdc);

    List<T> resources =
        entitiesWithType.stream()
            .filter(
                pair -> {
                  boolean hasNoSamhsaData = samhsaMatcher.hasNoSamhsaData(pair.left);

                  if (samhsaV2Shadow) {
                    // Log if missing claim for samhsa V2 Shadow check before filtering
                    samhsaV2InterceptorShadow.logMissingClaim(pair.left, !hasNoSamhsaData);
                  }

                  return !bundleOptions.excludeSamhsa || hasNoSamhsaData;
                })
            .map(
                pair ->
                    transformEntity(pair.right, pair.getLeft(), bundleOptions.includeTaxNumbers))
            // Enforces a specific sorting for pagination that parities the EOB resource sorting.
            .sorted(Comparator.comparing(r -> r.getIdElement().getIdPart()))
            .collect(Collectors.toList());

    Bundle bundle = new Bundle();
    bundle.setTotal(resources.size());

    if (paging.isPagingRequested()) {
      paging.setTotal(resources.size()).addLinks(bundle);
      // dont sublist unless we have something to paginate, else indexing issues
      if (resources.size() > 0) {
        int endIndex = Math.min(paging.getStartIndex() + paging.getPageSize(), resources.size());
        // Throw a 400 if startIndex >= results, since we cant sublist with these values
        TransformerUtilsV2.validateStartIndexSize(paging.getStartIndex(), resources.size());
        resources = resources.subList(paging.getStartIndex(), endIndex);
      }
    }

    resources.forEach(
        c -> {
          Bundle.BundleEntryComponent entry = bundle.addEntry();
          entry.setResource((Resource) c);
        });

    return bundle;
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
