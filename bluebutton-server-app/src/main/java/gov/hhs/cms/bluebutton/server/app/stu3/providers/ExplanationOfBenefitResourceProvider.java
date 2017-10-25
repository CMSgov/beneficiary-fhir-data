package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.PluralAttribute;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.DateRangeParam;
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
	private static final Logger LOGGER = LoggerFactory.getLogger(ExplanationOfBenefitResourceProvider.class);

	private EntityManager entityManager;

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
	 *            matches for {@link ExplanationOfBenefit}s
	 * @param dateRangeParam
	 *            a {@link DateRangeParam} can be used to search for
	 *            {@link ExplanationOfBenefit}s by these dates
	 * @return Returns a {@link List} of {@link ExplanationOfBenefit}s, which
	 *         may contain multiple matching resources, or may also be empty.
	 */
	@Search
	public List<ExplanationOfBenefit> findByPatient(
			@RequiredParam(name = ExplanationOfBenefit.SP_PATIENT) ReferenceParam patient,
			@OptionalParam(name = "billablePeriodDate") DateRangeParam dateRangeParam) {
		/*
		 * The way our JPA/SQL schema is setup, we have to run a separate search
		 * for each claim type, then combine the results. It's not super
		 * efficient, but it's also not so inefficient that it's worth fixing.
		 */
		List<ExplanationOfBenefit> eobs = new LinkedList<>();

		Optional<DateRangeParam> dateRange = null;
		if (dateRangeParam == null)
			dateRange = Optional.empty();
		else
			dateRange = Optional.of(dateRangeParam);

		eobs.addAll(findCarrierClaimsByPatient(patient, dateRange).stream().map(ClaimType.CARRIER.getTransformer())
				.collect(Collectors.toList()));
		eobs.addAll(findDMEClaimsByPatient(patient, dateRange).stream().map(ClaimType.DME.getTransformer())
				.collect(Collectors.toList()));
		eobs.addAll(findHHAClaimsByPatient(patient, dateRange).stream().map(ClaimType.HHA.getTransformer())
				.collect(Collectors.toList()));
		eobs.addAll(findHospiceClaimsByPatient(patient, dateRange).stream().map(ClaimType.HOSPICE.getTransformer())
				.collect(Collectors.toList()));
		eobs.addAll(findInpatientClaimsByPatient(patient, dateRange).stream().map(ClaimType.INPATIENT.getTransformer())
				.collect(Collectors.toList()));
		eobs.addAll(findOutpatientClaimsByPatient(patient, dateRange).stream()
				.map(ClaimType.OUTPATIENT.getTransformer()).collect(Collectors.toList()));
		eobs.addAll(findPartDEventsByPatient(patient, dateRange).stream().map(ClaimType.PDE.getTransformer())
				.collect(Collectors.toList()));
		eobs.addAll(findSNFClaimsByPatient(patient, dateRange).stream().map(ClaimType.SNF.getTransformer())
				.collect(Collectors.toList()));
		 

		return eobs;
	}


	/**
	 * @param patient
	 *            a {@link ReferenceParam} for the
	 *            {@link ExplanationOfBenefit#getPatient()} to try and find
	 *            matches for {@link CarrierClaim}s
	 * @param dateRange
	 *            a {@link DateRangeParam} can be used to search for
	 *            {@link CarrierClaim}s by these dates
	 * @return the {@link CarrierClaim}s
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Collection<CarrierClaim> findCarrierClaimsByPatient(ReferenceParam patient,
			Optional<DateRangeParam> dateRange) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();

		CriteriaQuery<CarrierClaim> criteria = builder.createQuery(CarrierClaim.class);
		Root<CarrierClaim> root = criteria.from(CarrierClaim.class);
		ClaimType.CARRIER.getEntityLazyAttributes().stream().forEach(a -> root.fetch((PluralAttribute) a));
		criteria.select(root);

		CriteriaQuery criteriaQuery = createSearchCriteria(criteria, patient, root, builder,
				root.get(CarrierClaim_.beneficiaryId), root.get(CarrierClaim_.dateFrom), dateRange);

		List<CarrierClaim> claimEntities = entityManager.createQuery(criteriaQuery).getResultList();
		LOGGER.info("deh-info in CarrierClaims-claimEntities size is " + claimEntities.size());
		return claimEntities;
	}



	/**
	 * @param patient
	 *            a {@link ReferenceParam} for the
	 *            {@link ExplanationOfBenefit#getPatient()} to try and find
	 *            matches for {@link DMEClaim}s
	 * @param dateRange
	 *            a {@link DateRangeParam} can be used to search for
	 *            {@link DMEClaim}s by these dates
	 * @return the {@link DMEClaim}s
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Collection<DMEClaim> findDMEClaimsByPatient(ReferenceParam patient, Optional<DateRangeParam> dateRange) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();

		CriteriaQuery<DMEClaim> criteria = builder.createQuery(DMEClaim.class);
		Root<DMEClaim> root = criteria.from(DMEClaim.class);
		ClaimType.DME.getEntityLazyAttributes().stream().forEach(a -> root.fetch((PluralAttribute) a));
		criteria.select(root);

		CriteriaQuery criteriaQuery = createSearchCriteria(criteria, patient, root, builder,
				root.get(DMEClaim_.beneficiaryId), root.get(DMEClaim_.dateFrom), dateRange);

		List<DMEClaim> claimEntities = entityManager.createQuery(criteriaQuery).getResultList();
		LOGGER.info("deh-info in DMEClaims-claimEntities size is " + claimEntities.size());
		return claimEntities;
	}

	/**
	 * @param patient
	 *            a {@link ReferenceParam} for the
	 *            {@link ExplanationOfBenefit#getPatient()} to try and find
	 *            matches for {@link HHAClaim}s
	 * @param dateRange
	 *            a {@link DateRangeParam} can be used to search for
	 *            {@link HHAClaim}s by these dates
	 * @return the {@link HHAClaim}s
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Collection<HHAClaim> findHHAClaimsByPatient(ReferenceParam patient, Optional<DateRangeParam> dateRange) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();

		CriteriaQuery<HHAClaim> criteria = builder.createQuery(HHAClaim.class);
		Root<HHAClaim> root = criteria.from(HHAClaim.class);
		ClaimType.HHA.getEntityLazyAttributes().stream().forEach(a -> root.fetch((PluralAttribute) a));
		criteria.select(root);

		CriteriaQuery criteriaQuery = createSearchCriteria(criteria, patient, root, builder,
				root.get(HHAClaim_.beneficiaryId), root.get(HHAClaim_.dateFrom), dateRange);

		List<HHAClaim> claimEntities = entityManager.createQuery(criteriaQuery).getResultList();
		LOGGER.info("deh-info in HHAClaims-claimEntities size is " + claimEntities.size());
		return claimEntities;
	}

	/**
	 * @param patient
	 *            a {@link ReferenceParam} for the
	 *            {@link ExplanationOfBenefit#getPatient()} to try and find
	 *            matches for {@link HospiceClaim}s
	 * @param dateRange
	 *            a {@link DateRangeParam} can be used to search for
	 *            {@link HospiceClaim}s by these dates
	 * @return the {@link HospiceClaim}s
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Collection<HospiceClaim> findHospiceClaimsByPatient(ReferenceParam patient,
			Optional<DateRangeParam> dateRange) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();

		CriteriaQuery<HospiceClaim> criteria = builder.createQuery(HospiceClaim.class);
		Root<HospiceClaim> root = criteria.from(HospiceClaim.class);
		ClaimType.HOSPICE.getEntityLazyAttributes().stream().forEach(a -> root.fetch((PluralAttribute) a));
		criteria.select(root);

		CriteriaQuery criteriaQuery = createSearchCriteria(criteria, patient, root, builder,
				root.get(HospiceClaim_.beneficiaryId), root.get(HospiceClaim_.dateFrom), dateRange);

		List<HospiceClaim> claimEntities = entityManager.createQuery(criteriaQuery).getResultList();
		LOGGER.info("deh-info in HospiceClaims-claimEntities size is " + claimEntities.size());
		return claimEntities;
	}

	/**
	 * @param patient
	 *            a {@link ReferenceParam} for the
	 *            {@link ExplanationOfBenefit#getPatient()} to try and find
	 *            matches for {@link InpatientClaim}s
	 * @param dateRange
	 *            a {@link DateRangeParam} can be used to search for
	 *            {@link InpatientClaim}s by these dates
	 * @return the {@link InpatientClaim}s
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Collection<InpatientClaim> findInpatientClaimsByPatient(ReferenceParam patient,
			Optional<DateRangeParam> dateRange) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();

		CriteriaQuery<InpatientClaim> criteria = builder.createQuery(InpatientClaim.class);
		Root<InpatientClaim> root = criteria.from(InpatientClaim.class);
		ClaimType.INPATIENT.getEntityLazyAttributes().stream().forEach(a -> root.fetch((PluralAttribute) a));
		criteria.select(root);

		CriteriaQuery criteriaQuery = createSearchCriteria(criteria, patient, root, builder,
				root.get(InpatientClaim_.beneficiaryId), root.get(InpatientClaim_.dateFrom), dateRange);

		List<InpatientClaim> claimEntities = entityManager.createQuery(criteriaQuery).getResultList();
		LOGGER.info("deh-info in InpatientClaims-claimEntities size is " + claimEntities.size());

		return claimEntities;
	}

	/**
	 * @param patient
	 *            a {@link ReferenceParam} for the
	 *            {@link ExplanationOfBenefit#getPatient()} to try and find
	 *            matches for {@link OutpatientClaim}s
	 * @param dateRange
	 *            a {@link DateRangeParam} can be used to search for
	 *            {@link OutpatientClaim}s by these dates
	 * @return the {@link OutpatientClaim}s
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Collection<OutpatientClaim> findOutpatientClaimsByPatient(ReferenceParam patient,
			Optional<DateRangeParam> dateRange) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();

		CriteriaQuery<OutpatientClaim> criteria = builder.createQuery(OutpatientClaim.class);
		Root<OutpatientClaim> root = criteria.from(OutpatientClaim.class);
		ClaimType.OUTPATIENT.getEntityLazyAttributes().stream().forEach(a -> root.fetch((PluralAttribute) a));
		criteria.select(root);

		CriteriaQuery criteriaQuery = createSearchCriteria(criteria, patient, root, builder,
				root.get(OutpatientClaim_.beneficiaryId), root.get(OutpatientClaim_.dateFrom), dateRange);


		List<OutpatientClaim> claimEntities = entityManager.createQuery(criteriaQuery).getResultList();
		LOGGER.info("deh-info in OutpatientClaims-claimEntities size is " + claimEntities.size());
		return claimEntities;
	}

	/**
	 * @param patient
	 *            a {@link ReferenceParam} for the
	 *            {@link ExplanationOfBenefit#getPatient()} to try and find
	 *            matches for {@link PartDEvent}s
	 * @param dateRange
	 *            a {@link DateRangeParam} can be used to search for
	 *            {@link PartDEvent}s by these dates
	 * @return the {@link PartDEvent}s
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Collection<PartDEvent> findPartDEventsByPatient(ReferenceParam patient,
			Optional<DateRangeParam> dateRange) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();

		CriteriaQuery<PartDEvent> criteria = builder.createQuery(PartDEvent.class);
		Root<PartDEvent> root = criteria.from(PartDEvent.class);
		ClaimType.PDE.getEntityLazyAttributes().stream().forEach(a -> root.fetch((PluralAttribute) a));
		criteria.select(root);

		CriteriaQuery criteriaQuery = createSearchCriteria(criteria, patient, root, builder,
				root.get(PartDEvent_.beneficiaryId), root.get(PartDEvent_.prescriptionFillDate), dateRange);

		List<PartDEvent> claimEntities = entityManager.createQuery(criteriaQuery).getResultList();
		LOGGER.info("deh-info in PartD-claimEntities size is " + claimEntities.size());

		return claimEntities;
	}

	/**
	 * @param patient
	 *            a {@link ReferenceParam} for the
	 *            {@link ExplanationOfBenefit#getPatient()} to try and find
	 *            matches for {@link SNFClaim}s
	 * @param dateRange
	 *            a {@link DateRangeParam} can be used to search for
	 *            {@link SNFClaim}s by these dates
	 * @return the {@link SNFClaim}s
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Collection<SNFClaim> findSNFClaimsByPatient(ReferenceParam patient, Optional<DateRangeParam> dateRange) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();

		CriteriaQuery<SNFClaim> criteria = builder.createQuery(SNFClaim.class);
		Root<SNFClaim> root = criteria.from(SNFClaim.class);
		ClaimType.SNF.getEntityLazyAttributes().stream().forEach(a -> root.fetch((PluralAttribute) a));
		criteria.select(root);

		CriteriaQuery criteriaQuery = createSearchCriteria(criteria, patient, root, builder,
				root.get(SNFClaim_.beneficiaryId), root.get(SNFClaim_.dateFrom), dateRange);


		List<SNFClaim> claimEntities = entityManager.createQuery(criteriaQuery).getResultList();
		LOGGER.info("deh-info in SNFClaims-claimEntities size is " + claimEntities.size());

		return claimEntities;
	}

	/**
	 * @param criteria
	 *            {@link CriteriaQuery} for the search
	 * @param patient
	 *            a {@link ReferenceParam} for the
	 *            {@link ExplanationOfBenefit#getPatient()} to try and find
	 *            matches for {@link PartDEvent}s
	 * @param root
	 *            {@link Root} for the search
	 * @param builder
	 *            {@link CriteriaBuilder} for the search
	 * @param beneficiaryIdPath
	 *            {@link Path} for the beneficiaryId
	 * @param dateFromPath
	 *            {@link Path} for the service from date (fill date for PartD)
	 * @param dateRange
	 *            a {@link DateRangeParam} can be used to search for
	 *            {@link ExplanationOfBenefit}s by these dates
	 * @return the {@link CriteriaQuery}
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private CriteriaQuery createSearchCriteria(CriteriaQuery criteria, ReferenceParam patient, Root root,
			CriteriaBuilder builder, Path beneficiaryIdPath, Path dateFromPath, Optional<DateRangeParam> dateRange) {
		
		// no search dates were supplied in the url
		if (!dateRange.isPresent()) {
			criteria.where(builder.equal(beneficiaryIdPath, patient.getIdPart()));
			LOGGER.info("deh- dateRange is empty");
			return criteria;
		}

		Date from = dateRange.get().getLowerBoundAsInstant();
		Date to = dateRange.get().getUpperBoundAsInstant();
		LOGGER.info("deh-..from date - " + from);
		LOGGER.info("deh-..to date - " + to);
		// only one search date was supplied with a greater than or equal to
		// operator in the url
		if (dateRange.get().getUpperBoundAsInstant() == null) {
			Comparable dateRangeLowerBound = LocalDateTime
					.ofInstant(dateRange.get().getLowerBoundAsInstant().toInstant(), ZoneId.systemDefault())
					.toLocalDate();
			criteria.where(builder.and(builder.equal(beneficiaryIdPath, patient.getIdPart()),
					builder.greaterThanOrEqualTo(dateFromPath, dateRangeLowerBound)));
			return criteria;
		}

		// only one search date was supplied with a less than or equal to
		// operator in the url
		if (dateRange.get().getLowerBoundAsInstant() == null) {
			Comparable dateRangeUpperBound = LocalDateTime
					.ofInstant(dateRange.get().getUpperBoundAsInstant().toInstant(), ZoneId.systemDefault())
					.toLocalDate();
			criteria.where(builder.and(builder.equal(beneficiaryIdPath, patient.getIdPart()),
					builder.lessThanOrEqualTo(dateFromPath, dateRangeUpperBound)));
			return criteria;
		}

		// both lower bound and upper bound search dates were supplied in the
		// url
		Comparable dateRangeLowerBound = LocalDateTime
				.ofInstant(dateRange.get().getLowerBoundAsInstant().toInstant(), ZoneId.systemDefault()).toLocalDate();
		Comparable dateRangeUpperBound = LocalDateTime
				.ofInstant(dateRange.get().getUpperBoundAsInstant().toInstant(), ZoneId.systemDefault()).toLocalDate();
		criteria.where(builder.and(builder.equal(beneficiaryIdPath, patient.getIdPart()),
						builder.between(dateFromPath, dateRangeLowerBound, dateRangeUpperBound)));
		return criteria;
	}
}
