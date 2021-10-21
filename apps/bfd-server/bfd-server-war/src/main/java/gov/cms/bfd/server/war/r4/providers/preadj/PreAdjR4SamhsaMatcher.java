package gov.cms.bfd.server.war.r4.providers.preadj;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.springframework.stereotype.Component;

/**
 * A {@link Predicate} that, when <code>true</code>, indicates that an {@link ExplanationOfBenefit}
 * (i.e. claim) is SAMHSA-related.
 *
 * <p>See <code>/bluebutton-data-server.git/dev/design-samhsa-filtering.md</code> for details on the
 * design of this feature.
 *
 * <p>This class is designed to be thread-safe, as it's expensive to construct and so should be used
 * as a singleton.
 */
@Component
public final class PreAdjR4SamhsaMatcher implements Predicate<PreAdjR4SamhsaMatcher.SamhsaCheck> {
  /** The {@link CSVFormat} used to parse the SAMHSA-related code CSV files. */
  private static final CSVFormat CSV_FORMAT = CSVFormat.EXCEL.withHeader();

  private final List<String> drgCodes;
  private final List<String> cptCodes;
  private final List<String> icd9ProcedureCodes;
  private final List<String> icd9DiagnosisCodes;
  private final List<String> icd10ProcedureCodes;
  private final List<String> icd10DiagnosisCodes;

