package gov.cms.bfd.server.war.r4.providers;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.codahale.metrics.MetricRegistry;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ClaimResponse;
import org.hl7.fhir.r4.model.IdType;
import org.springframework.stereotype.Component;

/** This FHIR {@link IResourceProvider} adds support for R4 {@link ClaimResponse} resources. */
@Component
public final class R4ClaimResponseResourceProvider implements IResourceProvider {

  /**
   * A {@link Pattern} that will match the {@link ClaimResponse#getId()}s used in this application,
   * e.g. <code>f-1234</code> or <code>m--1234</code> (for negative IDs).
   */
  private static final Pattern CLAIM_ID_PATTERN = Pattern.compile("([fm])-(-?\\p{Alnum}+)");

  private EntityManager entityManager;
  private MetricRegistry metricRegistry;
  private R4SamhsaMatcher samhsaMatcher;
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

  /** @param samhsaMatcher the {@link R4SamhsaMatcher} to use */
  @Inject
  public void setSamhsaFilterer(R4SamhsaMatcher samhsaMatcher) {
    this.samhsaMatcher = samhsaMatcher;
  }

  /** @param loadedFilterManager the {@link LoadedFilterManager} to use */
  @Inject
  public void setLoadedFilterManager(LoadedFilterManager loadedFilterManager) {
    this.loadedFilterManager = loadedFilterManager;
  }

  /** @see IResourceProvider#getResourceType() */
  @Override
  public Class<? extends IBaseResource> getResourceType() {
    return ClaimResponse.class;
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
  @Read(version = false)
  @Trace
  public ClaimResponse read(@IdParam IdType claimId, RequestDetails requestDetails) {
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
    Optional<PreAdjClaimResponseTypeV2> claimIdType =
        PreAdjClaimResponseTypeV2.parse(claimIdTypeText);
    if (!claimIdType.isPresent()) throw new ResourceNotFoundException(claimId);
    String claimIdString = claimIdMatcher.group(2);

    // TODO: Lookup claim by it's ID from the appropriate table.

    Object claimEntity = 5L;

    return claimIdType.get().getTransformer().transform(metricRegistry, claimEntity);
  }
}
