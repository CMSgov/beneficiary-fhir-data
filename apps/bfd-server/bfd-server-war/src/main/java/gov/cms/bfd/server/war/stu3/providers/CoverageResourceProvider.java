package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.Beneficiary_;
import gov.cms.bfd.server.war.Operation;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Component;

/**
 * This FHIR {@link IResourceProvider} adds support for STU3 {@link Coverage} resources, derived
 * from the CCW beneficiary enrollment data.
 */
@Component
public final class CoverageResourceProvider implements IResourceProvider {
  /** A {@link Pattern} that will match the {@link Coverage#getId()}s used in this application. */
  private static final Pattern COVERAGE_ID_PATTERN = Pattern.compile("(.*)-(\\p{Alnum}+)");

  private EntityManager entityManager;
  private MetricRegistry metricRegistry;

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

  /** @see ca.uhn.fhir.rest.server.IResourceProvider#getResourceType() */
  @Override
  public Class<? extends IBaseResource> getResourceType() {
    return Coverage.class;
  }

  /**
   * Adds support for the FHIR "read" operation, for {@link Coverage}s. The {@link Read} annotation
   * indicates that this method supports the read operation.
   *
   * <p>Read operations take a single parameter annotated with {@link IdParam}, and should return a
   * single resource instance.
   *
   * @param coverageId The read operation takes one parameter, which must be of type {@link IdType}
   *     and must be annotated with the {@link IdParam} annotation.
   * @return Returns a resource matching the specified {@link IdDt}, or <code>null</code> if none
   *     exists.
   */
  @Read(version = false)
  @Trace
  public Coverage read(@IdParam IdType coverageId) {
    if (coverageId == null) throw new IllegalArgumentException();
    if (coverageId.getVersionIdPartAsLong() != null) throw new IllegalArgumentException();

    String coverageIdText = coverageId.getIdPart();
    if (coverageIdText == null || coverageIdText.trim().isEmpty())
      throw new IllegalArgumentException();

    Operation operation = new Operation(Operation.Endpoint.V1_COVERAGE);
    operation.setOption("by", "id");
    operation.publishOperationName();

    Matcher coverageIdMatcher = COVERAGE_ID_PATTERN.matcher(coverageIdText);
    if (!coverageIdMatcher.matches()) throw new ResourceNotFoundException(coverageId);
    String coverageIdSegmentText = coverageIdMatcher.group(1);
    Optional<MedicareSegment> coverageIdSegment =
        MedicareSegment.selectByUrlPrefix(coverageIdSegmentText);
    if (!coverageIdSegment.isPresent()) throw new ResourceNotFoundException(coverageId);
    String coverageIdBeneficiaryIdText = coverageIdMatcher.group(2);

    Beneficiary beneficiaryEntity;
    try {
      beneficiaryEntity = findBeneficiaryById(coverageIdBeneficiaryIdText);
    } catch (NoResultException e) {
      throw new ResourceNotFoundException(
          new IdDt(Beneficiary.class.getSimpleName(), coverageIdBeneficiaryIdText));
    }

    Coverage coverage =
        CoverageTransformer.transform(metricRegistry, coverageIdSegment.get(), beneficiaryEntity);
    return coverage;
  }

  /**
   * Adds support for the FHIR "search" operation for {@link Coverage}s, allowing users to search by
   * {@link Coverage#getBeneficiary()}.
   *
   * <p>The {@link Search} annotation indicates that this method supports the search operation.
   * There may be many different methods annotated with this {@link Search} annotation, to support
   * many different search criteria.
   *
   * @param beneficiary a {@link ReferenceParam} for the {@link Coverage#getBeneficiary()} to try
   *     and find matches for
   * @param startIndex an {@link OptionalParam} for the startIndex (or offset) used to determine
   *     pagination
   * @param requestDetails a {@link RequestDetails} containing the details of the request URL, used
   *     to parse out pagination values
   * @return Returns a {@link List} of {@link Coverage}s, which may contain multiple matching
   *     resources, or may also be empty.
   */
  @Search
  @Trace
  public Bundle searchByBeneficiary(
      @RequiredParam(name = Coverage.SP_BENEFICIARY) ReferenceParam beneficiary,
      @OptionalParam(name = "startIndex") String startIndex,
      RequestDetails requestDetails) {
    List<IBaseResource> coverages;
    try {
      Beneficiary beneficiaryEntity = findBeneficiaryById(beneficiary.getIdPart());
      coverages = CoverageTransformer.transform(metricRegistry, beneficiaryEntity);
    } catch (NoResultException e) {
      coverages = new LinkedList<IBaseResource>();
    }

    Operation operation = new Operation(Operation.Endpoint.V1_COVERAGE);
    operation.setOption("by", "beneficiary");
    operation.publishOperationName();

    PagingArguments pagingArgs = new PagingArguments(requestDetails);
    Bundle bundle =
        TransformerUtils.createBundle(
            pagingArgs, "/Coverage?", Coverage.SP_BENEFICIARY, beneficiary.getIdPart(), coverages);
    return bundle;
  }

  /**
   * @param beneficiaryId the {@link Beneficiary#getBeneficiaryId()} value to find a matching {@link
   *     Beneficiary} for
   * @return the {@link Beneficiary} that matches the specified {@link
   *     Beneficiary#getBeneficiaryId()} value
   * @throws NoResultException A {@link NoResultException} will be thrown if no matching {@link
   *     Beneficiary} can be found in the database.
   */
  @Trace
  private Beneficiary findBeneficiaryById(String beneficiaryId) throws NoResultException {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Beneficiary> criteria = builder.createQuery(Beneficiary.class);
    Root<Beneficiary> root = criteria.from(Beneficiary.class);
    criteria.select(root);
    criteria.where(builder.equal(root.get(Beneficiary_.beneficiaryId), beneficiaryId));

    Beneficiary beneficiary = null;
    Long beneByIdQueryNanoSeconds = null;
    Timer.Context timerBeneQuery =
        metricRegistry
            .timer(MetricRegistry.name(getClass().getSimpleName(), "query", "bene_by_id"))
            .time();
    try {
      beneficiary = entityManager.createQuery(criteria).getSingleResult();
    } finally {
      beneByIdQueryNanoSeconds = timerBeneQuery.stop();
      TransformerUtils.recordQueryInMdc(
          "bene_by_id.include_", beneByIdQueryNanoSeconds, beneficiary == null ? 0 : 1);
    }

    return beneficiary;
  }
}
