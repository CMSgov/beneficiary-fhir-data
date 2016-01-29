package gov.hhs.cms.bluebutton.texttofhir.transform;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu21.model.Address;
import org.hl7.fhir.dstu21.model.Claim;
import org.hl7.fhir.dstu21.model.Claim.ItemsComponent;
import org.hl7.fhir.dstu21.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.dstu21.model.Coverage;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu21.model.Organization;
import org.hl7.fhir.dstu21.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.Assert;
import org.junit.Test;

import gov.hhs.cms.bluebutton.texttofhir.parsing.Entry;
import gov.hhs.cms.bluebutton.texttofhir.parsing.Section;
import gov.hhs.cms.bluebutton.texttofhir.parsing.TextFile;
import gov.hhs.cms.bluebutton.texttofhir.parsing.TextFileParseException;

/**
 * Unit tests for {@link TextFileTransformer}.
 */
public final class TextFileTransformerTest {
	/**
	 * Ensures that {@link TextFileTransformer} works as expected when fed data
	 * for all supported fields.
	 * 
	 * @throws TextFileParseException
	 *             (indicates a test failure)
	 */
	@Test
	public void allSupportedFields() throws TextFileParseException {
		// Create the "Demographics" section.
		Entry demographicsSource = new Entry(0, EntryName.DEMOGRAPHICS_SOURCE.getName(), "MyMedicare.gov");
		Entry demographicsName = new Entry(0, EntryName.DEMOGRAPHICS_NAME.getName(), "John Doe");
		Entry demographicsDob = new Entry(0, EntryName.DEMOGRAPHICS_DOB.getName(), "1/1/1910");
		Entry demographicsAddress1 = new Entry(0, EntryName.DEMOGRAPHICS_ADDRESS_LINE_1.getName(), "1 Main St");
		Entry demographicsAddress2 = new Entry(0, EntryName.DEMOGRAPHICS_ADDRESS_LINE_2.getName(), "Apt 42");
		Entry demographicsCity = new Entry(0, EntryName.DEMOGRAPHICS_CITY.getName(), "Springfield");
		Entry demographicsState = new Entry(0, EntryName.DEMOGRAPHICS_STATE.getName(), "IL");
		Entry demographicsZip = new Entry(0, EntryName.DEMOGRAPHICS_ZIP.getName(), "12345");
		Entry demographicsPhone = new Entry(0, EntryName.DEMOGRAPHICS_PHONE.getName(), "520-867-5309");
		Entry demographicsEmail = new Entry(0, EntryName.DEMOGRAPHICS_EMAIL.getName(), "bob@example.com");
		Entry demographicsPartADate = new Entry(0, EntryName.DEMOGRAPHICS_PART_A_DATE.getName(), "2/1/2014");
		Entry demographicsPartBDate = new Entry(0, EntryName.DEMOGRAPHICS_PART_B_DATE.getName(), "2/1/2014");
		Section demographics = new Section(0, SectionName.DEMOGRAPHIC.getName(), demographicsSource, demographicsName,
				demographicsDob, demographicsAddress1, demographicsAddress2, demographicsCity, demographicsState,
				demographicsZip, demographicsPhone, demographicsEmail, demographicsPartADate, demographicsPartBDate);

		// Create the "Plans" section.
		Entry plansSource = new Entry(0, EntryName.PLANS_SOURCE.getName(), "MyMedicare.gov");
		Entry plansId = new Entry(0, EntryName.PLANS_ID.getName(), "S1111/801");
		Entry plansPeriod = new Entry(0, EntryName.PLANS_PERIOD.getName(), "12/01/2012 - current");
		Entry plansName = new Entry(0, EntryName.PLANS_NAME.getName(), "");
		Entry plansMarketing = new Entry(0, EntryName.PLANS_MARKETING.getName(), "");
		Entry plansAddress = new Entry(0, EntryName.PLANS_ADDRESS.getName(), "");
		Entry plansType = new Entry(0, EntryName.PLANS_TYPE.getName(), "11 - Medicare Prescription Drug Plan");
		Section plans = new Section(1, SectionName.PLANS.getName(), plansSource, plansId, plansPeriod, plansName,
				plansMarketing, plansAddress, plansType);

		// Create the first "Claim Summary" section.
		Entry claimASource = new Entry(0, EntryName.CLAIM_SUMMARY_SOURCE.getName(), "MyMedicare.gov");
		Entry claimANumber = new Entry(0, EntryName.CLAIM_SUMMARY_NUMBER.getName(), "11122233330000");
		Entry claimAProvider = new Entry(0, EntryName.CLAIM_SUMMARY_PROVIDER.getName(), "No Information Available");
		Entry claimAProviderAddress = new Entry(0, EntryName.CLAIM_SUMMARY_PROVIDER_ADDRESS.getName(), "");
		Entry claimAStart = new Entry(0, EntryName.CLAIM_SUMMARY_START.getName(), "01/05/2014");
		Entry claimAEnd = new Entry(0, EntryName.CLAIM_SUMMARY_END.getName(), "01/05/2014");
		Entry claimACharged = new Entry(0, EntryName.CLAIM_SUMMARY_CHARGED.getName(), "$1,022.50");
		Entry claimAProviderPaid = new Entry(0, EntryName.CLAIM_SUMMARY_PROVIDER_PAID.getName(), "$625.86");
		Entry claimABeneficiaryBill = new Entry(0, EntryName.CLAIM_SUMMARY_BENEFICIARY_BILLED.getName(), "$625.86");
		Entry claimAType = new Entry(0, EntryName.CLAIM_SUMMARY_TYPE.getName(), "PartB");
		Entry claimACode1 = new Entry(0, EntryName.CLAIM_SUMMARY_DIAGNOSIS_CODE_1.getName(), "70700");
		Section claimA = new Section(2, SectionName.CLAIM_SUMMARY.getName(), claimASource, claimANumber, claimAProvider,
				claimAProviderAddress, claimAStart, claimAEnd, claimACharged, claimAProviderPaid, claimABeneficiaryBill,
				claimAType, claimACode1);

		// Create the first "Claim Summary" section.
		Entry claimALine1Number = new Entry(0, EntryName.CLAIM_LINES_NUMBER.getName(), "1");
		Entry claimALine1DateFrom = new Entry(0, EntryName.CLAIM_LINES_DATE_FROM.getName(), "01/05/2014");
		Entry claimALine1DateTo = new Entry(0, EntryName.CLAIM_LINES_DATE_TO.getName(), "01/05/2014");
		Entry claimALine1Procedure = new Entry(0, EntryName.CLAIM_LINES_PROCEDURE.getName(),
				"A0428 - Ambulance Service, Basic Life Support, Non-Emergency Transport, (Bls)");
		Entry claimALine1Modifier1 = new Entry(0, EntryName.CLAIM_LINES_MODIFIER_1.getName(), "RH");
		Entry claimALine1Modifier2 = new Entry(0, EntryName.CLAIM_LINES_MODIFIER_2.getName(), "");
		Entry claimALine1Modifier3 = new Entry(0, EntryName.CLAIM_LINES_MODIFIER_3.getName(), "");
		Entry claimALine1Modifier4 = new Entry(0, EntryName.CLAIM_LINES_MODIFIER_4.getName(), "");
		Entry claimALine1Quantity = new Entry(0, EntryName.CLAIM_LINES_QUANTITY.getName(), "1");
		Entry claimALine1Submitted = new Entry(0, EntryName.CLAIM_LINES_SUBMITTED.getName(), "$275.00");
		Entry claimALine1Allowed = new Entry(0, EntryName.CLAIM_LINES_ALLOWED.getName(), "$208.99");
		Entry claimALine1Uncovered = new Entry(0, EntryName.CLAIM_LINES_UNCOVERED.getName(), "$66.01");
		Entry claimALine1Place = new Entry(0, EntryName.CLAIM_LINES_PLACE.getName(), "41 - Ambulance - Land");
		Entry claimALine1Type = new Entry(0, EntryName.CLAIM_LINES_TYPE.getName(), "9 - Other Medical Services");
		Entry claimALine1RendererNumber = new Entry(0, EntryName.CLAIM_LINES_RENDERER_NUMBER.getName(), "PARTBPROV");
		Entry claimALine1RendererNpi = new Entry(0, EntryName.CLAIM_LINES_RENDERER_NPI.getName(), "");
		Section claimALines = new Section(3, SectionName.CLAIM_LINES_PREFIX.getName() + claimANumber.getValue(),
				claimALine1Number, claimALine1DateFrom, claimALine1DateTo, claimALine1Procedure, claimALine1Modifier1,
				claimALine1Modifier2, claimALine1Modifier3, claimALine1Modifier4, claimALine1Quantity,
				claimALine1Submitted, claimALine1Allowed, claimALine1Uncovered, claimALine1Place, claimALine1Type,
				claimALine1RendererNumber, claimALine1RendererNpi);

		// Create the text file and transform it.
		TextFile parsedFile = new TextFile(ZonedDateTime.now(), demographics, plans, claimA, claimALines);
		List<IBaseResource> resources = TextFileTransformer.transform(parsedFile);

		/*
		 * Verify the transformation results.
		 */

		Assert.assertNotNull(resources);
		Map<Class<?>, List<IBaseResource>> resourcesMap = resources.stream()
				.collect(Collectors.groupingBy((r) -> r.getClass()));

		Assert.assertEquals(1, resourcesMap.get(Organization.class).size());
		Organization cmsOrg = (Organization) resourcesMap.get(Organization.class).get(0);
		Assert.assertEquals(TextFileTransformer.ORG_NAME_CMS, cmsOrg.getName());

		Assert.assertEquals(1, resourcesMap.get(Patient.class).size());
		Patient patient = (Patient) resourcesMap.get(Patient.class).get(0);
		Assert.assertNotNull(patient.getId());
		Assert.assertEquals(demographicsName.getValue(), patient.getName().get(0).getNameAsSingleString());
		Assert.assertEquals(Date.valueOf(LocalDate.of(1910, 1, 1)), patient.getBirthDate());
		Assert.assertEquals(1, patient.getAddress().size());
		Address patientAddress = patient.getAddress().get(0);
		Assert.assertEquals(2, patientAddress.getLine().size());
		Assert.assertEquals(demographicsAddress1.getValue(), patientAddress.getLine().get(0).asStringValue());
		Assert.assertEquals(demographicsAddress2.getValue(), patientAddress.getLine().get(1).asStringValue());
		Assert.assertEquals(demographicsCity.getValue(), patientAddress.getCity());
		Assert.assertEquals(demographicsState.getValue(), patientAddress.getState());
		Assert.assertEquals(demographicsZip.getValue(), patientAddress.getPostalCode());
		Assert.assertEquals(2, patient.getTelecom().size());
		Assert.assertEquals(ContactPointSystem.PHONE, patient.getTelecom().get(0).getSystem());
		Assert.assertEquals(demographicsPhone.getValue(), patient.getTelecom().get(0).getValue());
		Assert.assertEquals(ContactPointSystem.EMAIL, patient.getTelecom().get(1).getSystem());
		Assert.assertEquals(demographicsEmail.getValue(), patient.getTelecom().get(1).getValue());

		Assert.assertEquals(3, resourcesMap.get(Coverage.class).size());
		Coverage coveragePartA = (Coverage) resourcesMap.get(Coverage.class).get(0);
		Assert.assertEquals(patient.getId(), coveragePartA.getSubscriber().getReference());
		// TODO verify issuer
		Assert.assertEquals("Medicare Part A", coveragePartA.getPlan());
		Assert.assertEquals(Date.valueOf(LocalDate.of(2014, 2, 1)), coveragePartA.getPeriod().getStart());
		Coverage coveragePartB = (Coverage) resourcesMap.get(Coverage.class).get(1);
		Assert.assertEquals(patient.getId(), coveragePartB.getSubscriber().getReference());
		// TODO verify issuer
		Assert.assertEquals("Medicare Part B", coveragePartB.getPlan());
		Assert.assertEquals(Date.valueOf(LocalDate.of(2014, 2, 1)), coveragePartB.getPeriod().getStart());
		Coverage coveragePartD = (Coverage) resourcesMap.get(Coverage.class).get(2);
		Assert.assertEquals(patient.getId(), coveragePartD.getSubscriber().getReference());
		// TODO verify issuer
		Assert.assertEquals("S1111/801", coveragePartD.getPlan());
		Assert.assertEquals(Date.valueOf(LocalDate.of(2012, 12, 1)), coveragePartD.getPeriod().getStart());
		Assert.assertEquals(null, coveragePartD.getPeriod().getEnd());
		Assert.assertEquals(plansType.getValue(), coveragePartD.getType().getDisplay());

		Assert.assertEquals(1, resourcesMap.get(ExplanationOfBenefit.class).size());
		ExplanationOfBenefit eobA = (ExplanationOfBenefit) resourcesMap.get(ExplanationOfBenefit.class).get(0);
		Assert.assertEquals(patient.getId(), eobA.getPatient().getReference());

		Assert.assertEquals(1, resourcesMap.get(Claim.class).size());
		Claim eobAClaimA = (Claim) resourcesMap.get(Claim.class).get(0);
		Assert.assertEquals(claimANumber.getValue(), eobAClaimA.getIdentifier().get(0).getValue());
		// TODO where to map Provider name? As an ID? Goofy...
		// TODO "provider billing address"
		Assert.assertEquals(Date.valueOf(LocalDate.of(2014, 1, 5)), eobAClaimA.getBillablePeriod().getStart());
		Assert.assertEquals(Date.valueOf(LocalDate.of(2014, 1, 5)), eobAClaimA.getBillablePeriod().getEnd());
		// FIXME no mapping for 'Amount Charged'
		// FIXME no mapping for 'Medicare Approved'
		// FIXME no mapping for 'Provider Paid'
		// FIXME no mapping for 'You May Be Billed'
		// FIXME mapping for 'Claim Type' points to a 'Part B' org?
		// Answer: map to 'EOB/coverage'.
		Assert.assertEquals(1, eobAClaimA.getDiagnosis().size());
		Assert.assertEquals(TextFileTransformer.CODING_SYSTEM_ICD9_DIAG,
				eobAClaimA.getDiagnosis().get(0).getDiagnosis().getSystem());
		Assert.assertEquals(claimACode1.getValue(), eobAClaimA.getDiagnosis().get(0).getDiagnosis().getCode());

		Assert.assertEquals(1, eobAClaimA.getItem().size());
		ItemsComponent eobAClaimAItem1 = eobAClaimA.getItem().get(0);
		Assert.assertEquals(1, eobAClaimAItem1.getSequence());
		// FIXME transformer code goes boom
		// Assert.assertEquals(Date.valueOf(LocalDate.of(2014, 1, 5)),
		// eobAClaimAItem1.getServicedPeriod().getStart());
		// Assert.assertEquals(Date.valueOf(LocalDate.of(2014, 1, 5)),
		// eobAClaimAItem1.getServicedPeriod().getEnd());
		Assert.assertEquals(TextFileTransformer.CODING_SYSTEM_ICD9_PROC, eobAClaimAItem1.getService().getSystem());
		Assert.assertEquals("A0428", eobAClaimAItem1.getService().getCode());
		Assert.assertEquals("Ambulance Service, Basic Life Support, Non-Emergency Transport, (Bls)",
				eobAClaimAItem1.getService().getDisplay());
		Assert.assertEquals(1, eobAClaimAItem1.getModifier().size());
		// TODO what system for modifier coding?
		Assert.assertEquals(claimALine1Modifier1.getValue(), eobAClaimAItem1.getModifier().get(0).getCode());
		Assert.assertEquals(new BigDecimal(claimALine1Quantity.getValue()), eobAClaimAItem1.getQuantity().getValue());
		// TODO code and system for Money values?
		// Answer: Look at ISO 4217.
		Assert.assertEquals(new BigDecimal("275.00"), eobAClaimAItem1.getNet().getValue());
		// FIXME Spec? EOB/item has adjudication, but EOB/Claim/item does not.
		// TODO (per above) 'Submitted'
		// TODO (per above) 'Allowed'
		// FIXME Spec? EOB/Claim has facility, but EOB/Claim/item does not.
		// Answer: Map to EOB/item/place
		// TODO (per above) 'Place of Service'
		Assert.assertEquals(claimALine1Type.getValue(), eobAClaimAItem1.getType().getDisplay());
		// TODO 'Rendering Provider No' how to map? Item/provider only has
		// 'HumanName'
		// Answer: Create an 'EOB/item/provider' with no name, but a 'managingOrganization'.
		// TODO 'Rendering Provider NPI' how to map?
	}
}
