package gov.cms.bfd.server.war.commons;

import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

public abstract class AbstractSamhsaMatcher<T> implements Predicate<T> {

  /** The {@link CSVFormat} used to parse the SAMHSA-related code CSV files. */
  private static final CSVFormat CSV_FORMAT = CSVFormat.EXCEL.withHeader();

  protected static final String DRG =
      CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.CLM_DRG_CD);

  protected final List<String> drgCodes;
  protected final List<String> cptCodes;
  protected final List<String> icd9ProcedureCodes;
  protected final List<String> icd9DiagnosisCodes;
  protected final List<String> icd10ProcedureCodes;
  protected final List<String> icd10DiagnosisCodes;

  /**
   * Constructs a new {@link AbstractSamhsaMatcher}, loading the lists of SAMHSA-related codes from
   * the classpath. The list data is normalized as it is loaded.
   */
  protected AbstractSamhsaMatcher() {
    this.drgCodes =
        resourceCsvColumnToList("samhsa-related-codes/codes-drg.csv", "MS-DRGs").stream()
            .map(AbstractSamhsaMatcher::normalizeDrgCode)
            .collect(Collectors.toUnmodifiableList());
    this.cptCodes =
        resourceCsvColumnToList("samhsa-related-codes/codes-cpt.csv", "CPT Code").stream()
            .map(AbstractSamhsaMatcher::normalizeHcpcsCode)
            .collect(Collectors.toUnmodifiableList());
    this.icd9ProcedureCodes =
        resourceCsvColumnToList("samhsa-related-codes/codes-icd-9-procedure.csv", "ICD-9-CM")
            .stream()
            .map(AbstractSamhsaMatcher::normalizeIcdCode)
            .collect(Collectors.toUnmodifiableList());
    this.icd9DiagnosisCodes =
        resourceCsvColumnToList(
                "samhsa-related-codes/codes-icd-9-diagnosis.csv", "ICD-9-CM Diagnosis Code")
            .stream()
            .map(AbstractSamhsaMatcher::normalizeIcdCode)
            .collect(Collectors.toUnmodifiableList());
    this.icd10ProcedureCodes =
        resourceCsvColumnToList(
                "samhsa-related-codes/codes-icd-10-procedure.csv", "ICD-10-PCS Code")
            .stream()
            .map(AbstractSamhsaMatcher::normalizeIcdCode)
            .collect(Collectors.toUnmodifiableList());
    this.icd10DiagnosisCodes =
        resourceCsvColumnToList(
                "samhsa-related-codes/codes-icd-10-diagnosis.csv", "ICD-10-CM Diagnosis Code")
            .stream()
            .map(AbstractSamhsaMatcher::normalizeIcdCode)
            .collect(Collectors.toUnmodifiableList());
  }

  /**
   * @param csvResourceName the classpath resource name of the CSV file to parse
   * @param columnToReturn the name of the column to return from the CSV file
   * @return a {@link List} of values from the specified column of the specified CSV file
   */
  @VisibleForTesting
  static List<String> resourceCsvColumnToList(String csvResourceName, String columnToReturn) {
    try (InputStream csvStream =
            Thread.currentThread().getContextClassLoader().getResourceAsStream(csvResourceName);
        InputStreamReader csvReader = new InputStreamReader(csvStream, StandardCharsets.UTF_8);
        CSVParser csvParser = new CSVParser(csvReader, CSV_FORMAT)) {
      List<String> columnValues = new ArrayList<>();
      csvParser.forEach(r -> columnValues.add(r.get(columnToReturn)));
      return columnValues;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Example input: MS-DRG 522 Example output: 522
   *
   * @param code The drg code to normalize.
   * @return the specified DRG code, but with the "MS-DRG" prefix and space removed.
   */
  protected static String normalizeDrgCode(String code) {
    code = code.trim();
    code = code.replace("MS-DRG ", "");
    return code;
  }

  /**
   * @param icdCode the ICD-9 or ICD-10 diagnosis code to normalize
   * @return the specified ICD-9 or ICD-10 code, but with whitespace trimmed, the first (if any)
   *     decimal point removed, and converted to all-caps
   */
  protected static String normalizeIcdCode(String icdCode) {
    icdCode = icdCode.trim();
    icdCode = icdCode.replaceFirst("\\.", "");
    icdCode = icdCode.toUpperCase();

    return icdCode;
  }

  /**
   * @param hcpcsCode the HCPCS code to normalize
   * @return the specified HCPCS code, but with whitespace trimmed and converted to all-caps
   */
  protected static String normalizeHcpcsCode(String hcpcsCode) {
    hcpcsCode = hcpcsCode.trim();
    hcpcsCode = hcpcsCode.toUpperCase();

    return hcpcsCode;
  }
}
