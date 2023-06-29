package gov.cms.bfd.server.war.stu3.providers;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.rest.param.DateRangeParam;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.data.fda.lookup.FdaDrugCodeDisplayLookup;
import gov.cms.bfd.data.npi.lookup.NPIOrgLookup;
import gov.cms.bfd.server.war.commons.QueryUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Java Callable class that will create a list of patient claims for a given claim type. */
@Component
@Scope("prototype")
public class PatientClaimsEobTaskTransformer implements Callable {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(PatientClaimsEobTaskTransformer.class);

  // +++++++++++++++++++++++++++++++++++
  // bean injected parameter values
  // +++++++++++++++++++++++++++++++++++

  /** capture performance metrics. */
  @Autowired private MetricRegistry metricRegistry;
  /** drug description lookup table. */
  @Autowired private final FdaDrugCodeDisplayLookup drugCodeDisplayLookup;
  /** NPI lookup table. */
  @Autowired private final NPIOrgLookup npiOrgLookup;
  /** The samhsa matcher. */
  @Autowired private final Stu3EobSamhsaMatcher samhsaMatcher;
  /** Database Entity manager. */
  private EntityManager entityManager = null;

  // +++++++++++++++++++++++++++++++++++
  // setup parameter values
  // +++++++++++++++++++++++++++++++++++

  /** the ClaimTransformer interface for execution. */
  private ClaimTransformerInterface claimTransformer;
  /** the claim type to retreive data for. */
  private ClaimType claimType;
  /** beneficiary identifier to process. */
  private Long id;
  /** the id represents a single patient read. */
  private boolean singlePatientRead;
  /** date range that lastUpdate falls within. */
  private Optional<DateRangeParam> lastUpdated;
  /** date range that clm_thru_dt falls within. */
  private Optional<DateRangeParam> serviceDate;
  /** whether to return tax numbers. */
  private Optional<Boolean> includeTaxNumbers;
  /** whether to exclude SAMHSA claims. */
  private Boolean excludeSamhsa;

  // +++++++++++++++++++++++++++++++++++
  // task properties
  // +++++++++++++++++++++++++++++++++++

  /** capture exception if thrown. */
  private Exception taskException = null;
  /** the list of EOBs that we'll return. */
  private List<ExplanationOfBenefit> eobs = new ArrayList<ExplanationOfBenefit>();

  /**
   * Constructor for TransformPatientClaimsToEobTask.
   *
   * <p>Spring will wire this class during the initial component scan, so this constructor should
   * only be explicitly called by tests.
   *
   * @param metricRegistry the metric registry bean
   * @param samhsaMatcher the samhsa matcher bean
   * @param drugCodeDisplayLookup the drug code display lookup bean
   * @param npiOrgLookup the npi org lookup bean
   */
  public PatientClaimsEobTaskTransformer(
      MetricRegistry metricRegistry,
      Stu3EobSamhsaMatcher samhsaMatcher,
      FdaDrugCodeDisplayLookup drugCodeDisplayLookup,
      NPIOrgLookup npiOrgLookup) {
    this.metricRegistry = requireNonNull(metricRegistry);
    this.samhsaMatcher = requireNonNull(samhsaMatcher);
    this.drugCodeDisplayLookup = requireNonNull(drugCodeDisplayLookup);
    this.npiOrgLookup = requireNonNull(npiOrgLookup);
  }

  /**
   * Setup the task processing parameters.
   *
   * @param claimTransformer the {@link ClaimTransformerInterface} to run.
   * @param claimType the claim type to process.
   * @param id the beneficiary or claim identifier to process.
   * @param lastUpdated the date range that lastUpdate falls within.
   * @param serviceDate the date range that clm_thru_dt falls within.
   * @param includeTaxNumbers whether to return tax numbers.
   * @param excludeSamhsa if true excludes SAMHSA claims.
   */
  public void setupTaskParams(
      ClaimTransformerInterface claimTransformer,
      ClaimType claimType,
      Long id,
      Optional<DateRangeParam> lastUpdated,
      Optional<DateRangeParam> serviceDate,
      Optional<Boolean> includeTaxNumbers,
      Boolean excludeSamhsa) {
    this.claimTransformer = requireNonNull(claimTransformer);
    this.claimType = requireNonNull(claimType);
    this.id = requireNonNull(id);
    this.lastUpdated = lastUpdated;
    this.serviceDate = serviceDate;
    this.excludeSamhsa = excludeSamhsa;
    this.includeTaxNumbers = includeTaxNumbers;
  }

