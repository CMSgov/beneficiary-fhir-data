package gov.hhs.cms.bluebutton.texttofhir.transform;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hl7.fhir.dstu21.exceptions.FHIRException;
import org.hl7.fhir.dstu21.model.Address;
import org.hl7.fhir.dstu21.model.Claim;
import org.hl7.fhir.dstu21.model.Coding;
import org.hl7.fhir.dstu21.model.ContactPoint;
import org.hl7.fhir.dstu21.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.dstu21.model.Coverage;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit.ItemsComponent;
import org.hl7.fhir.dstu21.model.HumanName;
import org.hl7.fhir.dstu21.model.IdType;
import org.hl7.fhir.dstu21.model.Organization;
import org.hl7.fhir.dstu21.model.Patient;
import org.hl7.fhir.dstu21.model.Period;
import org.hl7.fhir.dstu21.model.Reference;
import org.hl7.fhir.instance.model.api.IBaseResource;

import gov.hhs.cms.bluebutton.texttofhir.parsing.Entry;
import gov.hhs.cms.bluebutton.texttofhir.parsing.Section;
import gov.hhs.cms.bluebutton.texttofhir.parsing.TextFile;
import gov.hhs.cms.bluebutton.texttofhir.parsing.TextFileParseException;

/**
 * Can transform parsed CMS/MyMedicare.gov text files into DSTU 2.1 FHIR
 * resources.
 */
public final class TextFileTransformer {
	/**
	 * This is the date format used in Blue Button text files.
	 */
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy");

	/**
	 * The regex for parsing diagnostic/procedure codes with a description from
	 * the input text file format.
	 */
	private static final Pattern CODE_WITH_DESC = Pattern.compile("(.+) - (.+)");

	/**
	 * The regex for parsing US currency amounts.
	 */
	private static final Pattern MONEY = Pattern.compile("\\$(\\d+(\\.\\d+))");

	private static final String[] VALUES_TO_IGNORE = { "* Not Available *" };

	static final String ORG_NAME_CMS = "Centers for Medicare & Medicaid Services";

	static final String CODING_SYSTEM_ICD9_DIAG = "http://hl7.org/fhir/sid/icd-9-cm/diagnosis";

	static final String CODING_SYSTEM_ICD9_PROC = "http://hl7.org/fhir/sid/icd-9-cm/procedure";

	static final String CODING_SYSTEM_MONEY = "urn:std:iso:4217";

	static final String CODING_SYSTEM_MONEY_US = "USD";

	static final String PART_B_PLAN_NAME = "Medicare Part B";

	static final String ADJUDICATION_NON_COVERED = "non-covered";

	static final String ADJUDICATION_ALLOWED = "allowed";

	/**
	 * @param inputData
	 *            the {@link TextFile} data to be transformed into DSTU 2.1 FHIR
	 *            resources
	 * @return the DSTU 2.1 {@link IBaseResource}s that could be extracted from
	 *         the specified {@link TextFile}
	 * @throws TextFileParseException
	 *             A {@link TextFileParseException} will be thrown if the input
	 *             {@link TextFile} does not contain the expected entries.
	 */
	public static List<IBaseResource> transform(TextFile inputData) throws TextFileParseException {
		TransformationResults results = new TransformationResults();
		results.addResources(createCmsOrganization());
		results.addResources(extractPatient(inputData));
		results.addResources(extractCoverages(results, inputData));
		results.addResources(extractEobs(results, inputData));
		return results.getResources();
	}

	/**
	 * @return the {@link Organization} instance that should be used to
	 *         represent CMS
	 */
	private static IBaseResource createCmsOrganization() {
		Organization cmsOrg = new Organization();
		cmsOrg.setId(IdType.newRandomUuid());
		cmsOrg.setName(ORG_NAME_CMS);
		return cmsOrg;
	}

