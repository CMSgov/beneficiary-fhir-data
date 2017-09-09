package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.PluralAttribute;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaim;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaim_;
import gov.hhs.cms.bluebutton.data.model.rif.DMEClaim;
import gov.hhs.cms.bluebutton.data.model.rif.DMEClaim_;
import gov.hhs.cms.bluebutton.data.model.rif.HHAClaim;
import gov.hhs.cms.bluebutton.data.model.rif.HHAClaim_;
import gov.hhs.cms.bluebutton.data.model.rif.HospiceClaim;
import gov.hhs.cms.bluebutton.data.model.rif.HospiceClaim_;
import gov.hhs.cms.bluebutton.data.model.rif.InpatientClaim;
import gov.hhs.cms.bluebutton.data.model.rif.InpatientClaim_;
import gov.hhs.cms.bluebutton.data.model.rif.OutpatientClaim;
import gov.hhs.cms.bluebutton.data.model.rif.OutpatientClaim_;
import gov.hhs.cms.bluebutton.data.model.rif.PartDEvent;
import gov.hhs.cms.bluebutton.data.model.rif.PartDEvent_;
import gov.hhs.cms.bluebutton.data.model.rif.SNFClaim;
import gov.hhs.cms.bluebutton.data.model.rif.SNFClaim_;

/**
 * This FHIR {@link IResourceProvider} adds support for STU3
 * {@link ExplanationOfBenefit} resources, derived from the CCW claims.
 */
@Component
public final class ExplanationOfBenefitResourceProvider implements IResourceProvider {
	/**
	 * A {@link Pattern} that will match the
	 * {@link ExplanationOfBenefit#getId()}s used in this application.
	 */
	private static final Pattern EOB_ID_PATTERN = Pattern.compile("(\\p{Alpha}+)-(\\p{Alnum}+)");

	private EntityManager entityManager;
	private ExecutorService databaseQueryExecutorService;

	/**
	 * @param entityManager
	 *            a JPA {@link EntityManager} connected to the application's
	 *            database
	 */
	@PersistenceContext
	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	/**
	 * @param databaseQueryExecutorService
	 *            the {@link ExecutorService} bean that the application should
	 *            run asynchronous database queries on
	 */
	@Inject
	public void setDatabaseQueryExecutorService(ExecutorService databaseQueryExecutorService) {
		this.databaseQueryExecutorService = databaseQueryExecutorService;
	}

