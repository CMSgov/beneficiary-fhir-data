package gov.cms.bfd.model.codebook.extractor;

import static gov.cms.bfd.model.codebook.extractor.PdfParser.openCodebookPdfInputStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.model.codebook.model.Codebook;
import gov.cms.bfd.model.codebook.model.SupportedCodebook;
import gov.cms.bfd.model.codebook.model.Value;
import gov.cms.bfd.model.codebook.model.ValueGroup;
import gov.cms.bfd.model.codebook.model.Variable;
import gov.cms.bfd.model.codebook.model.VariableType;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Unit tests for {@link gov.cms.bfd.model.codebook.extractor.PdfParser}. */
public final class PdfParserTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(PdfParserTest.class);

  /**
   * The {@link PdfParser} instance used by tests. Class is immutable so all tests can use the same
   * instance.
   */
  private static final PdfParser parser = new PdfParser(false);

  /**
   * Parses all of the {@link SupportedCodebook}s using {@link
   * gov.cms.bfd.model.codebook.extractor.PdfParser}, looking for duplicate {@link Variable}s.
   *
   * @throws IOException Indicates test error.
   */
  @Test
  public void findDuplicateVariables() throws IOException {
    Map<String, List<Variable>> variablesById = new LinkedHashMap<>();

    // Build the map of Variable IDs to the Codebooks they're seen in.
    for (SupportedCodebook supportedCodebook : SupportedCodebook.values()) {
      Codebook codebook = parser.parseCodebookPdf(supportedCodebook);
      for (Variable variable : codebook.getVariables()) {
        if (!variablesById.containsKey(variable.getId()))
          variablesById.put(variable.getId(), new ArrayList<>());

        variablesById.get(variable.getId()).add(variable);
      }
    }

    // Find all of the variables that appear in more than one Codebook.
    List<String> duplicatedVariableIds = new ArrayList<>();
    for (String variableId : variablesById.keySet()) {
      List<Variable> variables = variablesById.get(variableId);
      if (variables.size() > 1) duplicatedVariableIds.add(variableId);
    }

    // Log a detailed warning for each duplicate.
    for (String duplicatedVariableId : duplicatedVariableIds) {
      List<Variable> duplicatedVariables = variablesById.get(duplicatedVariableId);
      LOGGER.warn(
          "The variable '{}' appears more than once: {}.",
          duplicatedVariableId,
          duplicatedVariables);
    }

    /*
     * We know that these variables are duplicated. It isn't great, but oh well. We
     * assert their presence just to get alerted if something interesting changes
     * with the data or parsing.
     */
    assertTrue(duplicatedVariableIds.contains("BENE_ID"));
    assertTrue(duplicatedVariableIds.contains("DOB_DT"));
    assertTrue(duplicatedVariableIds.contains("GNDR_CD"));

    // Blow up if anything more than those known problems appears.
    assertEquals(87, duplicatedVariableIds.size());
  }

  /**
   * Parses all of the {@link SupportedCodebook}s using {@link
   * gov.cms.bfd.model.codebook.extractor.PdfParser}, looking for duplicate {@link Value#getCode()}s
   * within each {@link Variable}.
   */
  @Test
  public void findDuplicateCodes() {
    for (SupportedCodebook supportedCodebook : SupportedCodebook.values()) {
      Codebook codebook = parser.parseCodebookPdf(supportedCodebook);

      String version = codebook.getVersion();
      for (Variable variable : codebook.getVariables()) {
        if (!variable.getValueGroups().isPresent()) continue;

        // Build a multimap of all the Values by their codes.
        Map<String, List<Value>> valuesByCode = new LinkedHashMap<>();
        for (ValueGroup valueGroup : variable.getValueGroups().get()) {
          for (Value value : valueGroup.getValues()) {
            if (!valuesByCode.containsKey(value.getCode()))
              valuesByCode.put(value.getCode(), new ArrayList<>());

            valuesByCode.get(value.getCode()).add(value);
          }
        }

        // Find all of the codes that appear in more than one Value.
        List<String> duplicatedCodes = new ArrayList<>();
        for (String code : valuesByCode.keySet()) {
          List<Value> values = valuesByCode.get(code);
          if (values.size() > 1) duplicatedCodes.add(code);
        }

        // Log a detailed warning for each duplicate.
        for (String duplicatedCode : duplicatedCodes) {
          List<Value> duplicatedValues = valuesByCode.get(duplicatedCode);
          assertEquals(duplicatedValues.get(0).getCode(), duplicatedValues.get(1).getCode());
          LOGGER.warn(
              "The code '{}' appears more than once in Variable '{}': {}.",
              duplicatedCode,
              variable,
              duplicatedValues);
        }
      }
    }
  }

  /**
   * Tests {@link
   * gov.cms.bfd.model.codebook.extractor.PdfParser#extractTextLinesFromPdf(InputStream)} against
   * {@link SupportedCodebook#FFS_CLAIMS}.
   *
   * @throws IOException Indicates test error.
   */
  @Test
  public void extractTextLinesFromPdf_ffsClaims() throws IOException {
    try (InputStream codebookPdfStream = openCodebookPdfInputStream(SupportedCodebook.FFS_CLAIMS)) {
      List<String> codebookTextLines = parser.extractTextLinesFromPdf(codebookPdfStream);

      /*
       * This should be left disabled, except when needed for development/debugging
       * purposes.
       */
      // writeTextToTempFile(codebookTextLines);

      assertNotNull(codebookTextLines);
      assertTrue(codebookTextLines.size() > 100);
    }
  }

  /**
   * Tests {@link gov.cms.bfd.model.codebook.extractor.PdfParser#findVariableSections(List)} against
   * all {@link SupportedCodebook}s.
   *
   * @throws IOException Indicates test error.
   */
  @Test
  public void findVariableSections() throws IOException {
    for (SupportedCodebook supportedCodebook : SupportedCodebook.values()) {
      try (InputStream codebookPdfStream = openCodebookPdfInputStream(supportedCodebook)) {
        LOGGER.info("Looking for sections in codebook: {}", supportedCodebook.name());

        /*
         * Note: We leave the printXXX(...) calls here disabled unless/until they're
         * needed to debug a specific problem, as they add a ton of log noise.
         */

        List<String> codebookTextLines = parser.extractTextLinesFromPdf(codebookPdfStream);
        // printTextLinesToConsole(codebookTextLines);
        List<List<String>> variableSections = parser.findVariableSections(codebookTextLines);
        // printSectionsToConsole(variableSections);

        for (List<String> variableSection : variableSections) {
          assertNotNull(variableSection);
          assertTrue(variableSection.size() >= 10);
        }

        /*
         * How else can you verify that the section splitting code worked correctly?
         * Pick a one-line field that should have a unique value in each section, find
         * all instances of that field in the un-grouped lines, then make sure that each
         * one of those unique field lines can be found in a section.
         */
        Predicate<? super String> searchFieldFilter = l -> l.startsWith("SHORT_NAME:");
        List<String> searchFieldLines =
            codebookTextLines.stream().filter(searchFieldFilter).collect(Collectors.toList());

        // If this fails, we need to pick a different search field.
        assertEquals(
            searchFieldLines.size(),
            new HashSet<>(searchFieldLines).size(),
            "Not all instances of that field are unique.");

        for (String searchFieldLine : searchFieldLines) {
          boolean foundSection = false;
          for (List<String> variableSection : variableSections) {
            for (String line : variableSection)
              if (searchFieldLine.equals(line)) foundSection = true;
          }
          assertTrue(
              foundSection, String.format("Can't find search field line: '%s'", searchFieldLine));
        }
      }
    }
  }

  /**
   * Tests {@link
   * gov.cms.bfd.model.codebook.extractor.PdfParser#parseCodebookPdf(SupportedCodebook)} against all
   * {@link SupportedCodebook}s.
   *
   * @throws IOException Indicates test error.
   */
  @Test
  public void parseCodebookPdf() throws IOException {
    for (SupportedCodebook supportedCodebook : SupportedCodebook.values()) {
      LOGGER.info("Looking for sections in codebook: {}", supportedCodebook.name());

      Codebook codebook = parser.parseCodebookPdf(supportedCodebook);

      /*
       * Since this test runs against all of the PDFs, it's mostly just a
       * "make sure things don't blow up" test case. Which is fine! But we can still
       * verify some basic facts about the results.
       */

      assertNotNull(codebook);
      // Note: The 2017-05 version of the PDE codebook has 56 variables.
      assertTrue(
          codebook.getVariables().size() > 50,
          "Not as many variables as expected: " + codebook.getVariables().size());

      for (Variable variable : codebook.getVariables()) {
        assertVariableIsValid(variable);
      }
    }
  }

  /**
   * Tests {@link
   * gov.cms.bfd.model.codebook.extractor.PdfParser#parseCodebookPdf(SupportedCodebook)} against
   * {@link SupportedCodebook#BENEFICIARY_SUMMARY} for the <code>DUAL_MO</code> variable.
   *
   * @throws IOException Indicates test error.
   */
  @Test
  public void parseCodebookPdf_DUAL_MO() throws IOException {
    /*
     * Why are we spot checking this one variable's parsed output? Because it's
     * somewhat interesting: it has multiple paragraphs in its COMMENT field, and
     * has a somewhat unusual valueFormat.
     */

    Codebook codebook = parser.parseCodebookPdf(SupportedCodebook.BENEFICIARY_SUMMARY);

    Variable variable =
        codebook.getVariables().stream().filter(v -> v.getId().equals("DUAL_MO")).findAny().get();

    String expectedDescription1 =
        "This variable is the number of months during the year that the beneficiary"
            + " was dually eligible (i.e., he/she was also eligible for Medicaid benefits).";
    String expectedComment1 =
        "CCW derived this variable by counting the number of months where the beneficiary"
            + " had dual eligibility (DUAL_STUS_CD_XX not equal to '00' or '**'). There are different ways to"
            + " classify dually eligible beneficiaries - in terms of whether he/she is enrolled in full or"
            + " partial benefits. Additional information regarding various ways to identify dually enrolled"
            + " populations, refer to a CCW Technical Guidance document entitled: \"Options in Determining Dual"
            + " Eligibles\"";

    assertEquals("Months of Dual Eligibility", variable.getLabel());
    assertParagraphsEquals(Arrays.asList(expectedDescription1), variable.getDescription());
    assertEquals("DUAL_MO", variable.getShortName().get());
    assertEquals("DUAL_ELGBL_MOS_NUM", variable.getLongName());
    assertEquals(VariableType.CHAR, variable.getType().get());
    assertEquals(Integer.valueOf(2), variable.getLength());
    assertEquals("CMS Enrollment Database (EDB) (derived)", variable.getSource().get());
    assertEquals(
        "The value in this field is between '00' through '12'.", variable.getValueFormat().get());
    assertFalse(variable.getValueGroups().isPresent());
    assertParagraphsEquals(Arrays.asList(expectedComment1), variable.getComment());
  }

  /**
   * Tests {@link
   * gov.cms.bfd.model.codebook.extractor.PdfParser#parseCodebookPdf(SupportedCodebook)}} against
   * {@link SupportedCodebook#FFS_CLAIMS} for the <code>
   * DSH_OP_CLM_VAL_AMT</code> variable.
   *
   * @throws IOException Indicates test error.
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

    Codebook codebook = parser.parseCodebookPdf(SupportedCodebook.FFS_CLAIMS);

    Variable variable =
        codebook.getVariables().stream()
            .filter(v -> v.getId().equals("DSH_OP_CLM_VAL_AMT"))
            .findAny()
            .get();

    String expectedDescription1 =
        "This is one component of the total amount that is payable on prospective"
            + " payment system (PPS) claims, and reflects the DSH (disproportionate share hospital) payments"
            + " for operating expenses (such as labor) for the claim.";
    String expectedDescription2 =
        "There are two types of DSH amounts that may be payable for many PPS claims;"
            + " the other type of DSH payment is for the DSH capital amount (variable called"
            + " CLM_PPS_CPTL_DSPRPRTNT_SHR_AMT).";
    String expectedDescription3 =
        "Both operating and capital DSH payments are components of the PPS, as well"
            + " as numerous other factors.";
    String expectedComment1 =
        "Medicare payments are described in detail in a series of Medicare Payment"
            + " Advisory Commission (MedPAC) documents called “Payment Basics” (see:"
            + " http://www.medpac.gov/payment_basics.cfm).";
    String expectedComment2 =
        "Also in the Medicare Learning Network (MLN) “Payment System Fact Sheet Series”"
            + " (see: "
            + "http://www.cms.gov/Outreach-and-Education/Medicare-Learning-Network-MLN/MLNProducts/MLN-Publications.html"
            + ").";
    String expectedComment3 =
        "DERIVATION RULES: If there is a value code '18' (i.e., in the Value Code File, if the"
            + " VAL_CD='18') then this dollar amount (VAL_AMT) is used to populate this field.\"";

    assertEquals("Operating Disproportionate Share (DSH) Amount", variable.getLabel());
    assertParagraphsEquals(
        Arrays.asList(expectedDescription1, expectedDescription2, expectedDescription3),
        variable.getDescription());
    assertEquals("DSH_OP", variable.getShortName().get());
    assertEquals("DSH_OP_CLM_VAL_AMT", variable.getLongName());
    assertEquals(VariableType.NUM, variable.getType().get());
    assertEquals(Integer.valueOf(12), variable.getLength());
    assertEquals("NCH", variable.getSource().get());
    assertEquals("XXX.XX", variable.getValueFormat().get());
    assertFalse(variable.getValueGroups().isPresent());
    assertParagraphsEquals(
        Arrays.asList(expectedComment1, expectedComment2, expectedComment3), variable.getComment());
  }

  /**
   * Tests {@link
   * gov.cms.bfd.model.codebook.extractor.PdfParser#parseCodebookPdf(SupportedCodebook)} against
   * {@link SupportedCodebook#FFS_CLAIMS} for the <code>
   * CARR_LINE_PRVDR_TYPE_CD</code> variable.
   *
   * @throws IOException Indicates test error.
   */
  @Test
  public void parseCodebookPdf_CARR_LINE_PRVDR_TYPE_CD() throws IOException {
    /*
     * Why are we spot checking this one variable's parsed output? Because it's
     * somewhat interesting: 1) it has multiple valueGroups, 2) many of its values
     * have multiple lines, 3) many of its coded values are duplicated, 4) it has a
     * COMMENT that's just "-".
     */

    Codebook codebook = parser.parseCodebookPdf(SupportedCodebook.FFS_CLAIMS);
    Variable variable =
        codebook.getVariables().stream()
            .filter(v -> v.getId().equals("CARR_LINE_PRVDR_TYPE_CD"))
            .findAny()
            .get();

    String expectedDescription1 =
        "Code identifying the type of provider furnishing the service for this line"
            + " item on the carrier claim.";

    assertEquals("Carrier Line Provider Type Code", variable.getLabel());
    assertParagraphsEquals(Arrays.asList(expectedDescription1), variable.getDescription());
    assertEquals("PRV_TYPE", variable.getShortName().get());
    assertEquals("CARR_LINE_PRVDR_TYPE_CD", variable.getLongName());
    assertEquals(VariableType.CHAR, variable.getType().get());
    assertEquals(Integer.valueOf(1), variable.getLength());
    assertEquals("NCH", variable.getSource().get());
    assertFalse(variable.getValueFormat().isPresent());
    assertEquals(2, variable.getValueGroups().get().size());
    assertEquals(8, variable.getValueGroups().get().get(0).getValues().size());
    assertParagraphsEquals(
        Arrays.asList("For Physician/Supplier Claims:"),
        variable.getValueGroups().get().get(0).getDescription());
    assertEquals(9, variable.getValueGroups().get().get(1).getValues().size());
    assertParagraphsEquals(
        Arrays.asList(
            "NOTE: PRIOR TO VERSION H, DME claims also used this code; the"
                + " following were valid code VALUES:"),
        variable.getValueGroups().get().get(1).getDescription());

    // Spot-check some of the values:
    Value value_0_3 = variable.getValueGroups().get().get(0).getValues().get(3);
    assertEquals("3", value_0_3.getCode());
    assertEquals("Institutional provider", value_0_3.getDescription());
    Value value_1_8 = variable.getValueGroups().get().get(1).getValues().get(8);
    assertEquals("8", value_1_8.getCode());
    assertEquals(
        "Other entities for whom EI numbers are used in coding the ID field or proprietorship"
            + " for whom EI numbers are used in coding the ID field.",
        value_1_8.getDescription());

    assertFalse(variable.getComment().isPresent());
  }

  /**
   * Verifies that {@link gov.cms.bfd.model.codebook.extractor.PdfParser#parseLabel(List)} works as
   * expected on labels with more than one line.
   */
  @Test
  public void parseLabel_multiline() {
    String[] variableSection =
        new String[] {
          "CLM_NEXT_GNRTN_ACO_IND_CD1",
          "LABEL: Claim Next Generation (NG) Accountable Care Organization (ACO) Indicator Code –",
          "Population-Based Payment (PBP)",
          "DESCRIPTION: The field identifies the claims that qualify for specific claims processing edits"
              + " related to",
        };

    String parsedLabel = parser.parseLabel(Arrays.asList(variableSection));
    assertEquals(
        "Claim Next Generation (NG) Accountable Care Organization (ACO) Indicator Code – Population-Based"
            + " Payment (PBP)",
        parsedLabel);
  }

  /**
   * Asserts that two paragraphs are equal.
   *
   * @param expectedParagraphs the expected {@link List} of paragraphs
   * @param actualParagraphs the actual {@link List} of paragraphs to verify
   */
  private static void assertParagraphsEquals(
      List<String> expectedParagraphs, Optional<List<String>> actualParagraphs) {
    assertEquals(expectedParagraphs != null, actualParagraphs.isPresent());
    if (actualParagraphs.isPresent())
      assertParagraphsEquals(expectedParagraphs, actualParagraphs.get());
  }

  /**
   * Asserts that two paragraphs are equal.
   *
   * @param expectedParagraphs the expected {@link List} of paragraphs
   * @param actualParagraphs the actual {@link List} of paragraphs to verify
   */
  private static void assertParagraphsEquals(
      List<String> expectedParagraphs, List<String> actualParagraphs) {
    if (expectedParagraphs != null) assertNotNull(actualParagraphs);
    else assertNull(actualParagraphs);
    assertEquals(expectedParagraphs.size(), actualParagraphs.size(), "Paragraph count mismatch.");

    for (int pIndex = 0; pIndex < expectedParagraphs.size(); pIndex++) {
      assertEquals(expectedParagraphs.get(pIndex), actualParagraphs.get(pIndex));
    }
  }

  /**
   * Prints the specified text out to {@link System#out} for development/debugging purposes.
   *
   * @param variableSections the variable sections to print out
   */
  @SuppressWarnings("unused")
  private static void printSectionsToConsole(List<List<String>> variableSections) {
    System.out.printf("\n# Codebook Variable Sections (%d count)\n====\n", variableSections.size());
    for (List<String> variableSection : variableSections) {
      System.out.printf("\n## Section (%d lines)\n----\n", variableSection.size());
      for (String sectionLine : variableSection) System.out.println(sectionLine);
    }
  }

  /**
   * Prints the specified text out to {@link System#out}, for development/debugging purposes.
   *
   * @param textLines the lines of text to print out (each line will have a linebreak appended to
   *     it)
   */
  @SuppressWarnings("unused")
  private static void printTextLinesToConsole(List<String> textLines) {
    for (String codebookTextLine : textLines) {
      System.out.println(codebookTextLine);
    }
  }

  /**
   * Writes the specified text out to a new randomly-named file, for development/debugging purposes.
   * The name of the new file will be written out via {@link #LOGGER}.
   *
   * @param textLines the lines of text to write out (each line will have a linebreak appended to
   *     it)
   */
  @SuppressWarnings("unused")
  private static void writeTextToTempFile(List<String> textLines) {
    Path codebookTextFilePath;
    try {
      codebookTextFilePath = Files.createTempFile("codebook", ".txt");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    try (FileWriter codebookTextFileWriter = new FileWriter(codebookTextFilePath.toFile()); ) {
      for (String codebookTextLine : textLines) {
        codebookTextFileWriter.write(codebookTextLine);
        codebookTextFileWriter.append('\n');
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    LOGGER.info(
        "Intermediate text extracted from PDF written to file: '{}'",
        codebookTextFilePath.toAbsolutePath());
  }

  /**
   * Asserts that the specified {@link Variable} is valid (inasmuch as we can check that in a
   * generic fashion).
   *
   * @param variable the {@link Variable} to validate
   */
  private static void assertVariableIsValid(Variable variable) {
    assertNotNull(variable);

    assertTrue(variable.getId() != null && !variable.getId().isEmpty());

    String assertionMessage =
        String.format("Invalid parse result for variable '%s'.", variable.getId());

    assertNotNull(variable.getCodebook(), assertionMessage);

    if (variable.getDescription().isPresent()) {
      assertFalse(variable.getDescription().get().isEmpty(), assertionMessage);
      for (String paragraph : variable.getDescription().get())
        assertTrue(paragraph != null && !paragraph.isEmpty(), assertionMessage);
    }

    if (variable.getShortName().isPresent())
      assertFalse(variable.getShortName().get().isEmpty(), assertionMessage);

    assertTrue(
        variable.getLongName() != null && !variable.getLongName().isEmpty(), assertionMessage);

    // Note: getLongName() is _usually_ the same as getId(), but not always.

    // Note: getType() isn't always present.

    assertTrue(variable.getLength() != null && variable.getLength() > 0, assertionMessage);

    if (variable.getSource().isPresent())
      assertFalse(variable.getSource().get().isEmpty(), assertionMessage);

    if (variable.getValueFormat().isPresent())
      assertFalse(variable.getValueFormat().get().isEmpty(), assertionMessage);

    if (variable.getValueGroups().isPresent())
      assertFalse(variable.getValueGroups().get().isEmpty(), assertionMessage);

    if (variable.getComment().isPresent()) {
      assertFalse(variable.getComment().get().isEmpty(), assertionMessage);
      for (String paragraph : variable.getComment().get())
        assertTrue(paragraph != null && !paragraph.isEmpty(), assertionMessage);
    }
  }
}