	/**
	 * @param inputData
	 *            the {@link TextFile} to extract data from
	 * @return the {@link Patient} contained in the specified input data
	 */
	private static IBaseResource extractPatient(TextFile inputData) {
		Section demoSection = inputData.getSection(SectionName.DEMOGRAPHIC);

		Patient patient = new Patient();
		patient.setId(IdType.newRandomUuid());

		String nameText = getStringOrNull(demoSection, EntryName.DEMOGRAPHICS_NAME);
		if (nameText != null) {
			patient.addName(new HumanName().setText(nameText));
		}

		String dateText = getStringOrNull(demoSection, EntryName.DEMOGRAPHICS_DOB);
		if (dateText != null) {
			LocalDate parsedDate = LocalDate.parse(dateText, DATE_FORMATTER);
			patient.setBirthDate(Date.valueOf(parsedDate));
		}

		String addressLine1Text = getStringOrNull(demoSection, EntryName.DEMOGRAPHICS_ADDRESS_LINE_1);
		String addressLine2Text = getStringOrNull(demoSection, EntryName.DEMOGRAPHICS_ADDRESS_LINE_2);
		String cityText = getStringOrNull(demoSection, EntryName.DEMOGRAPHICS_CITY);
		String stateText = getStringOrNull(demoSection, EntryName.DEMOGRAPHICS_STATE);
		String zipText = getStringOrNull(demoSection, EntryName.DEMOGRAPHICS_ZIP);
		if (addressLine1Text != null || addressLine2Text != null || cityText != null || stateText != null
				|| zipText != null) {
			Address address = new Address();
			patient.addAddress(address);
			if (addressLine1Text != null)
				address.addLine(addressLine1Text);
			if (addressLine2Text != null)
				address.addLine(addressLine2Text);
			address.setCity(cityText);
			address.setState(stateText);
			address.setPostalCode(zipText);
		}

		String phoneText = getStringOrNull(demoSection, EntryName.DEMOGRAPHICS_PHONE);
		if (phoneText != null) {
			patient.getTelecom().add(new ContactPoint().setSystem(ContactPointSystem.PHONE).setValue(phoneText));
		}

		String emailText = getStringOrNull(demoSection, EntryName.DEMOGRAPHICS_EMAIL);
		if (emailText != null) {
			patient.getTelecom().add(new ContactPoint().setSystem(ContactPointSystem.EMAIL).setValue(emailText));
		}

		return patient;
	}

	/**
	 * @param results
	 *            the in-progress {@link TransformationResults} that contains
	 *            the {@link IBaseResource}s created so far
	 * @param inputData
	 *            the {@link TextFile} to extract data from
	 * @return the {@link Coverage}s contained in the specified input data
	 * @throws TextFileParseException
	 *             A {@link TextFileParseException} will be thrown if the input
	 *             {@link TextFile} does not contain the expected entries.
	 */
	private static List<IBaseResource> extractCoverages(TransformationResults results, TextFile inputData)
			throws TextFileParseException {
		List<IBaseResource> coverages = new ArrayList<>();

		/*
		 * Pull the Part A/B start dates from the 'Demographics' section.
		 */
		Section demoSection = inputData.getSection(SectionName.DEMOGRAPHIC);

		String partADateText = getStringOrNull(demoSection, EntryName.DEMOGRAPHICS_PART_A_DATE);
		if (partADateText != null) {
			LocalDate parsedDate = LocalDate.parse(partADateText, DATE_FORMATTER);
			coverages.add(new Coverage().setSubscriber(results.createPatientReference())
					.setIssuer(results.createCmsOrganizationReference()).setPlan("Medicare Part A")
					.setPeriod(new Period().setStart(Date.valueOf(parsedDate))));
		}

		String partBDateText = getStringOrNull(demoSection, EntryName.DEMOGRAPHICS_PART_B_DATE);
		if (partBDateText != null) {
			LocalDate parsedDate = LocalDate.parse(partBDateText, DATE_FORMATTER);
			coverages.add(new Coverage().setSubscriber(results.createPatientReference())
					.setIssuer(results.createCmsOrganizationReference()).setPlan(PART_B_PLAN_NAME)
					.setPeriod(new Period().setStart(Date.valueOf(parsedDate))));
		}

		/*
		 * Pull the Part D info from the 'Plans' section.
		 */
		// TODO looks like this section could have multiple entries
		Section plansSection = inputData.getSection(SectionName.PLANS);

		int groupSize = 6;
		for (int entryIndex = 1; entryIndex < plansSection.getEntries().size(); entryIndex += groupSize) {
			Coverage coverage = new Coverage();
			coverages.add(coverage);

			coverage.setSubscriber(results.createPatientReference());
			coverage.setIssuer(results.createCmsOrganizationReference());

			Entry idEntry = getEntry(plansSection, entryIndex + 0, EntryName.PLANS_ID);
			coverage.setPlan(idEntry.getValue());

			coverage.setPeriod(new Period());
			Entry periodEntry = getEntry(plansSection, entryIndex + 1, EntryName.PLANS_PERIOD);
			Pattern PLAN_PERIOD_PATTERN = Pattern.compile("(.+) - (.+)");
			Matcher periodMatcher = PLAN_PERIOD_PATTERN.matcher(periodEntry.getValue());
			if (!periodMatcher.matches())
				throw new TextFileParseException(
						"Plan period does not match expected format: " + periodEntry.getValue());
			String periodStartText = periodMatcher.group(1);
			LocalDate periodStart = LocalDate.parse(periodStartText, DATE_FORMATTER);
			coverage.getPeriod().setStart(Date.valueOf(periodStart));
			String periodEndText = periodMatcher.group(2);
			if (periodEndText != null && !periodEndText.equals("current")) {
				LocalDate periodEnd = LocalDate.parse(periodEndText, DATE_FORMATTER);
				coverage.getPeriod().setEnd(Date.valueOf(periodEnd));
			}

			Entry typeEntry = getEntry(plansSection, entryIndex + 5, EntryName.PLANS_TYPE);
			coverage.setType(new Coding().setDisplay(typeEntry.getValue()));
		}

		return coverages;
	}

