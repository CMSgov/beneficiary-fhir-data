package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.io.IOException;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;

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