  /**
   * Constructs a new {@link PreAdjR4SamhsaMatcher}, loading the lists of SAMHSA-related codes from
   * the classpath.
   */
  public PreAdjR4SamhsaMatcher() {
    this.drgCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList("samhsa-related-codes/codes-drg.csv", "MS-DRGs").stream()
                .map(PreAdjR4SamhsaMatcher::normalizeDrgCode)
                .collect(Collectors.toList()));
    this.cptCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList("samhsa-related-codes/codes-cpt.csv", "CPT Code"));
    this.icd9ProcedureCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList("samhsa-related-codes/codes-icd-9-procedure.csv", "ICD-9-CM")
                .stream()
                .map(PreAdjR4SamhsaMatcher::normalizeIcd9Code)
                .collect(Collectors.toList()));
    this.icd9DiagnosisCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList(
                    "samhsa-related-codes/codes-icd-9-diagnosis.csv", "ICD-9-CM Diagnosis Code")
                .stream()
                .map(PreAdjR4SamhsaMatcher::normalizeIcd9Code)
                .collect(Collectors.toList()));
    this.icd10ProcedureCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList(
                    "samhsa-related-codes/codes-icd-10-procedure.csv", "ICD-10-PCS Code")
                .stream()
                .map(PreAdjR4SamhsaMatcher::normalizeIcd10Code)
                .collect(Collectors.toList()));
    this.icd10DiagnosisCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList(
                    "samhsa-related-codes/codes-icd-10-diagnosis.csv", "ICD-10-CM Diagnosis Code")
                .stream()
                .map(PreAdjR4SamhsaMatcher::normalizeIcd10Code)
                .collect(Collectors.toList()));
  }

  /**
   * @param csvResourceName the classpath resource name of the CSV file to parse
   * @param columnToReturn the name of the column to return from the CSV file
   * @return a {@link List} of values from the specified column of the specified CSV file
   */
  private static List<String> resourceCsvColumnToList(
      String csvResourceName, String columnToReturn) {
    try (InputStream csvStream =
            Thread.currentThread().getContextClassLoader().getResourceAsStream(csvResourceName);
        InputStreamReader csvReader = new InputStreamReader(csvStream, StandardCharsets.UTF_8);
        CSVParser csvParser = new CSVParser(csvReader, CSV_FORMAT); ) {
      List<String> columnValues = new ArrayList<>();
      csvParser.forEach(record -> columnValues.add(record.get(columnToReturn)));
      return columnValues;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public boolean test(SamhsaCheck samhsaCheck) {
    return samhsaCheck.icd10ProcCodes.stream()
            .anyMatch(c -> icd10ProcedureCodes.contains(normalizeIcd10Code(c)))
        || samhsaCheck.icd10DiagCodes.stream()
            .anyMatch(c -> icd10DiagnosisCodes.contains(normalizeIcd10Code(c)))
        || samhsaCheck.icd9DiagCodes.stream()
            .anyMatch(c -> icd9DiagnosisCodes.contains(normalizeIcd9Code(c)))
        || samhsaCheck.cptCodes.stream().anyMatch(cptCodes::contains);
  }

  public static class SamhsaCheck {
    private final Set<String> icd10ProcCodes = new HashSet<>();
    private final Set<String> icd10DiagCodes = new HashSet<>();
    private final Set<String> icd9DiagCodes = new HashSet<>();
    private final Set<String> cptCodes = new HashSet<>();

    private SamhsaCheck() {}

    public static SamhsaCheck create() {
      return new SamhsaCheck();
    }

    public SamhsaCheck addIcd10ProcCode(String code) {
      return addCode(icd10ProcCodes, code);
    }

    public SamhsaCheck addIcd10ProcCode(Collection<String> codes) {
      return addCode(icd10ProcCodes, codes);
    }

    public SamhsaCheck addIcd10DiagCode(String code) {
      return addCode(icd10DiagCodes, code);
    }

    public SamhsaCheck addIcd10DiagCode(Collection<String> codes) {
      return addCode(icd10DiagCodes, codes);
    }

    public SamhsaCheck addIcd9DiagCode(String code) {
      return addCode(icd9DiagCodes, code);
    }

    public SamhsaCheck addIcd9DiagCode(Collection<String> codes) {
      return addCode(icd9DiagCodes, codes);
    }

    public SamhsaCheck addCptCode(String code) {
      return addCode(cptCodes, code);
    }

    public SamhsaCheck addCptCode(Collection<String> codes) {
      return addCode(cptCodes, codes);
    }

    SamhsaCheck addCode(Set<String> set, String code) {
      return addCode(set, Collections.singleton(code));
    }

    SamhsaCheck addCode(Set<String> set, Collection<String> codes) {
      set.addAll(codes);
      set.removeIf(Objects::isNull);
      return this;
    }
  }

  /**
   * @param icd9Code the ICD-9 diagnosis code to normalize
   * @return the specified ICD-9 code, but with whitespace trimmed, the first (if any) decimal point
   *     removed, and converted to all-caps
   */
  private static String normalizeIcd9Code(String icd9Code) {
    icd9Code = icd9Code.trim();
    icd9Code = icd9Code.replaceFirst("\\.", "");
    icd9Code = icd9Code.toUpperCase();

    return icd9Code;
  }

  /**
   * Example input: MS-DRG 522 Example output: 522
   *
   * @param code
   * @return the specified DRG code, but with the "MS-DRG" prefix and space removed.
   */
  private static String normalizeDrgCode(String code) {
    code = code.trim();
    code = code.replace("MS-DRG ", "");
    return code;
  }

  /**
   * @param icd10DiagnosisCode the ICD-10 diagnosis code to normalize
   * @return the specified ICD-10 code, but with whitespace trimmed, the first (if any) decimal
   *     point removed, and converted to all-caps
   */
  private static String normalizeIcd10Code(String icd10DiagnosisCode) {
    icd10DiagnosisCode = icd10DiagnosisCode.trim();
    icd10DiagnosisCode = icd10DiagnosisCode.replaceFirst("\\.", "");
    icd10DiagnosisCode = icd10DiagnosisCode.toUpperCase();

    return icd10DiagnosisCode;
  }

  /**
   * @param hcpcsCode the HCPCS code to normalize
   * @return the specified HCPCS code, but with whitespace trimmed and converted to all-caps
   */
  private static String normalizeHcpcsCode(String hcpcsCode) {
    hcpcsCode = hcpcsCode.trim();
    hcpcsCode = hcpcsCode.toUpperCase();

    return hcpcsCode;
  }
}
