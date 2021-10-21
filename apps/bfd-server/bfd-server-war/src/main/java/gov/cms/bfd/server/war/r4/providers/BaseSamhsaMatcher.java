package gov.cms.bfd.server.war.r4.providers;

import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.server.war.commons.IcdCode;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

public abstract class BaseSamhsaMatcher<T> implements Predicate<T> {

  /** The {@link CSVFormat} used to parse the SAMHSA-related code CSV files. */
  private static final CSVFormat CSV_FORMAT = CSVFormat.EXCEL.withHeader();

  private static final String DRG =
      TransformerUtilsV2.calculateVariableReferenceUrl(CcwCodebookVariable.CLM_DRG_CD);

  protected final List<String> drgCodes;
  protected final List<String> cptCodes;
  protected final List<String> icd9ProcedureCodes;
  protected final List<String> icd9DiagnosisCodes;
  protected final List<String> icd10ProcedureCodes;
  protected final List<String> icd10DiagnosisCodes;

  /**
   * Constructs a new {@link BaseSamhsaMatcher}, loading the lists of SAMHSA-related codes from the
   * classpath.
   */
  protected BaseSamhsaMatcher() {
    this.drgCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList("samhsa-related-codes/codes-drg.csv", "MS-DRGs").stream()
                .map(BaseSamhsaMatcher::normalizeDrgCode)
                .collect(Collectors.toList()));
    this.cptCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList("samhsa-related-codes/codes-cpt.csv", "CPT Code"));
    this.icd9ProcedureCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList("samhsa-related-codes/codes-icd-9-procedure.csv", "ICD-9-CM")
                .stream()
                .map(BaseSamhsaMatcher::normalizeIcdCode)
                .collect(Collectors.toList()));
    this.icd9DiagnosisCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList(
                    "samhsa-related-codes/codes-icd-9-diagnosis.csv", "ICD-9-CM Diagnosis Code")
                .stream()
                .map(BaseSamhsaMatcher::normalizeIcdCode)
                .collect(Collectors.toList()));
    this.icd10ProcedureCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList(
                    "samhsa-related-codes/codes-icd-10-procedure.csv", "ICD-10-PCS Code")
                .stream()
                .map(BaseSamhsaMatcher::normalizeIcdCode)
                .collect(Collectors.toList()));
    this.icd10DiagnosisCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList(
                    "samhsa-related-codes/codes-icd-10-diagnosis.csv", "ICD-10-CM Diagnosis Code")
                .stream()
                .map(BaseSamhsaMatcher::normalizeIcdCode)
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
        CSVParser csvParser = new CSVParser(csvReader, CSV_FORMAT)) {
      List<String> columnValues = new ArrayList<>();
      csvParser.forEach(r -> columnValues.add(r.get(columnToReturn)));
      return columnValues;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * @param procedureConcept the procedure {@link CodeableConcept} to check
   * @return <code>true</code> if the specified procedure {@link CodeableConcept} contains any
   *     {@link Coding}s that match any of the {@link #cptCodes}, <code>false</code> if they all do
   *     not
   */
  protected boolean containsSamhsaProcedureCode(CodeableConcept procedureConcept) {
    for (Coding procedureCoding : procedureConcept.getCoding()) {
      if (TransformerConstants.CODING_SYSTEM_HCPCS.equals(procedureCoding.getSystem())) {
        if (isSamhsaCptCode(procedureCoding)) return true;
      } else {
        /*
         * Fail safe: if we don't know the procedure Coding system, assume the code is
         * SAMHSA.
         */
        return true;
      }
    }

    // No blacklisted procedure Codings found: this procedure isn't SAMHSA-related.
    return false;
  }

  protected boolean isSamhsaPackageCode(CodeableConcept packageConcept) {
    if (packageConcept != null) {
      for (Coding packageCoding : packageConcept.getCoding()) {
        if (BaseSamhsaMatcher.DRG.equals(packageCoding.getSystem())) {
          if (isSamhsaDrgCode(packageCoding)) return true;
        } else {
          // Fail safe: if we don't know the package coding system, assume the code is
          // SAMHSA.
          return true;
        }
      }
    }

    return false;
  }

  /**
   * @param coding the code {@link Coding} to check
   * @return <code>true</code> if the specified code {@link Coding} matches one of the {@link
   *     #drgCodes} entries, <code>false</code> if it does not
   */
  private boolean isSamhsaDrgCode(Coding coding) {
    // Per the CCW Codebook DRG codes in the CCW are already normalized to the 3
    // digit code.
    return coding.getCode() != null && drgCodes.contains(coding.getCode());
  }

  /**
   * @param coding the diagnosis {@link Coding} to check
   * @return <code>true</code> if the specified diagnosis {@link Coding} matches one of the {@link
   *     BaseSamhsaMatcher#icd9DiagnosisCodes} entries, <code>false</code> if it does not
   */
  private boolean isSamhsaIcd9Diagnosis(Coding coding) {
    /*
     * Note: per XXX all codes in icd9DiagnosisCodes are already normalized.
     */
    return coding.getCode() != null
        && icd9DiagnosisCodes.contains(normalizeIcdCode(coding.getCode()));
  }

  /**
   * @param coding the procedure {@link Coding} to check
   * @return <code>true</code> if the specified procedure {@link Coding} matches one of the {@link
   *     #icd9ProcedureCodes} entries, <code>false</code> if it does not
   */
  private boolean isSamhsaIcd9Procedure(Coding coding) {
    return coding.getCode() != null
        && icd9ProcedureCodes.contains(normalizeIcdCode(coding.getCode()));
  }

  /**
   * @param coding the diagnosis {@link Coding} to check
   * @return <code>true</code> if the specified diagnosis {@link Coding} matches one of the {@link
   *     BaseSamhsaMatcher#icd10DiagnosisCodes} entries, <code>false</code> if it does not
   */
  private boolean isSamhsaIcd10Diagnosis(Coding coding) {
    /*
     * Note: per XXX all codes in icd10DiagnosisCodes are already normalized.
     */
    return coding.getCode() != null
        && icd10DiagnosisCodes.contains(normalizeIcdCode(coding.getCode()));
  }

  private boolean isSamhsaIcd10Procedure(Coding coding) {
    return coding.getCode() != null
        && icd10ProcedureCodes.contains(normalizeIcdCode(coding.getCode()));
  }

  /**
   * @param coding the procedure {@link Coding} to check
   * @return <code>true</code> if the specified procedure {@link Coding} matches one of the {@link
   *     BaseSamhsaMatcher#cptCodes} entries, <code>false</code> if it does not
   */
  protected boolean isSamhsaCptCode(Coding coding) {
    /*
     * Note: per XXX all codes in icd10DiagnosisCodes are already normalized.
     */
    return coding.getCode() != null && cptCodes.contains(normalizeHcpcsCode(coding.getCode()));
  }

  protected boolean isSamhsaDiagnosis(CodeableConcept concept) {
    return isSamhsaCoding(concept, this::isSamhsaIcd9Diagnosis, this::isSamhsaIcd10Diagnosis);
  }

  protected boolean isSamhsaIcdProcedure(CodeableConcept concept) {
    return isSamhsaCoding(concept, this::isSamhsaIcd9Procedure, this::isSamhsaIcd10Procedure);
  }

  private boolean isSamhsaCoding(
      CodeableConcept concept,
      final Predicate<Coding> icd9Check,
      final Predicate<Coding> icd10Check) {
    boolean containsSamhsa = false;

    if (concept != null) {
      containsSamhsa =
          concept.getCoding().stream()
              .anyMatch(
                  coding -> {
                    if (IcdCode.CODING_SYSTEM_ICD_9.equals(coding.getSystem())) {
                      return icd9Check.test(coding);
                    } else if (IcdCode.CODING_SYSTEM_ICD_10.equals(coding.getSystem())) {
                      return icd10Check.test(coding);
                    } else if (TransformerConstants.CODING_SYSTEM_CPT.equals(coding.getSystem())) {
                      return isSamhsaCptCode(coding);
                    } else {
                      // Fail safe: if we don't know the ICD version, assume the code is SAMHSA.
                      return true;
                    }
                  });
    }

    return containsSamhsa;
  }

  /**
   * Example input: MS-DRG 522 Example output: 522
   *
   * @param code The drg code to normalize.
   * @return the specified DRG code, but with the "MS-DRG" prefix and space removed.
   */
  private static String normalizeDrgCode(String code) {
    code = code.trim();
    code = code.replace("MS-DRG ", "");
    return code;
  }

  /**
   * @param icdCode the ICD-9 or ICD-10 diagnosis code to normalize
   * @return the specified ICD-9 or ICD-10 code, but with whitespace trimmed, the first (if any)
   *     decimal point removed, and converted to all-caps
   */
  private static String normalizeIcdCode(String icdCode) {
    icdCode = icdCode.trim();
    icdCode = icdCode.replaceFirst("\\.", "");
    icdCode = icdCode.toUpperCase();

    return icdCode;
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