	/**
	 * @param results
	 *            the in-progress {@link TransformationResults} that contains
	 *            the {@link IBaseResource}s created so far
	 * @param inputData
	 *            the {@link TextFile} to extract data from
	 * @return the {@link ExplanationOfBenefit}s contained in the specified
	 *         input data
	 * @throws TextFileParseException
	 *             A {@link TextFileParseException} will be thrown if the input
	 *             {@link TextFile} does not contain the expected entries.
	 */
	private static List<IBaseResource> extractEobs(TransformationResults results, TextFile inputData)
			throws TextFileParseException {
		/*
		 * The claims have a weird format in the input data. The first claim
		 * section is named 'Claim Summary'. After that, will be a single 'Claim
		 * Lines for Claim Number: NNN' subsection for that claim, which
		 * contains all of the claim lines (via repeating entries). Subsequent
		 * claims then appear after that in sections that, instead of being
		 * named 'Claim Summary', have a blank name (but their lines subsection
		 * doesn't). Sigh.
		 */
		List<IBaseResource> resources = new ArrayList<>();

		// Find the 'Claim Summary' section and note its index.
		Integer claimSectionIndex = null;
		for (int i = 0; i < inputData.getSections().size(); i++) {
			if (inputData.getSections().get(i).getName().equals(SectionName.CLAIM_SUMMARY.getName())) {
				claimSectionIndex = i;
				break;
			}
		}

		// Bail out early if the input has no claims data.
		if (claimSectionIndex == null)
			return resources;

		int sectionIndex = 0;
		boolean foundClaimsStart = false;
		do {
			// Skip all of the sections until 'Claims Summary'.
			Section currentSection = inputData.getSections().get(sectionIndex);
			if (SectionName.CLAIM_SUMMARY.getName().equals(currentSection.getName()))
				foundClaimsStart = true;
			if (!foundClaimsStart) {
				sectionIndex++;
				continue;
			}

			// Should be at 'Claims Summary' or a blank-named section.
			if (!SectionName.CLAIM_SUMMARY.getName().equals(currentSection.getName())
					&& currentSection.getName() != null) {
				sectionIndex++;
				continue;
			}

			// Current section is a summary of the claim, next is lines.
			Section claimSummarySection = currentSection;
			String claimNumber = getStringOrThrow(claimSummarySection, EntryName.CLAIM_SUMMARY_NUMBER);
			Section claimLinesSection = inputData.getSections().get(sectionIndex + 1);
			String claimLinesSectionName = SectionName.CLAIM_LINES_PREFIX.getName() + claimNumber;
			if (!claimLinesSection.getName().equals(claimLinesSectionName))
				throw new TextFileParseException("Missing claim lines section: " + claimLinesSectionName);

			/*
			 * Our output format needs a bit of explanation: we are (mostly)
			 * producing an 'ExplanationOfBenefits' ('EOB') resource. Most of
			 * the data from the input 'Claim Summary' section will land in the
			 * EOB itself and the input 'Claim Lines' data will land in the
			 * output 'EOB/items' entries. However, to ensure that folks can
			 * trace the data back properly for customer service purposes, we'll
			 * set 'EOB/claim' to a stub 'Claim' resource with just the claim
			 * number. (The 'Claim' resource doesn't have payment adjudication
			 * and other fields that we need to use. 'EOB/items' entries do.)
			 */

			ExplanationOfBenefit eob = new ExplanationOfBenefit();
			resources.add(eob);
			eob.setId(IdType.newRandomUuid());
			eob.setPatient(results.createPatientReference());

			// TODO is billablePeriod equivalent to EOB/item/servicedPeriod?
			String serviceStartText = getStringOrThrow(claimSummarySection, EntryName.CLAIM_SUMMARY_START);
			LocalDate serviceStart = LocalDate.parse(serviceStartText, DATE_FORMATTER);
			eob.getBillablePeriod().setStart(Date.valueOf(serviceStart));

			String typeText = getStringOrNull(claimSummarySection, EntryName.CLAIM_SUMMARY_TYPE);
			if (typeText != null) {
				eob.getCoverage().setCoverage(results.createCoveragePartBReference());
			}

			String serviceEndText = getStringOrThrow(claimSummarySection, EntryName.CLAIM_SUMMARY_END);
			LocalDate serviceEnd = LocalDate.parse(serviceEndText, DATE_FORMATTER);
			eob.getBillablePeriod().setEnd(Date.valueOf(serviceEnd));

			Claim claim = new Claim();
			claim.setId(IdType.newRandomUuid());
			resources.add(claim);
			eob.setClaim(new Reference().setReference(claim.getId()));

			claim.addIdentifier().setValue(claimNumber);

			// TODO handle multiple diag codes
			String diagCode1Text = getStringOrNull(claimSummarySection, EntryName.CLAIM_SUMMARY_DIAGNOSIS_CODE_1);
			if (diagCode1Text != null)
				claim.addDiagnosis().getDiagnosis().setSystem(CODING_SYSTEM_ICD9_DIAG).setCode(diagCode1Text);

			/*
			 * Every 'Claim Lines ...' section should have exactly 16 entries
			 * per claim line/item.
			 */
			if (claimLinesSection.getEntries().size() % 16 != 0)
				throw new TextFileParseException(String.format("Unexpected number of entries (%d) in section '%s'.",
						claimLinesSection.getEntries().size(), claimLinesSectionName));

			// Walk through each claim line/item.
			for (int itemStartIndex = 0; itemStartIndex < claimLinesSection.getEntries().size(); itemStartIndex += 16) {
				/*
				 * Unlike elsewhere in this class, here we're going to pull
				 * entries by index instead of name. Makes the code less
				 * readable, but still a lot simpler than coping with repeating
				 * names.
				 */
				Entry entryLineNumber = getEntry(claimLinesSection, itemStartIndex + 0, EntryName.CLAIM_LINES_NUMBER);
				Entry entryServiceFrom = getEntry(claimLinesSection, itemStartIndex + 1,
						EntryName.CLAIM_LINES_DATE_FROM);
				Entry entryServiceTo = getEntry(claimLinesSection, itemStartIndex + 2, EntryName.CLAIM_LINES_DATE_TO);
				Entry entryProcedure = getEntry(claimLinesSection, itemStartIndex + 3, EntryName.CLAIM_LINES_PROCEDURE);
				Entry entryModifier1 = getEntry(claimLinesSection, itemStartIndex + 4,
						EntryName.CLAIM_LINES_MODIFIER_1);
				Entry entryModifier2 = getEntry(claimLinesSection, itemStartIndex + 5,
						EntryName.CLAIM_LINES_MODIFIER_2);
				Entry entryModifier3 = getEntry(claimLinesSection, itemStartIndex + 6,
						EntryName.CLAIM_LINES_MODIFIER_3);
				Entry entryModifier4 = getEntry(claimLinesSection, itemStartIndex + 7,
						EntryName.CLAIM_LINES_MODIFIER_4);
				Entry entryQuantity = getEntry(claimLinesSection, itemStartIndex + 8, EntryName.CLAIM_LINES_QUANTITY);
				Entry entrySubmitted = getEntry(claimLinesSection, itemStartIndex + 9, EntryName.CLAIM_LINES_SUBMITTED);
				Entry entryAllowed = getEntry(claimLinesSection, itemStartIndex + 10, EntryName.CLAIM_LINES_ALLOWED);
				Entry entryUncovered = getEntry(claimLinesSection, itemStartIndex + 11,
						EntryName.CLAIM_LINES_UNCOVERED);
				Entry entryPlace = getEntry(claimLinesSection, itemStartIndex + 12, EntryName.CLAIM_LINES_PLACE);
				Entry entryType = getEntry(claimLinesSection, itemStartIndex + 13, EntryName.CLAIM_LINES_TYPE);
				Entry entryRendererNumber = getEntry(claimLinesSection, itemStartIndex + 14,
						EntryName.CLAIM_LINES_RENDERER_NUMBER);
				Entry entryRendererNpi = getEntry(claimLinesSection, itemStartIndex + 15,
						EntryName.CLAIM_LINES_RENDERER_NPI);

				ItemsComponent eobItem = new ItemsComponent();
				eob.addItem(eobItem);

				String lineNumberText = entryLineNumber.getValue();
				eobItem.setSequence(Integer.parseInt(lineNumberText));

				eobItem.setServiced(new Period());
				if (entryServiceFrom != null && !entryServiceFrom.getValue().isEmpty()) {
					LocalDate itemServiceStart = LocalDate.parse(entryServiceFrom.getValue(), DATE_FORMATTER);
					try {
						eobItem.getServicedPeriod().setStart(Date.valueOf(itemServiceStart));
					} catch (FHIRException e) {
						// Shouldn't happen.
						throw new IllegalStateException(e);
					}
				}

				if (entryServiceTo != null && !entryServiceTo.getValue().isEmpty()) {
					LocalDate itemServiceEnd = LocalDate.parse(entryServiceTo.getValue(), DATE_FORMATTER);
					try {
						eobItem.getServicedPeriod().setEnd(Date.valueOf(itemServiceEnd));
					} catch (FHIRException e) {
						// Shouldn't happen.
						throw new IllegalStateException(e);
					}
				}

				if (entryProcedure != null && !entryProcedure.getValue().isEmpty()) {
					Matcher matcher = CODE_WITH_DESC.matcher(entryProcedure.getValue());
					if (!matcher.matches())
						throw new TextFileParseException(String.format(
								"Invalid value for entry '%s' in section '%s': %s", entryProcedure.getName(),
								claimLinesSection.getName(), entryProcedure.getValue()));

					eobItem.getService().setSystem(CODING_SYSTEM_ICD9_PROC);
					eobItem.getService().setCode(matcher.group(1));
					eobItem.getService().setDisplay(matcher.group(2));
				}

				if (entryModifier1 != null && !entryModifier1.getValue().isEmpty()) {
					eobItem.addModifier().setCode(entryModifier1.getValue());
					// TODO set system & code
				}

				if (entryModifier2 != null && !entryModifier2.getValue().isEmpty()) {
					eobItem.addModifier().setCode(entryModifier2.getValue());
					// TODO set system & code
				}

				if (entryModifier3 != null && !entryModifier3.getValue().isEmpty()) {
					eobItem.addModifier().setCode(entryModifier3.getValue());
					// TODO set system & code
				}

				if (entryModifier4 != null && !entryModifier4.getValue().isEmpty()) {
					eobItem.addModifier().setCode(entryModifier4.getValue());
					// TODO set system & code
				}

				if (entryQuantity != null && !entryQuantity.getValue().isEmpty()) {
					eobItem.getQuantity().setValue(new BigDecimal(entryQuantity.getValue()));
				}

				if (hasValue(entrySubmitted)) {
					Matcher matcher = MONEY.matcher(entrySubmitted.getValue());
					if (!matcher.matches())
						throw new TextFileParseException(String.format(
								"Invalid value for entry '%s' in section '%s': %s", entrySubmitted.getName(),
								claimLinesSection.getName(), entrySubmitted.getValue()));

					eobItem.getNet().setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
							.setValue(new BigDecimal(matcher.group(1)));
				}

				if (hasValue(entryAllowed)) {
					Matcher matcher = MONEY.matcher(entryAllowed.getValue());
					if (!matcher.matches())
						throw new TextFileParseException(
								String.format("Invalid value for entry '%s' in section '%s': %s",
										entryAllowed.getName(), claimLinesSection.getName(), entryAllowed.getValue()));

					eobItem.addAdjudication().setCategory(new Coding().setCode(ADJUDICATION_ALLOWED)).getAmount()
							.setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
							.setValue(new BigDecimal(matcher.group(1)));
				}

				if (hasValue(entryUncovered)) {
					Matcher matcher = MONEY.matcher(entryUncovered.getValue());
					if (!matcher.matches())
						throw new TextFileParseException(String.format(
								"Invalid value for entry '%s' in section '%s': %s", entryUncovered.getName(),
								claimLinesSection.getName(), entryUncovered.getValue()));

					eobItem.addAdjudication().setCategory(new Coding().setCode(ADJUDICATION_NON_COVERED)).getAmount()
							.setSystem(CODING_SYSTEM_MONEY).setCode(CODING_SYSTEM_MONEY_US)
							.setValue(new BigDecimal(matcher.group(1)));
				}

				if (entryPlace != null && !entryPlace.getValue().isEmpty()) {
					eobItem.getPlace().setDisplay(entryPlace.getValue());
					// TODO what coding system is this?
				}

				if (entryType != null && !entryType.getValue().isEmpty()) {
					eobItem.getType().setDisplay(entryType.getValue());
					// TODO what coding system is this?
				}
			}

			/*
			 * Bump section index by 2, as that's how many sections were
			 * processed here.
			 */
			sectionIndex += 2;
		} while (sectionIndex < inputData.getSections().size());

		return resources;
	}

