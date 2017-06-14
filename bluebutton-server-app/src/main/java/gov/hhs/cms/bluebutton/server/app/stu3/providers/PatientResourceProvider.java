package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

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
	 * @param theId
	 *            The read operation takes one parameter, which must be of type
	 *            {@link IdType} and must be annotated with the {@link IdParam}
	 *            annotation.
	 * @return Returns a resource matching the specified {@link IdDt}, or
	 *         <code>null</code> if none exists.
	 */
	@Read(version = true)
	public Patient read(@IdParam IdType theId) {
		// TODO
		return null;
	}

	/**
	 * <p>
	 * Adds support for the FHIR "search" operation for {@link Patient}s,
	 * allowing users to search by {@link Patient#getIdentifier()}.
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
		// TODO
		return null;
	}
}
