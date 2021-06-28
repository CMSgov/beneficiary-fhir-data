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
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.server.war.r4.providers.preadj.common.ClaimDao;
import gov.cms.bfd.server.war.r4.providers.preadj.common.ResourceTypeV2;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import org.apache.commons.lang3.StringUtils;
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
    implements IResourceProvider {

  /**
   * A {@link Pattern} that will match the {@link ClaimResponse#getId()}s used in this application,
   * e.g. <code>f-1234</code> or <code>m--1234</code> (for negative IDs).
   */
  // TODO: [DCGEO-98] Update to include support for 'm' (MCS) prefix
  private static final Pattern CLAIM_ID_PATTERN = Pattern.compile("([f])-(-?\\p{Alnum}+)");

  private EntityManager entityManager;
  private MetricRegistry metricRegistry;

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
    if (!optional.isPresent()) throw new ResourceNotFoundException(claimId);
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
   * Helper method to make mocking easier in tests.
   *
   * @param typeText String to parse representing the claim type.
   * @return The parsed {@link ClaimResponseTypeV2} type.
   */
  @VisibleForTesting
  abstract Optional<ResourceTypeV2<T>> parseClaimType(String typeText);

  /**
   * Returns a set of all supported resource types.
   *
   * @return Set of all supported resource types.
   */
  @VisibleForTesting
  abstract Set<ResourceTypeV2<T>> getResourceTypes();

  @Search
  @Trace
  public Bundle findByPatient(
      @RequiredParam(name = "mbi")
          @Description(shortDefinition = "The patient identifier to search for")
          ReferenceParam mbi,
      @OptionalParam(name = "type")
          @Description(shortDefinition = "The source type to use for fetching claim data")
          String type,
      @OptionalParam(name = "isHashed")
          @Description(shortDefinition = "A boolean indicating whether or not the MBI is hashed")
          String hashed,
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

      Optional<ResourceTypeV2<T>> optional = parseClaimType(type);
      if (optional.isPresent()) {
        bundleResource =
            createBundleFor(
                Collections.singleton(optional.get()), mbiString, isHashed, lastUpdated);
      } else {
        bundleResource = createBundleFor(getResourceTypes(), mbiString, isHashed, lastUpdated);
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
   * @return A Bundle with data found using the provided parameters.
   */
  @VisibleForTesting
  Bundle createBundleFor(
      Set<ResourceTypeV2<T>> resourceTypes,
      String mbi,
      boolean isHashed,
      DateRangeParam lastUpdated) {
    List<T> resources = new ArrayList<>();

    for (ResourceTypeV2<T> type : resourceTypes) {
      List<?> entities;

      // TODO: [DCGEO-117] Implement additional search by service date range
      if (isHashed) {
        entities = claimDao.findAllByMbiHash(type.getEntityClass(), mbi, lastUpdated);
      } else {
        entities = claimDao.findAllByMbi(type.getEntityClass(), mbi, lastUpdated);
      }

      resources.addAll(
          entities.stream()
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
}
