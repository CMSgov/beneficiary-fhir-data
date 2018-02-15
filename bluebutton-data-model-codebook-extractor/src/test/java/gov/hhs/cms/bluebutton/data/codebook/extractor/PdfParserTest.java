package gov.hhs.cms.bluebutton.data.codebook.extractor;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.hhs.cms.bluebutton.data.codebook.model.Codebook;
import gov.hhs.cms.bluebutton.data.codebook.model.Value;
import gov.hhs.cms.bluebutton.data.codebook.model.Variable;
import gov.hhs.cms.bluebutton.data.codebook.model.VariableType;

/**
 * Unit tests for {@link PdfParser}.
 */
public final class PdfParserTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(PdfParserTest.class);

	/**
	 * Tests {@link PdfParser#extractTextLinesFromPdf(InputStream)} against
	 * {@link SupportedCodebook#FFS_CLAIMS}.
	 * 
	 * @throws IOException
	 *             Indicates test error.
	 */
	@Test
	public void extractTextLinesFromPdf_ffsClaims() throws IOException {
		try (InputStream codebookPdfStream = SupportedCodebook.FFS_CLAIMS.getCodebookPdfInputStream();) {
			List<String> codebookTextLines = PdfParser.extractTextLinesFromPdf(codebookPdfStream);

			/*
			 * This should be left disabled, except when needed for development/debugging
			 * purposes.
			 */
			// writeTextToTempFile(codebookTextLines);

			Assert.assertNotNull(codebookTextLines);
			Assert.assertTrue(codebookTextLines.size() > 100);
		}
	}

	/**
	 * Tests {@link PdfParser#findVariableSections(List)} against all
	 * {@link SupportedCodebook}s.
	 * 
	 * @throws IOException
	 *             Indicates test error.
	 */
	@Test
	public void findVariableSections() throws IOException {
		for (SupportedCodebook supportedCodebook : SupportedCodebook.values()) {
			try (InputStream codebookPdfStream = supportedCodebook.getCodebookPdfInputStream();) {
				LOGGER.info("Looking for sections in codebook: {}", supportedCodebook.name());

				/*
				 * Note: We leave the printXXX(...) calls here disabled unless/until they're
				 * needed to debug a specific problem, as they add a ton of log noise.
				 */

				List<String> codebookTextLines = PdfParser.extractTextLinesFromPdf(codebookPdfStream);
				printTextLinesToConsole(codebookTextLines);
				List<List<String>> variableSections = PdfParser.findVariableSections(codebookTextLines);
				printSectionsToConsole(variableSections);

				for (List<String> variableSection : variableSections) {
					Assert.assertNotNull(variableSection);
					Assert.assertTrue(variableSection.size() >= 10);
				}

				/*
				 * How else can you verify that the section splitting code worked correctly?
				 * Pick a one-line field that should have a unique value in each section, find
				 * all instances of that field in the un-grouped lines, then make sure that each
				 * one of those unique field lines can be found in a section.
				 */
				Predicate<? super String> searchFieldFilter = l -> l.startsWith("SHORT_NAME:");
				List<String> searchFieldLines = codebookTextLines.stream().filter(searchFieldFilter)
						.collect(Collectors.toList());

				// If this fails, we need to pick a different search field.
				Assert.assertEquals("Not all instances of that field are unique.", searchFieldLines.size(),
						new HashSet<>(searchFieldLines).size());

				for (String searchFieldLine : searchFieldLines) {
					boolean foundSection = false;
					for (List<String> variableSection : variableSections) {
						for (String line : variableSection)
							if (searchFieldLine.equals(line))
								foundSection = true;
					}
					Assert.assertTrue(String.format("Can't find search field line: '%s'", searchFieldLine),
							foundSection);
				}
			}
		}
	}

	/**
	 * Tests {@link PdfParser#parseCodebookPdf(InputStream)} against all
	 * {@link SupportedCodebook}s.
	 * 
	 * @throws IOException
	 *             Indicates test error.
	 */
	@Test
	public void parseCodebookPdf() throws IOException {
		for (SupportedCodebook supportedCodebook : SupportedCodebook.values()) {
			LOGGER.info("Looking for sections in codebook: {}", supportedCodebook.name());

			Codebook codebook = PdfParser.parseCodebookPdf(supportedCodebook);

			/*
			 * Since this test runs against all of the PDFs, it's mostly just a
			 * "make sure things don't blow up" test case. Which is fine! But we can still
			 * verify some basic facts about the results.
			 */

			Assert.assertNotNull(codebook);
			// Note: The 2017-05 version of the PDE codebook has 56 variables.
			Assert.assertTrue("Not as many variables as expected: " + codebook.getVariables().size(),
					codebook.getVariables().size() > 50);

			for (Variable variable : codebook.getVariables()) {
				assertVariableIsValid(variable);
			}
		}
	}

	/**
	 * Tests {@link PdfParser#parseCodebookPdf(InputStream)} against
	 * {@link SupportedCodebook#BENEFICIARY_SUMMARY} for the <code>DUAL_MO</code>
	 * variable.
	 * 
	 * @throws IOException
	 *             Indicates test error.
	 */
	@Test
	public void parseCodebookPdf_DUAL_MO() throws IOException {
		/*
		 * Why are we spot checking this one variable's parsed output? Because it's
		 * somewhat interesting: it has multiple paragraphs in its COMMENT field, and
		 * has a somewhat unusual valueFormat.
		 */

		Codebook codebook = PdfParser.parseCodebookPdf(SupportedCodebook.BENEFICIARY_SUMMARY);

		Variable variable = codebook.getVariables().stream().filter(v -> v.getId().equals("DUAL_MO")).findAny().get();

		String expectedDescription1 = "This variable is the number of months during the year that the beneficiary"
				+ " was dually eligible (i.e., he/she was also eligible for Medicaid benefits).";
		String expectedComment1 = "CCW derived this variable by counting the number of months where the"
				+ " beneficiary had dual eligibility (DUAL_STUS_CD_XX not equal to '00' or '**').";
		String expectedComment2 = "There are different ways to classify dually eligible beneficiaries - in terms of"
				+ " whether he/she is enrolled in full or partial benefits. Additional information regarding"
				+ " various ways to identify dually enrolled populations, refer to a CCW Technical Guidance"
				+ " document entitled: \"Options in Determining Dual Eligibles\"";

		Assert.assertEquals("Months of Dual Eligibility", variable.getLabel());
		assertParagraphsEquals(Arrays.asList(expectedDescription1), variable.getDescription());
		Assert.assertEquals("DUAL_MO", variable.getShortName());
		Assert.assertEquals("DUAL_ELGBL_MONS", variable.getLongName());
		Assert.assertEquals(VariableType.NUM, variable.getType());
		Assert.assertEquals(new Integer(3), variable.getLength());
		Assert.assertEquals("CMS Common Medicare Environment (CME) (derived)", variable.getSource());
		Assert.assertEquals("The value in this field is between '00' through '12'.", variable.getValueFormat());
		Assert.assertNull(variable.getValueGroups());
		assertParagraphsEquals(Arrays.asList(expectedComment1, expectedComment2), variable.getComment());
	}

	/**
	 * Tests {@link PdfParser#parseCodebookPdf(InputStream)} against
	 * {@link SupportedCodebook#FFS_CLAIMS} for the <code>DSH_OP_CLM_VAL_AMT</code>
	 * variable.
	 * 
	 * @throws IOException
	 *             Indicates test error.
	 */
	@Test
	public void parseCodebookPdf_DSH_OP_CLM_VAL_AMT() throws IOException {
		/*
		 * Why are we spot checking this one variable's parsed output? Because it's
		 * somewhat interesting: 1) it has multiple paragraphs in its DESCRIPTION and
		 * COMMENT fields, 2) it's an example of a simple valueFormat, 3) its COMMENT
		 * has a line that looks like a new field, but isn't, and 4) its COMMENT also
		 * has a long URL that line breaks with a hyphen.
		 */

		Codebook codebook = PdfParser.parseCodebookPdf(SupportedCodebook.FFS_CLAIMS);

		Variable variable = codebook.getVariables().stream().filter(v -> v.getId().equals("DSH_OP_CLM_VAL_AMT"))
				.findAny().get();

		String expectedDescription1 = "This is one component of the total amount that is payable on prospective"
				+ " payment system (PPS) claims, and reflects the DSH (disproportionate share hospital) payments"
				+ " for operating expenses (such as labor) for the claim.";
		String expectedDescription2 = "There are two types of DSH amounts that may be payable for many PPS claims;"
				+ " the other type of DSH payment is for the DSH capital amount (variable called"
				+ " CLM_PPS_CPTL_DSPRPRTNT_SHR_AMT).";
		String expectedDescription3 = "Both operating and capital DSH payments are components of the PPS, as well"
				+ " as numerous other factors.";
		String expectedComment1 = "Medicare payments are described in detail in a series of Medicare Payment"
				+ " Advisory Commission (MedPAC) documents called “Payment Basics” (see:"
				+ " http://www.medpac.gov/payment_basics.cfm).";
		String expectedComment2 = "Also in the Medicare Learning Network (MLN) “Payment System Fact Sheet Series”"
				+ " (see: "
				+ "http://www.cms.gov/Outreach-and-Education/Medicare-Learning-Network-MLN/MLNProducts/MLN-Publications.html"
				+ ").";
		String expectedComment3 = "DERIVATION RULES: If there is a value code '18' (i.e., in the Value Code File, if the"
				+ " VAL_CD='18') then this dollar amount (VAL_AMT) is used to populate this field.\"";

		Assert.assertEquals("Operating Disproportionate Share (DSH) Amount", variable.getLabel());
		assertParagraphsEquals(Arrays.asList(expectedDescription1, expectedDescription2, expectedDescription3),
				variable.getDescription());
		Assert.assertEquals("DSH_OP", variable.getShortName());
		Assert.assertEquals("DSH_OP_CLM_VAL_AMT", variable.getLongName());
		Assert.assertEquals(VariableType.NUM, variable.getType());
		Assert.assertEquals(new Integer(12), variable.getLength());
		Assert.assertEquals("NCH", variable.getSource());
		Assert.assertEquals("XXX.XX", variable.getValueFormat());
		Assert.assertNull(variable.getValueGroups());
		assertParagraphsEquals(Arrays.asList(expectedComment1, expectedComment2, expectedComment3),
				variable.getComment());
	}

	/**
	 * Tests {@link PdfParser#parseCodebookPdf(InputStream)} against
	 * {@link SupportedCodebook#FFS_CLAIMS} for the
	 * <code>CARR_LINE_PRVDR_TYPE_CD</code> variable.
	 * 
	 * @throws IOException
	 *             Indicates test error.
	 */
	@Test
	public void parseCodebookPdf_CARR_LINE_PRVDR_TYPE_CD() throws IOException {
		/*
		 * Why are we spot checking this one variable's parsed output? Because it's
		 * somewhat interesting: 1) it has multiple valueGroups, 2) many of its values
		 * have multiple lines, 3) many of its coded values are duplicated, 4) it has a
		 * COMMENT that's just "-".
		 */

		Codebook codebook = PdfParser.parseCodebookPdf(SupportedCodebook.FFS_CLAIMS);
		Variable variable = codebook.getVariables().stream().filter(v -> v.getId().equals("CARR_LINE_PRVDR_TYPE_CD"))
				.findAny().get();

		String expectedDescription1 = "Code identifying the type of provider furnishing the service for this line"
				+ " item on the carrier claim.";

		Assert.assertEquals("Carrier Line Provider Type Code", variable.getLabel());
		assertParagraphsEquals(Arrays.asList(expectedDescription1), variable.getDescription());
		Assert.assertEquals("PRV_TYPE", variable.getShortName());
		Assert.assertEquals("CARR_LINE_PRVDR_TYPE_CD", variable.getLongName());
		Assert.assertEquals(VariableType.CHAR, variable.getType());
		Assert.assertEquals(new Integer(1), variable.getLength());
		Assert.assertEquals("NCH", variable.getSource());
		Assert.assertNull(variable.getValueFormat());
		Assert.assertEquals(2, variable.getValueGroups().size());
		Assert.assertEquals(8, variable.getValueGroups().get(0).getValues().size());
		assertParagraphsEquals(Arrays.asList("For Physician/Supplier Claims:"),
				variable.getValueGroups().get(0).getDescription());
		Assert.assertEquals(9, variable.getValueGroups().get(1).getValues().size());
		assertParagraphsEquals(Arrays.asList(
				"NOTE: PRIOR TO VERSION H, DME claims also used this code; the" + " following were valid code VALUES:"),
				variable.getValueGroups().get(1).getDescription());

		// Spot-check some of the values:
		Value value_0_3 = variable.getValueGroups().get(0).getValues().get(3);
		Assert.assertEquals("3", value_0_3.getCode());
		Assert.assertEquals("Institutional provider", value_0_3.getDescription());
		Value value_1_8 = variable.getValueGroups().get(1).getValues().get(8);
		Assert.assertEquals("8", value_1_8.getCode());
		Assert.assertEquals("Other entities for whom EI numbers are used in coding the ID field or proprietorship"
				+ " for whom EI numbers are used in coding the ID field.", value_1_8.getDescription());

		Assert.assertNull(variable.getComment());
	}

	/**
	 * Verifies that {@link PdfParser#parseLabel(List)} works as expected on labels
	 * with more than one line.
	 */
	@Test
	public void parseLabel_multiline() {
		String[] variableSection = new String[] { "CLM_NEXT_GNRTN_ACO_IND_CD1",
				"LABEL: Claim Next Generation (NG) Accountable Care Organization (ACO) Indicator Code –",
				"Population-Based Payment (PBP)",
				"DESCRIPTION: The field identifies the claims that qualify for specific claims processing edits"
						+ " related to", };

		String parsedLabel = PdfParser.parseLabel(Arrays.asList(variableSection));
		Assert.assertEquals(
				"Claim Next Generation (NG) Accountable Care Organization (ACO) Indicator Code – Population-Based"
						+ " Payment (PBP)",
				parsedLabel);
	}

	/**
	 * @param expectedParagraphs
	 *            the expected {@link List} of paragraphs
	 * @param actualParagraphs
	 *            the actual {@link List} of paragraphs to verify
	 */
	private void assertParagraphsEquals(List<String> expectedParagraphs, List<String> actualParagraphs) {
		if (expectedParagraphs != null)
			Assert.assertNotNull(actualParagraphs);
		else
			Assert.assertNull(actualParagraphs);
		Assert.assertEquals("Paragraph count mismatch.", expectedParagraphs.size(), actualParagraphs.size());

		for (int pIndex = 0; pIndex < expectedParagraphs.size(); pIndex++) {
			Assert.assertEquals(expectedParagraphs.get(pIndex), actualParagraphs.get(pIndex));
		}
	}

	/**
	 * Prints the specified text out to {@link System#out} for development/debugging
	 * purposes.
	 * 
	 * @param variableSections
	 *            the variable sections to print out
	 */
	@SuppressWarnings("unused")
	private static void printSectionsToConsole(List<List<String>> variableSections) {
		System.out.printf("\n# Codebook Variable Sections (%d count)\n====\n", variableSections.size());
		for (List<String> variableSection : variableSections) {
			System.out.printf("\n## Section (%d lines)\n----\n", variableSection.size());
			for (String sectionLine : variableSection)
				System.out.println(sectionLine);
		}
	}

	/**
	 * Prints the specified text out to {@link System#out}, for
	 * development/debugging purposes.
	 * 
	 * @param textLines
	 *            the lines of text to print out (each line will have a linebreak
	 *            appended to it)
	 */
	@SuppressWarnings("unused")
	private static void printTextLinesToConsole(List<String> textLines) {
		for (String codebookTextLine : textLines) {
			System.out.println(codebookTextLine);
		}
	}

	/**
	 * Writes the specified text out to a new randomly-named file, for
	 * development/debugging purposes. The name of the new file will be written out
	 * via {@link #LOGGER}.
	 * 
	 * @param textLines
	 *            the lines of text to write out (each line will have a linebreak
	 *            appended to it)
	 */
	@SuppressWarnings("unused")
	private static void writeTextToTempFile(List<String> textLines) {
		Path codebookTextFilePath;
		try {
			codebookTextFilePath = Files.createTempFile("codebook", ".txt");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		try (FileWriter codebookTextFileWriter = new FileWriter(codebookTextFilePath.toFile());) {
			for (String codebookTextLine : textLines) {
				codebookTextFileWriter.write(codebookTextLine);
				codebookTextFileWriter.append('\n');
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		LOGGER.info("Intermediate text extracted from PDF written to file: '{}'",
				codebookTextFilePath.toAbsolutePath());
	}

	/**
	 * Asserts that the specified {@link Variable} is valid (inasmuch as we can
	 * check that in a generic fashion).
	 * 
	 * @param variable
	 *            the {@link Variable} to validate
	 */
	private static void assertVariableIsValid(Variable variable) {
		Assert.assertNotNull(variable);

		// getId() should always be present.
		Assert.assertTrue(variable.getId() != null && !variable.getId().isEmpty());

		String assertionMessage = String.format("Invalid parse result for variable '%s'.", variable.getId());

		// getDescription() should always be present.
		Assert.assertTrue(assertionMessage, variable.getDescription() != null && !variable.getDescription().isEmpty());
		for (String descriptionParagraph : variable.getDescription())
			// Shouldn't be any empty paragraphs.
			Assert.assertTrue(assertionMessage, descriptionParagraph != null && !descriptionParagraph.isEmpty());

		// getShortName() should either be null or have content.
		Assert.assertTrue(assertionMessage, variable.getShortName() == null || !variable.getShortName().isEmpty());

		// getLongName() should always be present.
		Assert.assertTrue(assertionMessage, variable.getLongName() != null && !variable.getLongName().isEmpty());

		// Note: getLongName() is _usually_ the same as getId(), but not always.

		// Note: getType() isn't always present.

		// getLength() should always be present and valid.
		Assert.assertTrue(assertionMessage, variable.getLength() != null && variable.getLength() > 0);

		// getSource() should either be null or have content.
		Assert.assertTrue(assertionMessage, variable.getSource() == null || !variable.getSource().isEmpty());

		// getValueFormat() should either be null or have content.
		Assert.assertTrue(assertionMessage, variable.getValueFormat() == null || !variable.getValueFormat().isEmpty());

		// getValueGroups() should either be null or have content.
		Assert.assertTrue(assertionMessage, variable.getValueGroups() == null || !variable.getValueGroups().isEmpty());

		// getComment() should either be null or have content.
		Assert.assertTrue(assertionMessage, variable.getComment() == null || !variable.getComment().isEmpty());
	}
}
