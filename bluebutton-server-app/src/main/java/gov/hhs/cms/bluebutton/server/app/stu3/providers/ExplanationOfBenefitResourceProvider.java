package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.List;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;

/**
 * This FHIR {@link IResourceProvider} adds support for STU3
 * {@link ExplanationOfBenefit} resources, derived from the CCW claims.
 */
public final class ExplanationOfBenefitResourceProvider implements IResourceProvider {
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
	 * this method supports the read and/or vread operation.
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
	public ExplanationOfBenefit read(@IdParam IdType theId) {
		// TODO
		return null;
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
		// TODO
		return null;
	}
}