	/**
	 * @param entry
	 *            the {@link Entry} to check
	 * @return <code>true</code> if the specified {@link Entry}'s
	 *         {@link Entry#getValue()} property is not <code>null</code>, an
	 *         empty {@link String}, and does not match any of the
	 *         {@link #VALUES_TO_IGNORE} entries
	 */
	private static boolean hasValue(Entry entry) {
		if (entry == null)
			return false;
		if (entry.getValue() == null)
			return false;
		if (entry.getValue().trim().isEmpty())
			return false;
		if (Arrays.stream(VALUES_TO_IGNORE).anyMatch(v -> v.equals(entry.getValue())))
			return false;

		return true;
	}

	/**
	 * @param section
	 *            the Section to get the {@link Entry} from
	 * @param entryName
	 *            the {@link Entry#getName()} value of the {@link Entry} to get
	 *            the {@link Entry#getValue()} of
	 * @return the {@link Entry#getValue()} value for the specified
	 *         {@link Entry} in the specified {@link Section}, or
	 *         <code>null</code> if no matching {@link Entry} was found or if
	 *         the matching {@link Entry}'s value is an empty {@link String}
	 */
	private static String getStringOrNull(Section section, EntryName entryName) {
		Entry entry = section.getEntry(entryName);
		if (entry == null)
			return null;
		if (entry.getValue() == null)
			return null;
		if (entry.getValue().trim().isEmpty())
			return null;
		return entry.getValue().trim();
	}

