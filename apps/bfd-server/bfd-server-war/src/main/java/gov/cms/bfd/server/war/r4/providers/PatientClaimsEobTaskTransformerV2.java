package gov.cms.bfd.server.war.r4.providers;

import static gov.cms.bfd.server.war.SpringConfiguration.SSM_PATH_SAMHSA_V2_SHADOW;
import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.rest.param.DateRangeParam;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.server.war.SamhsaV2InterceptorShadow;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.QueryUtils;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.commons.SecurityTagsDao;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Java Callable class that will create a list of patient claims for a given claim type. There are
 * some pertinent info for developers to grasp especially in understanding that each instance of
 * this class will most likely be running concurrently with other claim tasks. This class is a
 * non-singleton spring-managed bean; each task requires its own unique instance of a {@link
 * EntityManager} since processing requires fetching data from a database and requires its own db
 * connection to operate independently of other concurrent claim tasks. All BFD db connections are
 * injected by a Spring-managed {@link com.zaxxer.hikari.HikariDataSource}.
 *
 * <p>To achieve a unique {@link EntityManager}, this class defines its scope as "prototype", which
 * identifies its need have its own unique instance of the {@link EntityManager}.
 *
 * <p>Once Spring has created the class, the caller then sets up the task parameters that define the
 * operating constraints {@link PatientClaimsEobTaskTransformerV2#setupTaskParams}.
 */
@Component
@Scope("prototype")
public class PatientClaimsEobTaskTransformerV2 implements Callable {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(PatientClaimsEobTaskTransformerV2.class);

  // +++++++++++++++++++++++++++++++++++
  // bean injected parameter values
  // +++++++++++++++++++++++++++++++++++

  /** capture performance metrics. */
  private final MetricRegistry metricRegistry;

  /** The samhsa matcher. */
  private final R4EobSamhsaMatcher samhsaMatcher;

  /** v2SamhsaConsentSimulation. */
  private final SamhsaV2InterceptorShadow samhsaV2InterceptorShadow;

  /** Database entity manager. */
  private EntityManager entityManager;

  // +++++++++++++++++++++++++++++++++++
  // setup parameter values
  // +++++++++++++++++++++++++++++++++++

  /** the ClaimTransformer interface for execution. */
  private ClaimTransformerInterfaceV2 claimTransformer = null;

  /** the claim type to retreive data for. */
  private ClaimType claimType;

  /** beneficiary identifier to process. */
  private Long id = 0L;

  /** date range that lastUpdate falls within. */
  private Optional<DateRangeParam> lastUpdated = Optional.empty();

  /** date range that clm_thru_dt falls within. */
  private Optional<DateRangeParam> serviceDate = Optional.empty();

  /** whether to exclude SAMHSA claims. */
  private boolean excludeSamhsa = false;

  // +++++++++++++++++++++++++++++++++++
  // task properties
  // +++++++++++++++++++++++++++++++++++

  /** capture exception if thrown. */
  private final AtomicReference<Exception> taskException = new AtomicReference<>();

  /** keep track of EOBs that were not removed (ignored) by SAMHSA iterations. */
  private final AtomicInteger samhsaIgnoredCount = new AtomicInteger(-1);

  /** keep track of SAMHSA removals. */
  private final AtomicInteger samhsaRemovedCount = new AtomicInteger(0);

  /** the list of EOBs that we'll return. */
  private final List<ExplanationOfBenefit> eobs = new ArrayList<ExplanationOfBenefit>();

  /** Flag to control whether SAMHSA shadow filtering should be applied. */
  private final boolean samhsaV2Shadow;

  private final SecurityTagsDao securityTagsDao;

  /**
   * Constructor for TransformPatientClaimsToEobTask.
   *
   * <p>Spring will wire this class during the initial component scan, so this constructor should
   * only be explicitly called by tests.
   *
   * @param metricRegistry the metric registry bean //* @param loadedFilterManager the loaded filter
   *     manager bean
   * @param samhsaMatcher the samhsa matcher bean
   * @param samhsaV2InterceptorShadow the v2SamhsaConsentSimulation
   * @param securityTagsDao the security Tags Dao
   * @param samhsaV2Shadow the samhsa V2 Shadow flag
   */
  public PatientClaimsEobTaskTransformerV2(
      MetricRegistry metricRegistry,
      R4EobSamhsaMatcher samhsaMatcher,
      SamhsaV2InterceptorShadow samhsaV2InterceptorShadow,
      SecurityTagsDao securityTagsDao,
      @Value("${" + SSM_PATH_SAMHSA_V2_SHADOW + ":false}") Boolean samhsaV2Shadow) {
    this.metricRegistry = requireNonNull(metricRegistry);
    this.samhsaMatcher = requireNonNull(samhsaMatcher);
    this.samhsaV2InterceptorShadow = samhsaV2InterceptorShadow;
    this.securityTagsDao = securityTagsDao;
    this.samhsaV2Shadow = samhsaV2Shadow;
  }

  /**
   * Setup the task processing parameters.
   *
   * @param claimTransformer the {@link ClaimTransformerInterfaceV2} to run.
   * @param claimType the claim type to process.
   * @param id the beneficiary or claim identifier to process.
   * @param lastUpdated the date range that lastUpdate falls within.
   * @param serviceDate the date range that clm_thru_dt falls within.
   * @param excludeSamhsa if true excludes SAMHSA claims.
   */
  public void setupTaskParams(
      ClaimTransformerInterfaceV2 claimTransformer,
      ClaimType claimType,
      Long id,
      Optional<DateRangeParam> lastUpdated,
      Optional<DateRangeParam> serviceDate,
      boolean excludeSamhsa) {
    this.claimTransformer = requireNonNull(claimTransformer);
    this.claimType = requireNonNull(claimType);
    this.id = requireNonNull(id);
    this.lastUpdated = lastUpdated;
    this.serviceDate = serviceDate;
    this.excludeSamhsa = excludeSamhsa;
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
  public PatientClaimsEobTaskTransformerV2 call() {
    LOGGER.debug("TransformPatientClaimsToEobTaskV2.call() started for {}", id);
    try {
      eobs.addAll(transformToEobs(findClaimTypeByPatient()));

      if (excludeSamhsa) {
        filterSamhsa(eobs);
      }
    } catch (NoResultException e) {
      LOGGER.warn(e.getMessage(), e);
      taskException.set(e);
    } catch (Exception e) {
      // keep track of the Exception so we can provide to caller.
      LOGGER.error(e.getMessage(), e);
      taskException.set(e);
    }
    return this;
  }

  /**
   * Transform a list of claims to a list of {@link ExplanationOfBenefit} objects.
   *
   * @param claims the claims/events to transform
   * @param <T> type of claims
   * @return the {@link ExplanationOfBenefit} instances, one per claim/event
   */
  private <T> List<ExplanationOfBenefit> transformToEobs(List<ClaimWithSecurityTags<T>> claims) {
    return claims.stream()
        .map(
            c -> {
              ExplanationOfBenefit eob = transformEobClaim(c);

              if (samhsaV2Shadow) {
                // Log the missing claim along with the EOB
                samhsaV2InterceptorShadow.logMissingClaim(c, samhsaMatcher.test(eob));
              }

              return eob;
            })
        .collect(Collectors.toList());
  }

  /**
   * Transforms the eob claim to the specified type.
   *
   * @param claimEntity the claim entity to transform
   * @return the {@link ExplanationOfBenefit} result
   */
  @VisibleForTesting
  private ExplanationOfBenefit transformEobClaim(ClaimWithSecurityTags<?> claimEntity) {
    return claimTransformer.transform(claimEntity);
  }

  /**
   * Check if the task encountered an {@link Exception}.
   *
   * @return false if the task encountered an exception, true otherwise.
   */
  public boolean ranSuccessfully() {
    return (taskException.get() == null);
  }

  /**
   * Fetch count of EOBs that were not removed by SAMHSA filter.
   *
   * @return number of EOBs that SAMHSA filtering ignored.
   */
  public int eobsIgnoredBySamhsaFilter() {
    return samhsaIgnoredCount.get();
  }

  /**
   * Fetch count of EOBs that were removed by SAMHSA filter.
   *
   * @return number of EOBs removed by SAMHSA filter.
   */
  public int eobsRemovedBySamhsaFilter() {
    return samhsaRemovedCount.get();
  }

  /**
   * true/false if SAMHSA filtering was invoked.
   *
   * @return true if SAMHSA filtering was invoked; false if not invoked.
   */
  public boolean wasSamhsaFilteringPerformed() {
    return (samhsaIgnoredCount.get() >= 0);
  }

  /**
   * Fetch the {@link Exception} if the task encountered one.
   *
   * @return Exception if the task encountered one, Optional.Empty() otherwise.
   */
  public Optional<Exception> getFailure() {
    return ranSuccessfully() ? Optional.empty() : Optional.of(taskException.get());
  }

  /**
   * Fetch the list of {@link ExplanationOfBenefit} that were created.
   *
   * @return {@link List} of {@link ExplanationOfBenefit}
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
  private <T> List<ClaimWithSecurityTags<T>> findClaimTypeByPatient() {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery criteria = builder.createQuery((Class) claimType.getEntityClass());
    Root root = criteria.from(claimType.getEntityClass());
    claimType.getEntityLazyAttributes().stream().forEach(a -> root.fetch(a));
    criteria.select(root).distinct(true);

    Predicate wherePredicate =
        builder.equal(root.get(claimType.getEntityBeneficiaryIdAttribute()), id);

    if (lastUpdated.isPresent()) {
      Predicate predicate = QueryUtils.createLastUpdatedPredicate(builder, root, lastUpdated.get());
      wherePredicate = builder.and(wherePredicate, predicate);
    }
    criteria.where(wherePredicate);

    List<T> claimEntities = null;
    try (Timer.Context timerEobQuery =
        CommonTransformerUtils.createMetricsTimer(
            metricRegistry,
            metricRegistry.getClass().getSimpleName(),
            "query",
            "eobs_by_bene_id",
            claimType.name().toLowerCase())) {
      try {
        claimEntities = entityManager.createQuery(criteria).getResultList();
      } finally {
        long eobsByBeneIdQueryNanoSeconds = timerEobQuery.stop();
        CommonTransformerUtils.recordQueryInMdc(
            String.format("eobs_by_bene_id_%s", claimType.name().toLowerCase()),
            eobsByBeneIdQueryNanoSeconds,
            claimEntities == null ? 0 : claimEntities.size());
      }
    }

    SecurityTagManager securityTagManager = new SecurityTagManager();

    List<ClaimWithSecurityTags<T>> claimEntitiesWithTags = new ArrayList<>();

    if (claimEntities != null && !claimEntities.isEmpty()) {
      Set<String> claimIds = securityTagManager.collectClaimIds((List<Object>) claimEntities);

      if (!claimIds.isEmpty()) {
        Map<String, Set<String>> claimIdToTagsMap =
            securityTagsDao.buildClaimIdToTagsMap(claimType.getEntityTagType(), claimIds);

        claimEntitiesWithTags =
            claimEntities.stream()
                .map(
                    claimEntity -> {
                      String claimId = securityTagManager.extractClaimId(claimEntity);
                      // Ensure claimId is valid (not null or empty) before attempting to fetch tags
                      Set<String> claimSpecificTags =
                          claimIdToTagsMap.getOrDefault(claimId, Collections.emptySet());
                      return new ClaimWithSecurityTags<>(claimEntity, claimSpecificTags);
                    })
                .collect(Collectors.toList());
      }
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
                  TransformerUtilsV2.compareLocalDate(
                      date,
                      lowerBound.atZone(ZoneId.systemDefault()).toLocalDate(),
                      serviceDate.get().getLowerBound().getPrefix());
      final java.util.function.Predicate<LocalDate> upperBoundCheck =
          upperBound == null
              ? (date) -> true
              : (date) ->
                  TransformerUtilsV2.compareLocalDate(
                      date,
                      upperBound.atZone(ZoneId.systemDefault()).toLocalDate(),
                      serviceDate.get().getUpperBound().getPrefix());

      return claimEntitiesWithTags.stream()
          .filter(
              entity ->
                  lowerBoundCheck.test(
                          claimType.getServiceEndAttributeFunction().apply(entity.getClaimEntity()))
                      && upperBoundCheck.test(
                          claimType
                              .getServiceEndAttributeFunction()
                              .apply(entity.getClaimEntity())))
          .collect(Collectors.toList());
    }

    return claimEntitiesWithTags;
  }

  /**
   * Removes all SAMHSA-related claims from the specified {@link List} of {@link
   * ExplanationOfBenefit} resources.
   *
   * @param eobs the {@link List} of {@link ExplanationOfBenefit} resources (i.e. claims) to filter
   */
  private void filterSamhsa(List<ExplanationOfBenefit> eobs) {
    ListIterator<ExplanationOfBenefit> eobsIter = eobs.listIterator();
    // init to zero if doing SAMHSA filtering
    samhsaIgnoredCount.getAndIncrement();
    while (eobsIter.hasNext()) {
      ExplanationOfBenefit eob = eobsIter.next();
      if (samhsaMatcher.test(eob)) {
        eobsIter.remove();
        samhsaRemovedCount.getAndIncrement();
      } else {
        samhsaIgnoredCount.getAndIncrement();
      }
    }
  }
}
