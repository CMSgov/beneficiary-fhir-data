package gov.hhs.cms.bluebutton.server.app.utils;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import ca.uhn.fhir.rest.client.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;

public class JsonInterceptor implements IClientInterceptor {
	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(JsonInterceptor.class);

	private Logger myLog = ourLog;
	private String response;

	@Override
	public void interceptRequest(IHttpRequest theRequest) {
		// TODO Auto-generated method stub

	}

	@Override
	public void interceptResponse(IHttpResponse theResponse) throws IOException {
		theResponse.bufferEntity();
		InputStream respEntity = null;
		try {
			respEntity = theResponse.readEntity();
			if (respEntity != null) {
				final byte[] bytes;
				try {
					bytes = IOUtils.toByteArray(respEntity);
				} catch (IllegalStateException e) {
					throw new InternalErrorException(e);
				}
				response = new String(bytes, "UTF-8");
				myLog.info("Client response body:\n{}", response);
			} else {
				myLog.info("Client response body: (none)");
			}
		} finally {
			IOUtils.closeQuietly(respEntity);
		}
	}

	public String getResponse() {
		return response;
	}

}