	/**
	 * @param section
	 *            the Section to get the {@link Entry} from
	 * @param entryName
	 *            the {@link Entry#getName()} value of the {@link Entry} to get
	 *            the {@link Entry#getValue()} of
	 * @return the {@link Entry#getValue()} value for the specified
	 *         {@link Entry} in the specified {@link Section}
	 * @throws TextFileParseException
	 *             A {@link TextFileParseException} will be thrown if no
	 *             matching {@link Entry} was found or if the matching
	 *             {@link Entry}'s value is an empty {@link String}.
	 */
	private static String getStringOrThrow(Section section, EntryName entryName) throws TextFileParseException {
		Entry entry = section.getEntry(entryName);
		if (entry == null)
			throw new TextFileParseException(String.format("Unable to find entry '%s' in section '%s'.",
					entryName.getName(), section.getName()));
		if (entry.getValue() == null)
			throw new TextFileParseException(
					String.format("Entry '%s' in section '%s' has no value.", entryName.getName(), section.getName()));
		if (entry.getValue().trim().isEmpty())
			throw new TextFileParseException(
					String.format("Entry '%s' in section '%s' has no value.", entryName.getName(), section.getName()));
		return entry.getValue().trim();
	}

	/**
	 * @param section
	 *            the {@link Section} to get the {@link Entry} from
	 * @param entryIndex
	 *            the index of the {@link Entry} to get in
	 *            {@link Section#getEntries()}
	 * @param entryName
	 *            the {@link EntryName} that must match
	 * @return the {@link Entry} at the specified index in the specified
	 *         {@link Section}, as long as the specified {@link EntryName}
	 *         matches
	 * @throws TextFileParseException
	 *             A {@link TextFileParseException} will be thrown if the
	 *             specified {@link Entry} either doesn't exist or doesn't match
	 *             the specified {@link EntryName}.
	 */
	private static Entry getEntry(Section section, int entryIndex, EntryName entryName) throws TextFileParseException {
		if (section.getEntries().size() <= entryIndex)
			throw new TextFileParseException(String.format("No entry '%s' at index '%d' in section '%s'.",
					entryName.getName(), entryIndex, section.getName()));

		Entry entry = section.getEntries().get(entryIndex);
		if (!entry.getName().equals(entryName.getName()))
			throw new TextFileParseException(String.format("No entry '%s' at index '%d' in section '%s'.",
					entryName.getName(), entryIndex, section.getName()));

		return entry;
	}

