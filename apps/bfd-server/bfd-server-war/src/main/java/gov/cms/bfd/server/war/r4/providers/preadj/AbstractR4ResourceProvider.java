package gov.cms.bfd.server.war.r4.providers.preadj;

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
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.server.war.r4.providers.TransformerUtilsV2;
import gov.cms.bfd.server.war.r4.providers.preadj.common.ClaimDao;
import gov.cms.bfd.server.war.r4.providers.preadj.common.ResourceTypeV2;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
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
import javax.inject.Inject;
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
    implements IResourceProvider {

  /**
   * A {@link Pattern} that will match the {@link ClaimResponse#getId()}s used in this application,
   * e.g. <code>f-1234</code> or <code>m--1234</code> (for negative IDs).
   */
  private static final Pattern CLAIM_ID_PATTERN = Pattern.compile("([fm])-(-?\\p{Alnum}+)");

  private EntityManager entityManager;
  private MetricRegistry metricRegistry;
  private R4ClaimSamhsaMatcher samhsaMatcher;

  private ClaimDao claimDao;

  private Class<T> resourceType;

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

  /** @param samhsaMatcher the {@link R4ClaimSamhsaMatcher} to use */
  @Inject
  public void setSamhsaFilterer(R4ClaimSamhsaMatcher samhsaMatcher) {
    this.samhsaMatcher = samhsaMatcher;
  }

  @PostConstruct
  public void init() {
    claimDao = new ClaimDao(entityManager, metricRegistry);

    setResourceType();
  }

  /** @see IResourceProvider#getResourceType() */
  public Class<T> getResourceType() {
    return resourceType;
  }

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
   * @return Returns a resource matching the specified {@link IdDt}, or <code>null</code> if none
   *     exists.
   */
  @Read
  @Trace
  public T read(@IdParam IdType claimId, RequestDetails requestDetails) {
    if (claimId == null) throw new IllegalArgumentException("Resource ID can not be null");
    if (claimId.getVersionIdPartAsLong() != null)
      throw new IllegalArgumentException("Resource ID must not define a version.");

    String claimIdText = claimId.getIdPart();
    if (claimIdText == null || claimIdText.trim().isEmpty())
      throw new IllegalArgumentException("Resource ID can not be null/blank");

    Matcher claimIdMatcher = CLAIM_ID_PATTERN.matcher(claimIdText);
    if (!claimIdMatcher.matches())
      throw new IllegalArgumentException("Unsupported ID pattern: " + claimIdText);

    String claimIdTypeText = claimIdMatcher.group(1);
    Optional<ResourceTypeV2<T>> optional = parseClaimType(claimIdTypeText);
    if (optional.isEmpty()) throw new ResourceNotFoundException(claimId);
    ResourceTypeV2<T> claimIdType = optional.get();
    String claimIdString = claimIdMatcher.group(2);

    Object claimEntity;

    try {
      claimEntity = claimDao.getEntityById(claimIdType, claimIdString);
    } catch (NoResultException e) {
      throw new ResourceNotFoundException(claimId);
    }

    return claimIdType.getTransformer().transform(metricRegistry, claimEntity);
  }

  /**
   * Implementation specific claim type parsing
   *
   * @param typeText String to parse representing the claim type.
   * @return The parsed {@link ClaimResponseTypeV2} type.
   */
  @VisibleForTesting
  abstract Optional<ResourceTypeV2<T>> parseClaimType(String typeText);

  /**
   * Creates a Set of {@link ResourceTypeV2} for the given claim types.
   *
   * @param types The types of claims to include
   * @return A Set of {@link ResourceTypeV2} claim types.
   */
  @VisibleForTesting
  @Nonnull
  Set<ResourceTypeV2<T>> parseClaimTypes(@Nonnull TokenAndListParam types) {
    return types.getValuesAsQueryTokens().get(0).getValuesAsQueryTokens().stream()
        .map(TokenParam::getValue)
        .map(String::toLowerCase)
        .map(getResourceTypeMap()::get)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  /**
   * Returns a set of all supported resource types.
   *
   * @return Set of all supported resource types.
   */
  @VisibleForTesting
  abstract Set<ResourceTypeV2<T>> getResourceTypes();

  /**
   * Returns implementation specific {@link ResourceTypeV2} map.
   *
   * @return The implementation specific {@link ResourceTypeV2} map.
   */
  @VisibleForTesting
  abstract Map<String, ResourceTypeV2<T>> getResourceTypeMap();

  @Search
  @Trace
  public Bundle findByPatient(
      @RequiredParam(name = "mbi")
          @Description(shortDefinition = "The patient identifier to search for")
          ReferenceParam mbi,
      @OptionalParam(name = "type")
          @Description(shortDefinition = "A list of claim types to include")
          TokenAndListParam types,
      @OptionalParam(name = "isHashed")
          @Description(shortDefinition = "A boolean indicating whether or not the MBI is hashed")
          String hashed,
      @OptionalParam(name = "excludeSAMHSA")
          @Description(shortDefinition = "If true, exclude all SAMHSA-related resources")
          String samhsa,
      @OptionalParam(name = "_lastUpdated")
          @Description(shortDefinition = "Include resources last updated in the given range")
          DateRangeParam lastUpdated,
      @OptionalParam(name = "service-date")
          @Description(shortDefinition = "Include resources that completed in the given range")
          DateRangeParam serviceDate,
      RequestDetails requestDetails) {
    if (mbi != null && !StringUtils.isBlank(mbi.getIdPart())) {
      String mbiString = mbi.getIdPart();

      Bundle bundleResource;

      boolean isHashed = !Boolean.FALSE.toString().equalsIgnoreCase(hashed);
      boolean excludeSamhsa = Boolean.TRUE.toString().equalsIgnoreCase(samhsa);

      if (isHashed) {
        TransformerUtilsV2.logMbiHashToMdc(mbiString);
      }

      if (types != null) {
        bundleResource =
            createBundleFor(
                parseClaimTypes(types),
                mbiString,
                isHashed,
                excludeSamhsa,
                lastUpdated,
                serviceDate);
      } else {
        bundleResource =
            createBundleFor(
                getResourceTypes(), mbiString, isHashed, excludeSamhsa, lastUpdated, serviceDate);
      }

      return bundleResource;
    } else {
      throw new IllegalArgumentException("mbi can't be null/blank");
    }
  }

  /**
   * Creates a Bundle of resources for the given data using the given {@link ResourceTypeV2}.
   *
   * @param resourceTypes The {@link ResourceTypeV2} data to retrieve.
   * @param mbi The mbi to look up associated data for.
   * @param isHashed Denotes if the given mbi is hashed.
   * @param lastUpdated Date range of desired lastUpdate values to retrieve data for.
   * @param serviceDate Date range of the desired service date to retrieve data for.
   * @return A Bundle with data found using the provided parameters.
   */
  @VisibleForTesting
  Bundle createBundleFor(
      Set<ResourceTypeV2<T>> resourceTypes,
      String mbi,
      boolean isHashed,
      boolean excludeSamhsa,
      DateRangeParam lastUpdated,
      DateRangeParam serviceDate) {
    List<T> resources = new ArrayList<>();

    for (ResourceTypeV2<T> type : resourceTypes) {
      List<?> entities;

      entities =
          claimDao.findAllByMbiAttribute(
              type.getEntityClass(),
              type.getEntityMbiRecordAttribute(),
              mbi,
              isHashed,
              lastUpdated,
              serviceDate,
              type.getEntityEndDateAttribute());

      resources.addAll(
          entities.stream()
              .filter(e -> !excludeSamhsa || hasNoSamhsaData(metricRegistry, e))
              .map(e -> type.getTransformer().transform(metricRegistry, e))
              .collect(Collectors.toList()));
    }

    Bundle bundle = new Bundle();

    resources.forEach(
        c -> {
          Bundle.BundleEntryComponent entry = bundle.addEntry();
          entry.setResource((Resource) c);
        });

    return bundle;
  }

  @VisibleForTesting
  boolean hasNoSamhsaData(MetricRegistry metricRegistry, Object entity) {
    Claim claim;

    if (entity instanceof PreAdjFissClaim) {
      claim = FissClaimTransformerV2.transform(metricRegistry, entity);
    } else if (entity instanceof PreAdjMcsClaim) {
      claim = McsClaimTransformerV2.transform(metricRegistry, entity);
    } else {
      throw new IllegalArgumentException(
          "Unsupported entity " + entity.getClass().getCanonicalName() + " for samhsa filtering");
    }

    return !samhsaMatcher.test(claim);
  }
}
