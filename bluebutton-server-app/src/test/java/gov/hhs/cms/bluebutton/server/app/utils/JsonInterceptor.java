package gov.hhs.cms.bluebutton.server.app.utils;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;

/**
 * An interceptor class to save endpoint responses from requests to a
 * fhirClient.
 */
public class JsonInterceptor implements IClientInterceptor {
	private String response;

	@Override
	public void interceptRequest(IHttpRequest theRequest) {
		// TODO Auto-generated method stub

	}

	@Override
	public void interceptResponse(IHttpResponse theResponse) throws IOException {
		/*
		 * The following code comes from {@link LoggingInterceptor} and has been
		 * re-purposed and used here to save responses from the fhirClient.
		 */
		theResponse.bufferEntity();
		InputStream respEntity = null;
		try {
			respEntity = theResponse.readEntity();
			final byte[] bytes;
			try {
				bytes = IOUtils.toByteArray(respEntity);
			} catch (IllegalStateException e) {
				throw new InternalErrorException(e);
			}
			response = new String(bytes, "UTF-8");
		} finally {
			IOUtils.closeQuietly(respEntity);
		}
	}

	public String getResponse() {
		return response;
	}

}