	/**
	 * Mutable class that is used to model and reference the results of a
	 * {@link TextFileTransformer#transform(TextFile)} operation, as it
	 * progresses.
	 */
	private static final class TransformationResults {
		private final List<IBaseResource> resources;

		/**
		 * Constructs a new {@link TransformationResults} instance.
		 */
		public TransformationResults() {
			this.resources = new ArrayList<>();
		}

		/**
		 * @param resources
		 *            the {@link IBaseResource}(s) to add to this
		 *            {@link TransformationResults}
		 */
		public void addResources(IBaseResource... resources) {
			for (IBaseResource resource : resources)
				this.resources.add(resource);
		}

		/**
		 * @param resources
		 *            the {@link IBaseResource}(s) to add to this
		 *            {@link TransformationResults}
		 */
		public void addResources(Collection<IBaseResource> resources) {
			for (IBaseResource resource : resources)
				addResources(resource);
		}

		/**
		 * @return the {@link List} of {@link IBaseResource}s included in this
		 *         {@link TransformationResults} instance
		 */
		public List<IBaseResource> getResources() {
			return Collections.unmodifiableList(resources);
		}

		/**
		 * @return a new {@link Reference} to the {@link Organization} in this
		 *         {@link TransformationResults} that represents CMS
		 */
		public Reference createCmsOrganizationReference() {
			Organization cmsOrg = (Organization) resources.stream().filter(r -> r.getClass().equals(Organization.class))
					.filter(r -> ORG_NAME_CMS.equals(((Organization) r).getName())).findFirst().get();
			Reference cmsOrgRef = new Reference();
			cmsOrgRef.setReference(cmsOrg.getId());
			return cmsOrgRef;
		}

		/**
		 * @return a new {@link Reference} to the primary {@link Patient} in
		 *         this {@link TransformationResults}
		 */
		public Reference createPatientReference() {
			Patient patient = (Patient) resources.stream().filter(r -> r.getClass().equals(Patient.class)).findFirst()
					.get();
			Reference patientRef = new Reference();
			patientRef.setReference(patient.getId());
			return patientRef;
		}

		/**
		 * @return a new {@link Reference} to the "Part B" {@link Coverage} in
		 *         this {@link TransformationResults}
		 */
		public Reference createCoveragePartBReference() {
			Coverage partB = (Coverage) resources.stream().filter(r -> r.getClass().equals(Coverage.class))
					.filter(r -> PART_B_PLAN_NAME.equals(((Coverage) r).getPlan())).findFirst().get();
			Reference partBRef = new Reference();
			partBRef.setReference(partB.getId());
			return partBRef;
		}
	}
}