	/**
	 * @see ca.uhn.fhir.rest.server.IResourceProvider#getResourceType()
	 */
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return ExplanationOfBenefit.class;
	}

	/**
	 * <p>
	 * Adds support for the FHIR "read" operation, for
	 * {@link ExplanationOfBenefit}s. The {@link Read} annotation indicates that
	 * this method supports the read operation.
	 * </p>
	 * <p>
	 * Read operations take a single parameter annotated with {@link IdParam},
	 * and should return a single resource instance.
	 * </p>
	 * 
	 * @param eobId
	 *            The read operation takes one parameter, which must be of type
	 *            {@link IdType} and must be annotated with the {@link IdParam}
	 *            annotation.
	 * @return Returns a resource matching the specified {@link IdDt}, or
	 *         <code>null</code> if none exists.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Read(version = false)
	public ExplanationOfBenefit read(@IdParam IdType eobId) {
		if (eobId == null)
			throw new IllegalArgumentException();
		if (eobId.getVersionIdPartAsLong() != null)
			throw new IllegalArgumentException();

		String eobIdText = eobId.getIdPart();
		if (eobIdText == null || eobIdText.trim().isEmpty())
			throw new IllegalArgumentException();

		Matcher eobIdMatcher = EOB_ID_PATTERN.matcher(eobIdText);
		if (!eobIdMatcher.matches())
			throw new ResourceNotFoundException(eobId);
		String eobIdTypeText = eobIdMatcher.group(1);
		Optional<ClaimType> eobIdType = ClaimType.parse(eobIdTypeText);
		if (!eobIdType.isPresent())
			throw new ResourceNotFoundException(eobId);
		String eobIdClaimIdText = eobIdMatcher.group(2);

		CriteriaBuilder builder = entityManager.getCriteriaBuilder();

		Class<?> entityClass = eobIdType.get().getEntityClass();
		CriteriaQuery criteria = builder.createQuery(entityClass);
		Root root = criteria.from(entityClass);
		eobIdType.get().getEntityLazyAttributes().stream().forEach(a -> root.fetch(a));
		criteria.select(root);
		criteria.where(builder.equal(root.get(eobIdType.get().getEntityIdAttribute()), eobIdClaimIdText));

		Object claimEntity = null;
		try {
			claimEntity = entityManager.createQuery(criteria).getSingleResult();
		} catch (NoResultException e) {
			throw new ResourceNotFoundException(eobId);
		}

		ExplanationOfBenefit eob = eobIdType.get().getTransformer().apply(claimEntity);
		return eob;
	}

	/**
	 * <p>
	 * Adds support for the FHIR "search" operation for
	 * {@link ExplanationOfBenefit}s, allowing users to search by
	 * {@link ExplanationOfBenefit#getPatient()}.
	 * </p>
	 * <p>
	 * The {@link Search} annotation indicates that this method supports the
	 * search operation. There may be many different methods annotated with this
	 * {@link Search} annotation, to support many different search criteria.
	 * </p>
	 * 
	 * @param patient
	 *            a {@link ReferenceParam} for the
	 *            {@link ExplanationOfBenefit#getPatient()} to try and find
	 *            matches for
	 * @return Returns a {@link List} of {@link ExplanationOfBenefit}s, which
	 *         may contain multiple matching resources, or may also be empty.
	 */
	@Search
	public List<ExplanationOfBenefit> findByPatient(
			@RequiredParam(name = ExplanationOfBenefit.SP_PATIENT) ReferenceParam patient) {
		/*
		 * The way our JPA/SQL schema is setup, we have to run a separate search
		 * for each claim type, then combine the results. It's not super
		 * efficient, but it's also not so inefficient that it's worth fixing.
		 */
		List<ExplanationOfBenefit> eobs = new LinkedList<>();

		CompletableFuture<List<ExplanationOfBenefit>> carrierClaimsFuture = CompletableFuture
				.supplyAsync(() -> findCarrierClaimsByPatient(patient).stream().map(ClaimType.CARRIER.getTransformer())
						.collect(Collectors.toList()), databaseQueryExecutorService);
		CompletableFuture<List<ExplanationOfBenefit>> dmeClaimsFuture = CompletableFuture
				.supplyAsync(() -> findDMEClaimsByPatient(patient).stream().map(ClaimType.DME.getTransformer())
						.collect(Collectors.toList()), databaseQueryExecutorService);
		CompletableFuture<List<ExplanationOfBenefit>> hhaClaimsFuture = CompletableFuture
				.supplyAsync(() -> findHHAClaimsByPatient(patient).stream().map(ClaimType.HHA.getTransformer())
						.collect(Collectors.toList()), databaseQueryExecutorService);
		CompletableFuture<List<ExplanationOfBenefit>> hospiceClaimsFuture = CompletableFuture
				.supplyAsync(() -> findHospiceClaimsByPatient(patient).stream().map(ClaimType.HOSPICE.getTransformer())
						.collect(Collectors.toList()), databaseQueryExecutorService);
		CompletableFuture<List<ExplanationOfBenefit>> inpatientClaimsFuture = CompletableFuture.supplyAsync(
				() -> findInpatientClaimsByPatient(patient).stream().map(ClaimType.INPATIENT.getTransformer())
						.collect(Collectors.toList()),
				databaseQueryExecutorService);
		CompletableFuture<List<ExplanationOfBenefit>> outpatientClaimsFuture = CompletableFuture.supplyAsync(
				() -> findOutpatientClaimsByPatient(patient).stream().map(ClaimType.OUTPATIENT.getTransformer())
						.collect(Collectors.toList()),
				databaseQueryExecutorService);
		CompletableFuture<List<ExplanationOfBenefit>> partDEventsFuture = CompletableFuture
				.supplyAsync(() -> findPartDEventsByPatient(patient).stream().map(ClaimType.PDE.getTransformer())
						.collect(Collectors.toList()), databaseQueryExecutorService);
		CompletableFuture<List<ExplanationOfBenefit>> snfClaimsFuture = CompletableFuture
				.supplyAsync(() -> findSNFClaimsByPatient(patient).stream().map(ClaimType.SNF.getTransformer())
						.collect(Collectors.toList()), databaseQueryExecutorService);

		// Wait for all of the claim query & transform operations to complete.
		CompletableFuture<Void> allClaimsFuture = CompletableFuture.allOf(carrierClaimsFuture, dmeClaimsFuture,
				hhaClaimsFuture, hospiceClaimsFuture, inpatientClaimsFuture, outpatientClaimsFuture, partDEventsFuture,
				snfClaimsFuture);
		allClaimsFuture.join();

		// Add all of the produced claims to the result list.
		try {
			eobs.addAll(carrierClaimsFuture.get());
			eobs.addAll(dmeClaimsFuture.get());
			eobs.addAll(hhaClaimsFuture.get());
			eobs.addAll(hospiceClaimsFuture.get());
			eobs.addAll(inpatientClaimsFuture.get());
			eobs.addAll(outpatientClaimsFuture.get());
			eobs.addAll(partDEventsFuture.get());
			eobs.addAll(snfClaimsFuture.get());
		} catch (InterruptedException e) {
			// FIXME better exception
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			// FIXME better exception
			throw new RuntimeException(e);
		}

		return eobs;
	}

	/**
	 * @param patient
	 *            a {@link ReferenceParam} for the
	 *            {@link ExplanationOfBenefit#getPatient()} to try and find
	 *            matches for
	 * @return the {@link CarrierClaim}s
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Collection<CarrierClaim> findCarrierClaimsByPatient(ReferenceParam patient) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();

		CriteriaQuery<CarrierClaim> criteria = builder.createQuery(CarrierClaim.class);
		Root<CarrierClaim> root = criteria.from(CarrierClaim.class);
		ClaimType.CARRIER.getEntityLazyAttributes().stream().forEach(a -> root.fetch((PluralAttribute) a));
		criteria.select(root);
		criteria.where(builder.equal(root.get(CarrierClaim_.beneficiaryId), patient.getIdPart()));

		List<CarrierClaim> claimEntities = entityManager.createQuery(criteria).getResultList();
		return claimEntities;
	}

	/**
	 * @param patient
	 *            a {@link ReferenceParam} for the
	 *            {@link ExplanationOfBenefit#getPatient()} to try and find
	 *            matches for
	 * @return the {@link DMEClaim}s
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Collection<DMEClaim> findDMEClaimsByPatient(ReferenceParam patient) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();

		CriteriaQuery<DMEClaim> criteria = builder.createQuery(DMEClaim.class);
		Root<DMEClaim> root = criteria.from(DMEClaim.class);
		ClaimType.DME.getEntityLazyAttributes().stream().forEach(a -> root.fetch((PluralAttribute) a));
		criteria.select(root);
		criteria.where(builder.equal(root.get(DMEClaim_.beneficiaryId), patient.getIdPart()));

		List<DMEClaim> claimEntities = entityManager.createQuery(criteria).getResultList();
		return claimEntities;
	}

	/**
	 * @param patient
	 *            a {@link ReferenceParam} for the
	 *            {@link ExplanationOfBenefit#getPatient()} to try and find
	 *            matches for
	 * @return the {@link HHAClaim}s
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Collection<HHAClaim> findHHAClaimsByPatient(ReferenceParam patient) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();

		CriteriaQuery<HHAClaim> criteria = builder.createQuery(HHAClaim.class);
		Root<HHAClaim> root = criteria.from(HHAClaim.class);
		ClaimType.HHA.getEntityLazyAttributes().stream().forEach(a -> root.fetch((PluralAttribute) a));
		criteria.select(root);
		criteria.where(builder.equal(root.get(HHAClaim_.beneficiaryId), patient.getIdPart()));

		List<HHAClaim> claimEntities = entityManager.createQuery(criteria).getResultList();
		return claimEntities;
	}

	/**
	 * @param patient
	 *            a {@link ReferenceParam} for the
	 *            {@link ExplanationOfBenefit#getPatient()} to try and find
	 *            matches for
	 * @return the {@link HospiceClaim}s
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Collection<HospiceClaim> findHospiceClaimsByPatient(ReferenceParam patient) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();

		CriteriaQuery<HospiceClaim> criteria = builder.createQuery(HospiceClaim.class);
		Root<HospiceClaim> root = criteria.from(HospiceClaim.class);
		ClaimType.HOSPICE.getEntityLazyAttributes().stream().forEach(a -> root.fetch((PluralAttribute) a));
		criteria.select(root);
		criteria.where(builder.equal(root.get(HospiceClaim_.beneficiaryId), patient.getIdPart()));

		List<HospiceClaim> claimEntities = entityManager.createQuery(criteria).getResultList();
		return claimEntities;
	}

	/**
	 * @param patient
	 *            a {@link ReferenceParam} for the
	 *            {@link ExplanationOfBenefit#getPatient()} to try and find
	 *            matches for
	 * @return the {@link InpatientClaim}s
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Collection<InpatientClaim> findInpatientClaimsByPatient(ReferenceParam patient) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();

		CriteriaQuery<InpatientClaim> criteria = builder.createQuery(InpatientClaim.class);
		Root<InpatientClaim> root = criteria.from(InpatientClaim.class);
		ClaimType.INPATIENT.getEntityLazyAttributes().stream().forEach(a -> root.fetch((PluralAttribute) a));
		criteria.select(root);
		criteria.where(builder.equal(root.get(InpatientClaim_.beneficiaryId), patient.getIdPart()));

		List<InpatientClaim> claimEntities = entityManager.createQuery(criteria).getResultList();
		return claimEntities;
	}

	/**
	 * @param patient
	 *            a {@link ReferenceParam} for the
	 *            {@link ExplanationOfBenefit#getPatient()} to try and find
	 *            matches for
	 * @return the {@link OutpatientClaim}s
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Collection<OutpatientClaim> findOutpatientClaimsByPatient(ReferenceParam patient) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();

		CriteriaQuery<OutpatientClaim> criteria = builder.createQuery(OutpatientClaim.class);
		Root<OutpatientClaim> root = criteria.from(OutpatientClaim.class);
		ClaimType.OUTPATIENT.getEntityLazyAttributes().stream().forEach(a -> root.fetch((PluralAttribute) a));
		criteria.select(root);
		criteria.where(builder.equal(root.get(OutpatientClaim_.beneficiaryId), patient.getIdPart()));

		List<OutpatientClaim> claimEntities = entityManager.createQuery(criteria).getResultList();
		return claimEntities;
	}

	/**
	 * @param patient
	 *            a {@link ReferenceParam} for the
	 *            {@link ExplanationOfBenefit#getPatient()} to try and find
	 *            matches for
	 * @return the {@link PartDEvent}s
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Collection<PartDEvent> findPartDEventsByPatient(ReferenceParam patient) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();

		CriteriaQuery<PartDEvent> criteria = builder.createQuery(PartDEvent.class);
		Root<PartDEvent> root = criteria.from(PartDEvent.class);
		ClaimType.PDE.getEntityLazyAttributes().stream().forEach(a -> root.fetch((PluralAttribute) a));
		criteria.select(root);
		criteria.where(builder.equal(root.get(PartDEvent_.beneficiaryId), patient.getIdPart()));

		List<PartDEvent> claimEntities = entityManager.createQuery(criteria).getResultList();
		return claimEntities;
	}

	/**
	 * @param patient
	 *            a {@link ReferenceParam} for the
	 *            {@link ExplanationOfBenefit#getPatient()} to try and find
	 *            matches for
	 * @return the {@link SNFClaim}s
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Collection<SNFClaim> findSNFClaimsByPatient(ReferenceParam patient) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();

		CriteriaQuery<SNFClaim> criteria = builder.createQuery(SNFClaim.class);
		Root<SNFClaim> root = criteria.from(SNFClaim.class);
		ClaimType.SNF.getEntityLazyAttributes().stream().forEach(a -> root.fetch((PluralAttribute) a));
		criteria.select(root);
		criteria.where(builder.equal(root.get(SNFClaim_.beneficiaryId), patient.getIdPart()));

		List<SNFClaim> claimEntities = entityManager.createQuery(criteria).getResultList();
		return claimEntities;
	}
}
