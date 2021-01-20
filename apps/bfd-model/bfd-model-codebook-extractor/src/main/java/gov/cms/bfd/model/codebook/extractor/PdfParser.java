package gov.cms.bfd.model.codebook.extractor;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import gov.cms.bfd.model.codebook.model.Codebook;
import gov.cms.bfd.model.codebook.model.Value;
import gov.cms.bfd.model.codebook.model.ValueGroup;
import gov.cms.bfd.model.codebook.model.Variable;
import gov.cms.bfd.model.codebook.model.VariableType;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts a <a href="https://www.ccwdata.org/web/guest/data-dictionaries">CMS Chronic Conditions
 * Warehouse (CCW) data dictionary</a> codebook into a JAX-B model {@link Codebook} instance.
 */
public final class PdfParser {
  private static final Logger LOGGER = LoggerFactory.getLogger(PdfParser.class);

  public static final String FIELD_NAME_LABEL = "LABEL";
  private static final String FIELD_NAME_LABEL_ALT1 = "NAME";
  public static final String FIELD_NAME_DESCRIPTION = "DESCRIPTION";
  public static final String FIELD_NAME_SHORT_NAME = "SHORT NAME";
  public static final String FIELD_NAME_LONG_NAME = "LONG NAME";
  public static final String FIELD_NAME_TYPE = "TYPE";
  private static final String FIELD_NAME_LENGTH = "LENGTH";
  private static final String FIELD_NAME_SOURCE = "SOURCE";
  private static final String FIELD_NAME_VALUES = "VALUES";
  private static final String FIELD_NAME_VALUES_ALT1 = "CODE VALUES";
  private static final String FIELD_NAME_COMMENT = "COMMENT";

  /**
   * Matches "XX = YY" text lines that represent the start of a new {@link Value}. Provides
   * capturing groups for the code and the start of the description on the line.
   */
  private static final Pattern PATTERN_VALUE_LINE_WITH_CODE =
      Pattern.compile("^(\\S+)\\s+=\\s*(.*)$");

