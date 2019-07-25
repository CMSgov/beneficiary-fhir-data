package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.io.IOException;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.PatientResourceProvider.IncludeIdentifiersMode;

/**
 * An interceptor class to add headers to requests for supplying additional
 * parameters to FHIR "read" operations. The operation only allows for certain
 * parameters to be sent (e.g. {@link RequestDetails}) so we add headers with
 * our own parameters to the request in order to make use of them.
 */
public class ExtraParamsInterceptor implements IClientInterceptor {

	private IncludeIdentifiersMode includeIdentifiersMode = IncludeIdentifiersMode.OMIT_HICNS_AND_MBIS;

	@Override
	public void interceptRequest(IHttpRequest theRequest) {
		if (includeIdentifiersMode == IncludeIdentifiersMode.INCLUDE_HICNS_AND_MBIS)
			theRequest.addHeader(IncludeIdentifiersMode.HEADER_NAME_INCLUDE_IDENTIFIERS, Boolean.TRUE.toString());
	}

	@Override
	public void interceptResponse(IHttpResponse theResponse) throws IOException {
		// TODO Auto-generated method stub

	}

	public void setIncludeIdentifiers(IncludeIdentifiersMode includeIdentifiersMode) {
		this.includeIdentifiersMode = includeIdentifiersMode;
	}
}
