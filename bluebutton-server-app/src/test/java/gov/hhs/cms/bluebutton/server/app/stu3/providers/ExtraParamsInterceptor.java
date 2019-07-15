package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.io.IOException;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;

/**
 * An interceptor class to add headers to requests for supplying additional
 * parameters to FHIR "read" operations. The operation only allows for certain
 * parameters to be sent (e.g. {@link RequestDetails}) so we add headers with
 * our own parameters to the request in order to make use of them.
 */
public class ExtraParamsInterceptor implements IClientInterceptor {

	private String includeIdentifiers = "false";

	@Override
	public void interceptRequest(IHttpRequest theRequest) {
		theRequest.addHeader("IncludeIdentifiers", this.includeIdentifiers);
	}

	@Override
	public void interceptResponse(IHttpResponse theResponse) throws IOException {
		// TODO Auto-generated method stub

	}

	public void setIncludeIdentifiers(String includeIdentifiers) {
		this.includeIdentifiers = includeIdentifiers;
	}

}