  /**
   * Sets the {@link #entityManager}; defined as scope=prototype to get unique JPA {@link
   * EntityManager} per thread.
   *
   * @param entityManager a JPA {@link EntityManager} connected to the application's database
   */
  @PersistenceContext
  @Scope("prototype")
  public void setEntityManager(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /**
   * ExecutorService will invoke the task.
   *
   * @return the results for the task.
   */
  @Override
  public PatientClaimsEobTaskTransformer call() {
    LOGGER.debug("TransformPatientClaimsToEobTask.call() started for {}", id);
    try {
      eobs.addAll(transformToEobs(findClaimTypeByPatient()));
      if (excludeSamhsa) {
        filterSamhsa(eobs);
      }
    } catch (NoResultException e) {
      LOGGER.error((taskException = e).getMessage(), e);
    } catch (Exception e) {
      // keep track of the Exception so we can provide to caller.
      LOGGER.error((taskException = e).getMessage(), e);
    }
    return this;
  }

  /**
   * Transform a list of claims to a list of {@link org.hl7.fhir.r4.model.ExplanationOfBenefit}
   * objects.
   *
   * @param claims the claims/events to transform
   * @return the {@link ExplanationOfBenefit} instances, one per claim/event
   */
  @Trace
  private List<ExplanationOfBenefit> transformToEobs(List<?> claims) {
    return claims.stream().map(c -> transformEobClaim(c)).collect(Collectors.toList());
  }

  /**
   * Transforms the eob claim to the specified type.
   *
   * @param claimEntity the claim entity to transform
   * @return the transformed explanation of benefit
   */
  private ExplanationOfBenefit transformEobClaim(Object claimEntity) {
    return claimTransformer.transform(claimEntity, includeTaxNumbers);
  }

  /**
   * Check if the task encountered an {@link Exception}.
   *
   * @return false if the task encountered an exception, true otherwise.
   */
  public boolean ranSuccessfully() {
    return (taskException == null);
  }

  /**
   * Fetch the {@link Exception} if the task encountered one.
   *
   * @return Exception if the task encountered one, Optional.Empty() otherwise.
   */
  public Optional<Exception> getFailure() {
    return ranSuccessfully() ? Optional.empty() : Optional.of(taskException);
  }

  /**
   * Fetch the list of {@link IBaseResource} that were created.
   *
   * @return {@link List} of {@link IBaseResource}
   */
  public List<ExplanationOfBenefit> fetchEOBs() {
    return eobs;
  }

  /**
   * Fetch the claim type string.
   *
   * @return {@link String} identifying the {@link ClaimType} of task
   */
  @Override
  public String toString() {
    return claimType.toString();
  }

  /**
   * Find claim type by patient list.
   *
   * @param <T> the type parameter
   * @return the matching claim/event entities
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  @Trace
  private <T> List<T> findClaimTypeByPatient() {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery criteria = builder.createQuery((Class) claimType.getEntityClass());
    Root root = criteria.from(claimType.getEntityClass());
    claimType.getEntityLazyAttributes().stream().forEach(a -> root.fetch(a));
    criteria.select(root).distinct(true);

    // Search for a beneficiary's records. Use lastUpdated if present
    Predicate wherePredicate =
        builder.equal(root.get(claimType.getEntityBeneficiaryIdAttribute()), id);
    if (lastUpdated.isPresent()) {
      Predicate predicate = QueryUtils.createLastUpdatedPredicate(builder, root, lastUpdated.get());
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
          String.format("eobs_by_bene_id_%s", claimType.name().toLowerCase()),
          eobsByBeneIdQueryNanoSeconds,
          claimEntities == null ? 0 : claimEntities.size());
    }

    if (claimEntities != null && !serviceDate.isEmpty()) {
      final Instant lowerBound =
          serviceDate.get().getLowerBoundAsInstant() != null
              ? serviceDate.get().getLowerBoundAsInstant().toInstant()
              : null;
      final Instant upperBound =
          serviceDate.get().getUpperBoundAsInstant() != null
              ? serviceDate.get().getUpperBoundAsInstant().toInstant()
              : null;
      final java.util.function.Predicate<LocalDate> lowerBoundCheck =
          lowerBound == null
              ? (date) -> true
              : (date) ->
                  TransformerUtils.compareLocalDate(
                      date,
                      lowerBound.atZone(ZoneId.systemDefault()).toLocalDate(),
                      serviceDate.get().getLowerBound().getPrefix());
      final java.util.function.Predicate<LocalDate> upperBoundCheck =
          upperBound == null
              ? (date) -> true
              : (date) ->
                  TransformerUtils.compareLocalDate(
                      date,
                      upperBound.atZone(ZoneId.systemDefault()).toLocalDate(),
                      serviceDate.get().getUpperBound().getPrefix());

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
   * Removes all SAMHSA-related claims from the specified {@link List} of {@link
   * ExplanationOfBenefit} resources.
   *
   * @param eobs the {@link List} of {@link IBaseResource} resources (i.e. ExplanationOfBenefit) to
   *     filter
   */
  private void filterSamhsa(List<ExplanationOfBenefit> eobs) {
    ListIterator<ExplanationOfBenefit> eobsIter = eobs.listIterator();
    while (eobsIter.hasNext()) {
      ExplanationOfBenefit eob = (ExplanationOfBenefit) eobsIter.next();
      if (samhsaMatcher.test(eob)) {
        eobsIter.remove();
      }
    }
  }
}
