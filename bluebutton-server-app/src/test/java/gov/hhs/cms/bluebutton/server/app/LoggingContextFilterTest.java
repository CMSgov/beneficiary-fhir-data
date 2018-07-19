package gov.hhs.cms.bluebutton.server.app;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link LoggingContextFilter}.
 */
public final class LoggingContextFilterTest {
	/**
	 * Verifies that {@link LoggingContextFilter#stripSensitiveInfoFromUri(String)}
	 * works as expected.
	 */
	@Test
	public void stripSensitiveInfoFromUri() {
		Assert.assertEquals("/v1/fhir/Patient/***",
				LoggingContextFilter.stripSensitiveInfoFromUri("/v1/fhir/Patient/1234"));
		Assert.assertEquals("/v1/fhir/Patient/***",
				LoggingContextFilter.stripSensitiveInfoFromUri("/v1/fhir/Patient/-1234"));
		Assert.assertEquals("/v1/fhir/Patient/***?foo=bar",
				LoggingContextFilter.stripSensitiveInfoFromUri("/v1/fhir/Patient/1234?foo=bar"));

		Assert.assertEquals("/v1/fhir/Coverage/part-a-***",
				LoggingContextFilter.stripSensitiveInfoFromUri("/v1/fhir/Coverage/part-a-1234"));
		Assert.assertEquals("/v1/fhir/Coverage/part-a-***",
				LoggingContextFilter.stripSensitiveInfoFromUri("/v1/fhir/Coverage/part-a--1234"));
		Assert.assertEquals("/v1/fhir/Coverage/part-a-***?foo=bar",
				LoggingContextFilter.stripSensitiveInfoFromUri("/v1/fhir/Coverage/part-a-1234?foo=bar"));

		Assert.assertEquals("/v1/fhir/ExplanationOfBenefit/carrier-***",
				LoggingContextFilter.stripSensitiveInfoFromUri("/v1/fhir/ExplanationOfBenefit/carrier-1234"));
		Assert.assertEquals("/v1/fhir/ExplanationOfBenefit/carrier-***",
				LoggingContextFilter.stripSensitiveInfoFromUri("/v1/fhir/ExplanationOfBenefit/carrier--1234"));
		Assert.assertEquals("/v1/fhir/ExplanationOfBenefit/carrier-***?foo=bar",
				LoggingContextFilter.stripSensitiveInfoFromUri("/v1/fhir/ExplanationOfBenefit/carrier-1234?foo=bar"));
	}
}
