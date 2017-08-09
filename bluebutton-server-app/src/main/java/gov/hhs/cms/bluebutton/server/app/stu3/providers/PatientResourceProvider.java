package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;
import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary_;

/**
 * This FHIR {@link IResourceProvider} adds support for STU3 {@link Patient}
 * resources, derived from the CCW beneficiaries.
 */
@Component
public final class PatientResourceProvider implements IResourceProvider {
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
		return Patient.class;
	}

	/**
	 * <p>
	 * Adds support for the FHIR "read" operation, for {@link Patient}s. The
	 * {@link Read} annotation indicates that this method supports the read
	 * and/or vread operation.
	 * </p>
	 * <p>
	 * Read operations take a single parameter annotated with {@link IdParam},
	 * and should return a single resource instance.
	 * </p>
	 * 
	 * @param patientId
	 *            The read operation takes one parameter, which must be of type
	 *            {@link IdType} and must be annotated with the {@link IdParam}
	 *            annotation.
	 * @return Returns a resource matching the specified {@link IdDt}, or
	 *         <code>null</code> if none exists.
	 */
	@Read(version = true)
	public Patient read(@IdParam IdType patientId) {
		if (patientId == null)
			throw new IllegalArgumentException();
		if (patientId.getVersionIdPartAsLong() != null)
			throw new IllegalArgumentException();

		String beneIdText = patientId.getIdPart();
		if (beneIdText == null || beneIdText.trim().isEmpty())
			throw new IllegalArgumentException();

		CriteriaBuilder builder = entityManager.getCriteriaBuilder();

		CriteriaQuery<Beneficiary> criteria = builder.createQuery(Beneficiary.class);
		Root<Beneficiary> root = criteria.from(Beneficiary.class);
		criteria.select(root);
		criteria.where(builder.equal(root.get(Beneficiary_.beneficiaryId), beneIdText));

		Beneficiary beneficiary = null;
		try {
			beneficiary = entityManager.createQuery(criteria).getSingleResult();
		} catch (NoResultException e) {
			throw new ResourceNotFoundException(patientId);
		}

		Patient patient = BeneficiaryTransformer.transform(beneficiary);
		return patient;
	}

	/**
	 * <p>
	 * Adds support for the FHIR "search" operation for {@link Patient}s,
	 * allowing users to search by {@link Patient#getId()}.
	 * <p>
	 * The {@link Search} annotation indicates that this method supports the
	 * search operation. There may be many different methods annotated with this
	 * {@link Search} annotation, to support many different search criteria.
	 * </p>
	 * 
	 * @param logicalId
	 *            a {@link TokenParam} (with no system, per the spec) for the
	 *            {@link Patient#getId()} to try and find a matching
	 *            {@link Patient} for
	 * @return Returns a {@link List} of {@link Patient}s, which may contain
	 *         multiple matching resources, or may also be empty.
	 */
	@Search
	public List<Patient> findByLogicalId(@RequiredParam(name = Patient.SP_RES_ID) TokenParam logicalId) {
		if (logicalId.getQueryParameterQualifier() != null)
			throw new InvalidRequestException(
					"Unsupported query parameter qualifier: " + logicalId.getQueryParameterQualifier());
		if (logicalId.getSystem() != null && !logicalId.getSystem().isEmpty())
			throw new InvalidRequestException("Unsupported query parameter system: " + logicalId.getSystem());
		if (logicalId.getValueNotNull().isEmpty())
			throw new InvalidRequestException("Unsupported query parameter value: " + logicalId.getValue());

		try {
			return Arrays.asList(read(new IdType(logicalId.getValue())));
		} catch (ResourceNotFoundException e) {
			return new LinkedList<>();
		}
	}

	/**
	 * <p>
	 * Adds support for the FHIR "search" operation for {@link Patient}s,
	 * allowing users to search by {@link Patient#getIdentifier()}.
	 * Specifically, the following criteria are supported:
	 * </p>
	 * <ul>
	 * <li>Matching a {@link Beneficiary#getHicn()} hash value: when
	 * {@link TokenParam#getSystem()} matches
	 * {@link BeneficiaryTransformer#CODING_SYSTEM_CCW_BENE_HICN_HASH}.</li>
	 * </ul>
	 * <p>
	 * Searches that don't match one of the above forms are not supported.
	 * </p>
	 * <p>
	 * The {@link Search} annotation indicates that this method supports the
	 * search operation. There may be many different methods annotated with this
	 * {@link Search} annotation, to support many different search criteria.
	 * </p>
	 * 
	 * @param identifier
	 *            an {@link Identifier} {@link TokenParam} for the
	 *            {@link Patient#getIdentifier()} to try and find a matching
	 *            {@link Patient} for
	 * @return Returns a {@link List} of {@link Patient}s, which may contain
	 *         multiple matching resources, or may also be empty.
	 */
	@Search
	public List<Patient> findByIdentifier(@RequiredParam(name = Patient.SP_IDENTIFIER) TokenParam identifier) {
		if (identifier.getQueryParameterQualifier() != null)
			throw new InvalidRequestException(
					"Unsupported query parameter qualifier: " + identifier.getQueryParameterQualifier());

		if (!BeneficiaryTransformer.CODING_SYSTEM_CCW_BENE_HICN_HASH.equals(identifier.getSystem()))
			throw new InvalidRequestException("Unsupported identifier system: " + identifier.getSystem());

		try {
			return Arrays.asList(findByHicnHash(identifier.getValue()));
		} catch (NoResultException e) {
			return new LinkedList<>();
		}
	}

	/**
	 * @param hicnHash
	 *            the {@link Beneficiary#getHicn()} hash value to match
	 * @return a FHIR {@link Patient} for the CCW {@link Beneficiary} that
	 *         matches the specified {@link Beneficiary#getHicn()} hash value
	 * @throws NoResultException
	 *             A {@link NoResultException} will be thrown if no matching
	 *             {@link Beneficiary} can be found
	 */
	private Patient findByHicnHash(String hicnHash) {
		if (hicnHash == null || hicnHash.trim().isEmpty())
			throw new IllegalArgumentException();

		CriteriaBuilder builder = entityManager.getCriteriaBuilder();

		CriteriaQuery<Beneficiary> criteria = builder.createQuery(Beneficiary.class);
		Root<Beneficiary> root = criteria.from(Beneficiary.class);
		criteria.select(root);
		criteria.where(builder.equal(root.get(Beneficiary_.hicn), hicnHash));

		Beneficiary beneficiary = entityManager.createQuery(criteria).getSingleResult();

		Patient patient = BeneficiaryTransformer.transform(beneficiary);
		return patient;
	}
}
