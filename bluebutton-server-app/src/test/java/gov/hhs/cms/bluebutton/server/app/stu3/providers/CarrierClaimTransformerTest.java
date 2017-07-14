package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Arrays;
import java.util.List;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.junit.Assert;
import org.junit.Test;

import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaim;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResource;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResourceGroup;
import gov.hhs.cms.bluebutton.server.app.ServerTestUtils;

/**
 * Unit tests for {@link CarrierClaimTransformer}.
 */
public final class CarrierClaimTransformerTest {
	/**
	 * Verifies that {@link CarrierClaimTransformer#transform(Object)} works as
	 * expected when run against the {@link StaticRifResource#SAMPLE_A_CARRIER}
	 * {@link CarrierClaim}.
	 */
	@Test
	public void transformSampleARecord() {
		List<Object> parsedRecords = ServerTestUtils
				.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		CarrierClaim claim = parsedRecords.stream().filter(r -> r instanceof CarrierClaim).map(r -> (CarrierClaim) r)
				.findFirst().get();

		ExplanationOfBenefit eob = CarrierClaimTransformer.transform(claim);
		assertMatches(claim, eob);
	}

	/**
	 * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if
	 * it were produced from the specified {@link CarrierClaim}.
	 * 
	 * @param claim
	 *            the {@link CarrierClaim} that the {@link ExplanationOfBenefit}
	 *            was generated from
	 * @param eob
	 *            the {@link ExplanationOfBenefit} that was generated from the
	 *            specified {@link CarrierClaim}
	 */
	static void assertMatches(CarrierClaim claim, ExplanationOfBenefit eob) {
		Assert.assertEquals(TransformerUtils.buildEobId(ClaimType.CARRIER, claim.getClaimId()),
				eob.getIdElement().getIdPart());
	}
}