  /**
   * @param codebookSource the codebook to be converted
   * @return a {@link Codebook} instance representing the data from the parsed codebook PDF
   */
  public static Codebook parseCodebookPdf(SupportedCodebook codebookSource) {
    try (InputStream codebookPdfStream = codebookSource.getCodebookPdfInputStream(); ) {
      List<String> codebookTextLines = extractTextLinesFromPdf(codebookPdfStream);

      Codebook codebook = new Codebook(codebookSource);

      /*
       * It's a bit inefficient, but we first go through all the lines and group them
       * into the separate variable declarations represented by them. Just makes it
       * easier to reason about the logic here.
       */
      List<List<String>> variableSections = findVariableSections(codebookTextLines);

      // Parse each section into the Variable model it represents.
      for (List<String> variableSection : variableSections) {
        Variable variable = parseVariable(codebook, variableSection);
        codebook.getVariables().add(variable);
      }

      return codebook;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * @param codebook the {@link Codebook} that is being parsed
   * @param variableSection the text section representing a single {@link Variable} to be parsed
   * @return the {@link Variable} that was parsed from the specified text section
   */
  private static Variable parseVariable(Codebook codebook, List<String> variableSection) {
    Variable variable = new Variable(codebook);

    variable.setId(parseId(variableSection));
    variable.setLabel(parseLabel(variableSection));
    variable.setDescription(parseDescription(variableSection));
    variable.setShortName(parseShortName(variableSection));
    variable.setLongName(parseLongName(variableSection));
    variable.setType(parseType(variableSection));
    variable.setLength(parseLength(variableSection));
    variable.setSource(parseSource(variableSection));
    variable.setValueFormat(parseValueFormat(variableSection));
    variable.setValueGroups(parseValueGroups(variableSection));
    variable.setComment(parseComment(variableSection));

    return variable;
  }

  /**
   * @param pdfStream the {@link InputStream} for the PDF to extract the text lines of
   * @return the trimmed lines of text contained in the specified PDF {@link InputStream}
   */
  static List<String> extractTextLinesFromPdf(InputStream pdfStream) {
    PdfReader pdfReader = null;
    try {
      pdfReader = new PdfReader(pdfStream);

      List<String> textLines = new ArrayList<>();
      for (int page = 1; page <= pdfReader.getNumberOfPages(); page++) {
        String textFromPage = PdfTextExtractor.getTextFromPage(pdfReader, page);
        String[] linesFromPage = textFromPage.split("\n");
        for (String lineFromPage : linesFromPage) {
          /* Clean up the text a bit, to simplify later parsing logic. */

          // Remove leading/trailing whitespace.
          lineFromPage = lineFromPage.trim();

          // Normalize all spaces to single-spaced.
          int previousLength;
          do {
            previousLength = lineFromPage.length();
            lineFromPage = lineFromPage.replaceAll("  ", " ");
          } while (previousLength != lineFromPage.length());

          // Skip/elide "oops" lines.
          if (isTextLinePdfParsingJunk(lineFromPage)) continue;

          textLines.add(lineFromPage);
        }
      }

      return textLines;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      if (pdfReader != null) pdfReader.close();
    }
  }

  /**
   * @param lineFromPage the line of text parsed from a PDF to be checked
   * @return <code>true</code> if the specified line appears to be "junk" caused by problems with
   *     extracting text from a PDF, <code>false</code> if not
   */
  private static boolean isTextLinePdfParsingJunk(String lineFromPage) {
    /*
     * These lines appear when superscript text is parsed from a PDF, e.g. "1st"
     * with the "st" superscript. The superscript portion will be parsed out as a
     * separate line, directly above the line that it should appear in. It screws up
     * the grammar a bit, but we leave it out anyways.
     */
    if (lineFromPage.equals("st")
        || lineFromPage.equals("nd")
        || lineFromPage.equals("rd")
        || lineFromPage.equals("th")) return true;

    return false;
  }

  /**
   * @param codebookTextLines the lines of codebook text to process
   * @return a {@link List} of {@link List}s, where each inner {@link List} contains the text lines
   *     for a single variable
   */
  static List<List<String>> findVariableSections(List<String> codebookTextLines) {
    /*
     * The best mechanism for determining section delimiters seems to be this
     * strategy: 1) find the start and end of the variable sections, 2) within
     * there, mark section start/end boundaries by finding the "LABEL:" lines, which
     * are always the second line in each section.
     */
    int firstVariableLine = findFirstVariableLine(codebookTextLines);
    int lastVariableLine = findLastVariableLine(codebookTextLines);

    // Chop up the lines into the sections they represent.
    List<List<String>> variableSectionLineGroups = new ArrayList<>();
    List<String> sectionBuilder = new ArrayList<>();
    for (int lineIndex = firstVariableLine; lineIndex <= lastVariableLine; lineIndex++) {
      String line = codebookTextLines.get(lineIndex);

      // Add any line that isn't document metadata.
      if (!isDocumentMetadata(line)) sectionBuilder.add(line);

      /*
       * Every time we hit a "LABEL:" we know that we're either 1) at the second line
       * of the very first section, or 2) that we're starting a new section and just
       * finished one two lines earlier.
       */
      if (containsVariableLabelField(line)) {
        if (sectionBuilder.size() == 2) {
          // Do nothing: we're near the beginning of the very first section.
        } else {
          // Add a copy of the completed section to our sections list.
          List<String> completedSection = sectionBuilder.subList(0, sectionBuilder.size() - 2);
          variableSectionLineGroups.add(new ArrayList<>(completedSection));

          // Remove the completed section from the builder.
          completedSection.clear();
        }
      }
    }

    /*
     * Make sure to grab the last section, which obviously won't have a "LABEL:"
     * after it to trigger the above logic, and so must be handled manually.
     */
    if (!sectionBuilder.isEmpty()) variableSectionLineGroups.add(new ArrayList<>(sectionBuilder));

    return variableSectionLineGroups;
  }

  /**
   * @param codebookTextLines the {@link List} of codebook lines to search
   * @return the index of the first line of the first variable definition in the specified codebook
   *     lines {@link List}
   */
  private static int findFirstVariableLine(List<String> codebookTextLines) {
    /*
     * There's a pretty simple algorithm here that applies to all the codebooks we
     * care about: find the first line that starts with "LABEL:", then walk back to
     * the first non-metadata line. That line will be the first variables section
     * line.
     */
    for (int lineIndex = 0; lineIndex < codebookTextLines.size(); lineIndex++) {
      String line = codebookTextLines.get(lineIndex);

      if (containsVariableLabelField(line)) {
        for (int lineIndexGoingBack = lineIndex - 1;
            lineIndexGoingBack >= 0;
            lineIndexGoingBack--) {
          String lineGoingBack = codebookTextLines.get(lineIndexGoingBack);
          if (!isDocumentMetadata(lineGoingBack)) return lineIndexGoingBack;
        }
      }
    }

    throw new IllegalStateException("Unexpected codebook format.");
  }

  /**
   * @param codebookTextLines the {@link List} of codebook lines to search
   * @return the index of the last line of the last variable definition in the specified codebook
   *     lines {@link List}
   */
  private static int findLastVariableLine(List<String> codebookTextLines) {
    /*
     * This is actually pretty simple. It's only pulled out into a method for
     * consistency's sake.
     */
    return codebookTextLines.size() - 1;
  }

  /**
   * @param line the codebook PDF text line to check
   * @return <code>true</code> if the line represents document metadata that should be left out of
   *     the parsing (e.g. page footers), <code>false</code> otherwise
   */
  private static boolean isDocumentMetadata(String line) {
    /*
     * We also skip blank lines, as those aren't reliably useful for parsing, given
     * the vagaries of extracting text from PDFs.
     */

    Pattern[] metadataPatterns =
        new Pattern[] {
          Pattern.compile("^$"),
          Pattern.compile("^Medicare FFS Claims \\(Version .\\) Codebook .*$"),
          Pattern.compile("^\\^ Back to TOC \\^$"),
          Pattern.compile("^CMS Chronic Conditions Data Warehouse \\(CCW\\) – Codebook$"),
          Pattern.compile(
              "^Master Beneficiary Summary File \\(MBSF\\) with Medicare Part A, B, C & D$"),
          Pattern.compile("^Medicare Part D Event \\(PDE\\) \\/ Drug Characteristics File$"),
          Pattern.compile("^.*Version .*\\s+Page \\d+ of \\d+$")
        };

    for (Pattern metadataPattern : metadataPatterns)
      if (metadataPattern.matcher(line).matches()) return true;

    return false;
  }

  /**
   * @param line the codebook text line to check
   * @return <code>true</code> if the specified line appears to be the "LABEL:" field in a variable
   *     section, <code>false</code> otherwise
   */
  private static boolean containsVariableLabelField(String line) {
    /*
     * Almost all variables use the "LABEL:" field name, but at least some instead
     * use "NAME:" for no apparent reason. (One example: the beneficiary "BUYIN01"
     * variable from the 2017-05 codebook version.)
     */
    return line.startsWith(FIELD_NAME_LABEL + ":") || line.startsWith(FIELD_NAME_LABEL_ALT1 + ":");
  }

  /**
   * @param variableSection the variable section to parse the value from
   * @return the {@link Variable#getId()} value from the specified {@link Variable} raw text section
   */
  private static String parseId(List<String> variableSection) {
    // The first line of each section is always the ID, by itself.
    return variableSection.get(0);
  }

  /**
   * @param variableSection the variable section to parse the value from
   * @return the {@link Variable#getLabel()} value from the specified {@link Variable} raw text
   *     section
   */
  static String parseLabel(List<String> variableSection) {
    /*
     * Almost all variables use the "LABEL:" field name, but at least some instead
     * use "NAME:" for no apparent reason. (One example: the beneficiary "BUYIN01"
     * variable from the 2017-05 codebook version.) Some variables' labels are long
     * enough to take up more than one line, but they appear to never be multiple
     * paragraphs.
     */
    List<String> fieldText =
        extractFieldContent(variableSection, FIELD_NAME_LABEL, FIELD_NAME_LABEL_ALT1);
    List<String> fieldParagraphs = extractParagraphs(fieldText);
    if (fieldParagraphs.size() != 1)
      throw new IllegalStateException(
          String.format(
              "Invalid '%s' field in variable section: %s", FIELD_NAME_LABEL, variableSection));

    return fieldParagraphs.get(0);
  }

  /**
   * @param variableSection the variable section to parse the value from
   * @return the {@link Variable#getDescription()} value from the specified {@link Variable} raw
   *     text section
   */
  private static List<String> parseDescription(List<String> variableSection) {
    List<String> fieldText = extractFieldContent(variableSection, FIELD_NAME_DESCRIPTION);
    List<String> fieldParagraphs = extractParagraphs(fieldText);

    return fieldParagraphs;
  }

  /**
   * @param variableSection the variable section to parse the value from
   * @return the {@link Variable#getShortName()} value from the specified {@link Variable} raw text
   *     section, or <code>null</code> if none was specified
   */
  private static String parseShortName(List<String> variableSection) {
    List<String> fieldText = extractFieldContent(variableSection, FIELD_NAME_SHORT_NAME);

    if (fieldText.isEmpty()) return null;

    if (fieldText.size() != 1)
      throw new IllegalStateException(
          String.format(
              "Invalid '%s' field in variable section: %s",
              FIELD_NAME_SHORT_NAME, variableSection));

    return fieldText.get(0);
  }

  /**
   * @param variableSection the variable section to parse the value from
   * @return the {@link Variable#getLongName()} value from the specified {@link Variable} raw text
   *     section
   */
  private static String parseLongName(List<String> variableSection) {
    List<String> fieldText = extractFieldContent(variableSection, FIELD_NAME_LONG_NAME);
    if (fieldText.size() != 1)
      throw new IllegalStateException(
          String.format(
              "Invalid '%s' field in variable section: %s", FIELD_NAME_LONG_NAME, variableSection));

    return fieldText.get(0);
  }

  /**
   * @param variableSection the variable section to parse the value from
   * @return the {@link Variable#getType()} value from the specified {@link Variable} raw text
   *     section
   */
  private static VariableType parseType(List<String> variableSection) {
    List<String> fieldText = extractFieldContent(variableSection, FIELD_NAME_TYPE);
    if (fieldText == null) return null;
    if (fieldText.size() != 1)
      throw new IllegalStateException(
          String.format(
              "Invalid '%s' field in variable section: %s", FIELD_NAME_TYPE, variableSection));

    return VariableType.valueOf(fieldText.get(0));
  }

  /**
   * @param variableSection the variable section to parse the value from
   * @return the {@link Variable#getLength()} value from the specified {@link Variable} raw text
   *     section
   */
  private static Integer parseLength(List<String> variableSection) {
    List<String> fieldText = extractFieldContent(variableSection, FIELD_NAME_LENGTH);
    if (fieldText.size() != 1)
      throw new IllegalStateException(
          String.format(
              "Invalid '%s' field in variable section: %s", FIELD_NAME_LENGTH, variableSection));

    return Integer.parseInt(fieldText.get(0));
  }

  /**
   * @param variableSection the variable section to parse the value from
   * @return the {@link Variable#getSource()} value from the specified {@link Variable} raw text
   *     section, or <code>null</code> if none was present
   */
  private static String parseSource(List<String> variableSection) {
    List<String> fieldText = extractFieldContent(variableSection, FIELD_NAME_SOURCE);

    if (fieldText.isEmpty()) return null;

    if (fieldText.size() != 1)
      throw new IllegalStateException(
          String.format(
              "Invalid '%s' field in variable section: %s", FIELD_NAME_SOURCE, variableSection));

    return fieldText.get(0);
  }

  /**
   * @param variableSection the variable section to parse the value from
   * @return the {@link Variable#getValueFormat()} value from the specified {@link Variable} raw
   *     text section, or <code>null</code> if it was not present
   */
  private static String parseValueFormat(List<String> variableSection) {
    /*
     * The parsing strategy here is basically this: 1) each Variable has EITHER a
     * valueFormat or valueGroups, 2) if the field value includes at least one
     * "XX = YY" line, it's a valueGroups, 3) otherwise (if it doesn't contain a
     * code list), it's a valueFormat.
     */

    List<String> fieldText =
        extractFieldContent(variableSection, FIELD_NAME_VALUES, FIELD_NAME_VALUES_ALT1);

    // If the field isn't present at all, something's wrong.
    if (fieldText == null)
      throw new IllegalStateException(
          String.format(
              "Invalid '%s' field in variable section: %s", FIELD_NAME_VALUES, variableSection));

    // If the field is present but empty, just return null.
    if (fieldText.isEmpty()) return null;

    // Does this field have a an "XX = YY" coded value?
    Pattern codedValuePattern = Pattern.compile("^\\w+\\s+=\\s+.*$");
    for (String line : variableSection) {
      if (codedValuePattern.matcher(line).matches()) return null;
    }

    /*
     * The parsing logic specified above is a bit hacky, so we just have a sanity
     * check here to catch any edge cases it might leave out.
     */
    if (fieldText.size() > 2
        && !"FI_NUM".equals(parseId(variableSection))
        && !"TIER_ID".equals(parseId(variableSection)))
      throw new IllegalStateException(
          String.format(
              "Suspicious value format for '%s': %s", parseId(variableSection), variableSection));

    return fieldText.get(0);
  }

  /**
   * @param variableSection the variable section to parse the value from
   * @return the {@link Variable#getValueGroups()} value from the specified {@link Variable} raw
   *     text section, or <code>null</code> if it was not present
   */
  private static List<ValueGroup> parseValueGroups(List<String> variableSection) {
    /*
     * The parsing strategy here is basically this: 1) each Variable has EITHER a
     * valueFormat or valueGroups, 2) if the field value includes at least one
     * "XX = YY" line, it's a valueGroups, 3) otherwise (if it doesn't contain a
     * code list), it's a valueFormat.
     */

    String variableId = parseId(variableSection);
    List<String> fieldLines =
        extractFieldContent(variableSection, FIELD_NAME_VALUES, FIELD_NAME_VALUES_ALT1);
    if (fieldLines == null)
      throw new IllegalStateException(
          String.format(
              "Invalid '%s' field in variable section: %s", FIELD_NAME_VALUES, variableSection));

    // Does this field have a an "XX = YY" coded value?
    boolean foundCodedValue = false;
    for (String line : variableSection) {
      if (PATTERN_VALUE_LINE_WITH_CODE.matcher(line).matches()) foundCodedValue = true;
    }
    if (!foundCodedValue) return null;

    /*
     * Now we know we're dealing with coded values, so we need to parse those.
     */

    List<ValueGroup> valueGroups = new ArrayList<>();
    ValueGroup currentValueGroup = new ValueGroup();
    List<String> currentValueGroupDescription = new ArrayList<>();
    List<String> currentValue = new ArrayList<>();
    for (int fieldLineIndex = 0; fieldLineIndex < fieldLines.size(); fieldLineIndex++) {
      String fieldLine = fieldLines.get(fieldLineIndex);
      if (isValueGroupDescription(variableId, fieldLines, fieldLineIndex)) {
        // FYI: We're in a ValueGroup description.

        if (!currentValue.isEmpty()) {
          // FYI: We just ended a Value that needs to be collected.
          Value completedValue = parseValue(currentValue);
          currentValueGroup.getValues().add(completedValue);
          currentValue = new ArrayList<>();

          // FYI: We also just ended a ValueGroup that needs to be collected.
          valueGroups.add(currentValueGroup);
          currentValueGroup = new ValueGroup();
        }

        // Regardless of what else is happening, always collect the line.
        currentValueGroupDescription.add(fieldLine);
      } else {
        // FYI: We're in a Value.
        boolean isLineStartOfValue = PATTERN_VALUE_LINE_WITH_CODE.matcher(fieldLine).matches();

        if (!currentValueGroupDescription.isEmpty()) {
          // FYI: We just ended a ValueGroup description that needs to be collected.

          List<String> valueGroupDescriptionParagraphs =
              extractParagraphs(currentValueGroupDescription);
          currentValueGroup.setDescription(valueGroupDescriptionParagraphs);
          currentValueGroupDescription = new ArrayList<>();
        }

        if (isLineStartOfValue && !currentValue.isEmpty()) {
          // FYI: We're starting a new Value and need to collect the previous one.

          Value completedValue = parseValue(currentValue);
          currentValueGroup.getValues().add(completedValue);
          currentValue = new ArrayList<>();
        }

        // Regardless of what else is happening, always collect the line.
        currentValue.add(fieldLine);
      }
    }

    if (!currentValue.isEmpty()) {
      // FYI: We're through all lines but need to collect the last Value.

      Value completedValue = parseValue(currentValue);
      currentValueGroup.getValues().add(completedValue);

      // FYI: We also just ended a ValueGroup that needs to be collected.
      valueGroups.add(currentValueGroup);
    }

    // Sanity check: make sure we don't have any leftovers.
    if (!currentValueGroupDescription.isEmpty()) throw new BadCodeMonkeyException();

    return valueGroups;
  }

  /**
   * @param variableId the {@link Variable#getId()} of the {@link Variable} being parsed
   * @param fieldLines the {@link Variable#getValueGroups()} text being parsed
   * @param fieldLineIndex the index of the specific {@link Variable#getValueGroups()} text line to
   *     examine
   * @return <code>true</code> if the specified line is part of a {@link
   *     ValueGroup#getDescription()}, <code>false</code> otherwise
   */
  private static boolean isValueGroupDescription(
      String variableId, List<String> fieldLines, int fieldLineIndex) {
    /*
     * If the very first line for the field is an "XX = YY" line, it's safe to
     * assume that none of the field's lines are ValueGroup.descriptions.
     */
    if (PATTERN_VALUE_LINE_WITH_CODE.matcher(fieldLines.get(0)).matches()) return false;

    /*
     * If the specified line is before the first "XX = YY" line, then it's the
     * ValueGroup.description for the first ValueGroup.
     */
    boolean foundValueCodeLine = false;
    for (int i = 0; i <= fieldLineIndex; i++) {
      String fieldLine = fieldLines.get(i);
      if (PATTERN_VALUE_LINE_WITH_CODE.matcher(fieldLine).matches()) foundValueCodeLine = true;
    }
    if (foundValueCodeLine == false) return true;

    /*
     * After the first ValueGroup.description, there's no way to reliably determine
     * if non-"XX = YY" lines are part of a Value.description or part of a the
     * ValueGroup.description for a new ValueGroup. Instead, we keep a hardcoded
     * whitelist of lines known to be subsequent ValueGroup.description lines and
     * check against that. It sucks, but there isn't a better alternative.
     */
    Map<String, String[]> knownValueGroupDescriptionLinesByVariable = new HashMap<>();
    knownValueGroupDescriptionLinesByVariable.put(
        "CARR_CLM_PMT_DNL_CD",
        new String[] {
          "Prior to 2011, the following 1-byte character codes were also valid (these characters",
          "preceded use of 2-byte codes, above):"
        });
    knownValueGroupDescriptionLinesByVariable.put(
        "CARR_LINE_PRVDR_TYPE_CD",
        new String[] {
          "NOTE: PRIOR TO VERSION H, DME claims also used this code; the following were valid",
          "code VALUES:"
        });
    // TODO complete this table
    // knownValueGroupDescriptionLinesByVariable.put("", new String[] { "" });

    List<String> knownValueGroupDescriptionLines =
        knownValueGroupDescriptionLinesByVariable.containsKey(variableId)
            ? Arrays.asList(knownValueGroupDescriptionLinesByVariable.get(variableId))
            : Collections.emptyList();

    /*
     * Log each variable that has ValueGroup.description lines, so their parsing can
     * be checked by hand, since there's no reliable algorithm for finding later
     * ValueGroup lines.
     */
    if (!knownValueGroupDescriptionLinesByVariable.containsKey(variableId)) {
      LOGGER.warn(
          "The '{}' variable appears to have VALUE groups. Be sure to verify it parsed correctly!",
          variableId);
    }

    String fieldLine = fieldLines.get(fieldLineIndex);
    return knownValueGroupDescriptionLines.contains(fieldLine);
  }

  /**
   * @param valueLines the lines of text representing a {@link Value} to be parsed
   * @return the {@link Value} parsed from those lines
   */
  private static Value parseValue(List<String> valueLines) {
    // Copy the list so we can bang on it safely.
    List<String> valueLinesCopy = new ArrayList<>(valueLines);

    // Parse the first line.
    Matcher valueStartMatcher = PATTERN_VALUE_LINE_WITH_CODE.matcher(valueLinesCopy.get(0));
    valueStartMatcher.matches();

    // Grab the code from the first line.
    String code = valueStartMatcher.group(1);

    // Strip out the "XX = " prefix from the first line.
    valueLinesCopy.set(0, valueStartMatcher.group(2));

    // Convert it all to "paragraphs" to undo the line wrapping.
    valueLinesCopy = extractParagraphs(valueLinesCopy);

    // Just in case multiple paragraphs were found, glue them back together.
    StringBuilder description = new StringBuilder();
    for (Iterator<String> paragraphsIter = valueLinesCopy.iterator(); paragraphsIter.hasNext(); ) {
      description.append(paragraphsIter.next());
      if (paragraphsIter.hasNext()) description.append(' ');
    }

    Value value = new Value();
    value.setCode(code);
    value.setDescription(description.toString());
    return value;
  }

  /**
   * @param variableSection the variable section to parse the value from
   * @return the {@link Variable#getComment()} value from the specified {@link Variable} raw text
   *     section, or <code>null</code> if none was present
   */
  private static List<String> parseComment(List<String> variableSection) {
    List<String> fieldText = extractFieldContent(variableSection, FIELD_NAME_COMMENT);
    if (fieldText.isEmpty()) return null;
    List<String> fieldParagraphs = extractParagraphs(fieldText);

    /*
     * This is just a sanity check that has caught some parsing code errors in the
     * past. The last chunk of text in the variable section should always be
     * captured in the COMMENT field.
     */
    String sectionEndText = variableSection.get(variableSection.size() - 1);
    sectionEndText = sectionEndText.replaceAll("COMMENT:\\s+", "");
    if (!fieldParagraphs.get(fieldParagraphs.size() - 1).contains(sectionEndText))
      throw new BadCodeMonkeyException(
          String.format(
              "Parsed the '%s' field incorrectly for the '%s' variable.",
              FIELD_NAME_COMMENT, variableSection.get(0)));

    return fieldParagraphs;
  }

  /**
   * @param variableSection the variable section to extract the field content from
   * @param fieldNames the possible names of the variable section field to extract the content for
   * @return the lines of text between the specified variable section field name and the next field,
   *     exclusive
   */
  private static List<String> extractFieldContent(
      List<String> variableSection, String... fieldNames) {
    StringBuilder fieldNamePattern = new StringBuilder();
    fieldNamePattern.append("(?:");
    for (int i = 0; i < fieldNames.length; i++) {
      fieldNamePattern.append(fieldNames[i]);
      if (i < (fieldNames.length - 1)) fieldNamePattern.append('|');
    }
    fieldNamePattern.append(')');

    Pattern fieldStartPattern = Pattern.compile("^" + fieldNamePattern.toString() + ":(.*)$");
    Pattern fieldEndPattern = Pattern.compile("^([A-Z_0-9 ]+):.*$");

    // Find the starting field line and the line of the next field after it.
    Integer fieldStartLineIndex = null;
    String fieldStartLineContent = null;
    Integer fieldEndLineIndex = null;
    for (int lineIndex = 0; lineIndex < variableSection.size(); lineIndex++) {
      String line = variableSection.get(lineIndex);

      Matcher fieldStartMatcher = fieldStartPattern.matcher(line);
      if (fieldStartMatcher.matches()) {
        fieldStartLineIndex = lineIndex;
        fieldStartLineContent = fieldStartMatcher.group(1).trim();
        continue;
      }

      Matcher fieldEndMatcher = fieldEndPattern.matcher(line);
      if (fieldStartLineIndex != null && fieldEndMatcher.matches()) {
        String nextFieldLabel = fieldEndMatcher.group(1);

        /*
         * The DESCRIPTION and COMMENT fields have lots of paragraphs that, in plain
         * text, _look_ like field labels, but aren't. For example, the
         * CLM_MDCR_NON_PMT_RSN_CD variable's COMMENT has a paragraph that starts with
         * "NOTE3: Effective...".
         */
        String knownFieldLabels[] =
            new String[] {
              FIELD_NAME_LABEL,
              FIELD_NAME_LABEL_ALT1,
              FIELD_NAME_DESCRIPTION,
              FIELD_NAME_SHORT_NAME,
              FIELD_NAME_LONG_NAME,
              FIELD_NAME_TYPE,
              FIELD_NAME_LENGTH,
              FIELD_NAME_SOURCE,
              FIELD_NAME_VALUES,
              FIELD_NAME_VALUES_ALT1,
              FIELD_NAME_COMMENT
            };
        if (Arrays.stream(knownFieldLabels).anyMatch(l -> l.equals(nextFieldLabel))) {
          fieldEndLineIndex = lineIndex;
          break;
        }
      }
    }

    if (fieldStartLineIndex == null)
      /*
       * Fields aren't always present, e.g. the bene B_MO_CNT variable doesn't have a
       * TYPE.
       */
      return null;
    if (fieldEndLineIndex == null)
      // Might have been the last field in the section.
      fieldEndLineIndex = variableSection.size();

    // Build results.
    List<String> fieldContent = new ArrayList<>(fieldEndLineIndex - fieldStartLineIndex);
    fieldContent.add(fieldStartLineContent);
    fieldContent.addAll(
        new ArrayList<>(variableSection.subList(fieldStartLineIndex + 1, fieldEndLineIndex)));

    /*
     * Do some last-second normalization of the results, to help eliminate
     * mostly-empty results.
     */
    List<String> fieldContentNormalized = new ArrayList<>(fieldEndLineIndex - fieldStartLineIndex);
    for (String fieldContentLine : fieldContent) {
      // Skip blank lines.
      if (fieldContentLine.isEmpty()) continue;

      // Skip lines that are just "-" (as happens for missing/empty COMMENTs).
      if (fieldContentLine.equals("-")) continue;

      fieldContentNormalized.add(fieldContentLine);
    }

    return fieldContentNormalized;
  }

  /**
   * @param text the text to extract paragraphs from
   * @return a new {@link List} of {@link String}s, with one entry per paragraph that was found in
   *     the original list of word-wrapped {@link String}s
   */
  private static List<String> extractParagraphs(List<String> text) {
    /*
     * So this is... a bit sketchy. There's no perfect method for extracting
     * paragraphs from a PDF, even if we weren't converting it to plain text first.
     * PDFs don't _have_ paragraphs -- they just have rectangles with text in them,
     * set various distances apart. What we do here is a heuristic approach: a line
     * is the end of a paragraph if it ends with a period. Somewhat surprisingly, QA
     * reveals that simple approach to be "good enough".
     */

    List<String> paragraphs = new ArrayList<>();

    StringBuilder paragraphBuilder = new StringBuilder();
    for (Iterator<String> lineIter = text.iterator(); lineIter.hasNext(); ) {
      String line = lineIter.next();
      paragraphBuilder.append(line);

      if (line.endsWith(".")) {
        paragraphs.add(paragraphBuilder.toString());
        paragraphBuilder = new StringBuilder();
      } else if (lineIter.hasNext()) {
        // Ensure that words across lines aren't jammed together.
        if (!paragraphBuilder.toString().endsWith("-")) paragraphBuilder.append(' ');
      }
    }

    // Always grab whatever hasn't been flushed (if anything) as a new line.
    if (paragraphBuilder.length() > 0) paragraphs.add(paragraphBuilder.toString());

    return paragraphs;
  }
}
