package gov.cms.bfd.server.war.r4.providers;

import com.google.common.annotations.VisibleForTesting;
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
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

public abstract class AbstractSamhsaMatcher<T> implements Predicate<T> {

  /** The {@link CSVFormat} used to parse the SAMHSA-related code CSV files. */
  private static final CSVFormat CSV_FORMAT = CSVFormat.EXCEL.withHeader();

  protected static final String DRG =
      TransformerUtilsV2.calculateVariableReferenceUrl(CcwCodebookVariable.CLM_DRG_CD);

  protected final List<String> drgCodes;
  protected final List<String> cptCodes;
  protected final List<String> icd9ProcedureCodes;
  protected final List<String> icd9DiagnosisCodes;
  protected final List<String> icd10ProcedureCodes;
  protected final List<String> icd10DiagnosisCodes;

  /**
   * Constructs a new {@link AbstractSamhsaMatcher}, loading the lists of SAMHSA-related codes from
   * the classpath.
   */
  protected AbstractSamhsaMatcher() {
    this.drgCodes =
        resourceCsvColumnToList("samhsa-related-codes/codes-drg.csv", "MS-DRGs").stream()
            .map(AbstractSamhsaMatcher::normalizeDrgCode)
            .collect(Collectors.toUnmodifiableList());
    this.cptCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList("samhsa-related-codes/codes-cpt.csv", "CPT Code"));
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
   * Checks that for the specified {@link CodeableConcept}, the Codings (if any) within, contain a
   * blacklisted MHSA procedure code. If any of the systemms within th {@link CodeableConcept} are
   * not known/expected, returns {@code true} and assuems the system is SAMHSA as a safety fallback.
   *
   * @param procedureConcept the procedure {@link CodeableConcept} to check
   * @return <code>true</code> if the specified procedure {@link CodeableConcept} contains any
   *     {@link Coding}s that match any of the {@link #cptCodes} or has unkonwn coding systems,
   *     <code>false</code> otherwise.
   */
  protected boolean containsSamhsaProcedureCode(CodeableConcept procedureConcept) {
    return !procedureConcept.getCoding().isEmpty()
        && (hasHcpcsAndSamhsaCptCode(procedureConcept)
            || !containsOnlyKnownSystems(procedureConcept));
  }

  /**
   * Checks if the given concept contains any HCPCS system as well as a CPT samhsa code.
   *
   * @param procedureConcept the procedure {@link CodeableConcept} to check
   * @return <code>true</code> if the specified procedure {@link CodeableConcept} contains a HCPCS
   *     system as well as a CPT samhsa coding, (matched with {@link #cptCodes}), <code>false</code>
   *     otherwise.
   */
  private boolean hasHcpcsAndSamhsaCptCode(CodeableConcept procedureConcept) {
    /*
     * Note: CPT codes represent a subset of possible HCPCS codes (but are the only
     * subset that we blacklist from).
     */
    Set<String> codingSystems =
        procedureConcept.getCoding().stream().map(Coding::getSystem).collect(Collectors.toSet());

    return codingSystems.contains(TransformerConstants.CODING_SYSTEM_HCPCS)
        && procedureConcept.getCoding().stream().anyMatch(this::isSamhsaCptCode);
  }

  /**
   * @param procedureConcept the procedure {@link CodeableConcept} to check
   * @return <code>true</code> if the specified procedure {@link CodeableConcept} contains at least
   *     one {@link Coding} and only contains {@link Coding}s that have known coding systems <code>
   *     false</code> otherwise
   */
  private boolean containsOnlyKnownSystems(CodeableConcept procedureConcept) {
    Set<String> codingSystems =
        procedureConcept.getCoding().stream().map(Coding::getSystem).collect(Collectors.toSet());

    String hcpcsCdSystem =
        TransformerUtilsV2.calculateVariableReferenceUrl(CcwCodebookVariable.HCPCS_CD);

    // Valid system url for productOrService coding
    Set<String> hcpcsSystem = Set.of(TransformerConstants.CODING_SYSTEM_HCPCS);

    // Additional valid coding system URL for backwards-compatibility
    // See: https://jira.cms.gov/browse/BFD-1345
    Set<String> backwardsCompatibleHcpcsSystems =
        Set.of(TransformerConstants.CODING_SYSTEM_HCPCS, hcpcsCdSystem);

    return codingSystems.equals(hcpcsSystem)
        || codingSystems.equals(backwardsCompatibleHcpcsSystems);
  }

  protected boolean isSamhsaPackageCode(CodeableConcept packageConcept) {
    return packageConcept != null
        && packageConcept.getCoding().stream()
            .anyMatch(
                coding ->
                    !AbstractSamhsaMatcher.DRG.equals(coding.getSystem())
                        || isSamhsaDrgCode(coding));
  }

  /**
   * @param coding the code {@link Coding} to check
   * @return <code>true</code> if the specified code {@link Coding} matches one of the {@link
   *     #drgCodes} entries, <code>false</code> if it does not
   */
  @VisibleForTesting
  boolean isSamhsaDrgCode(Coding coding) {
    return isSamhsaCodingForSystem(coding, drgCodes, AbstractSamhsaMatcher.DRG);
  }

  /**
   * @param coding the diagnosis {@link Coding} to check
   * @return <code>true</code> if the specified diagnosis {@link Coding} matches one of the {@link
   *     AbstractSamhsaMatcher#icd9DiagnosisCodes} entries, <code>false</code> if it does not
   */
  @VisibleForTesting
  boolean isSamhsaIcd9Diagnosis(Coding coding) {
    return isSamhsaCodingForSystem(coding, icd9DiagnosisCodes, IcdCode.CODING_SYSTEM_ICD_9);
  }

  /**
   * @param coding the procedure {@link Coding} to check
   * @return <code>true</code> if the specified procedure {@link Coding} matches one of the {@link
   *     #icd9ProcedureCodes} entries, <code>false</code> if it does not
   */
  @VisibleForTesting
  boolean isSamhsaIcd9Procedure(Coding coding) {
    return isSamhsaCodingForSystem(coding, icd9ProcedureCodes, IcdCode.CODING_SYSTEM_ICD_9);
  }

  /**
   * @param coding the diagnosis {@link Coding} to check
   * @return <code>true</code> if the specified diagnosis {@link Coding} matches one of the {@link
   *     AbstractSamhsaMatcher#icd10DiagnosisCodes} entries, <code>false</code> if it does not
   */
  @VisibleForTesting
  boolean isSamhsaIcd10Diagnosis(Coding coding) {
    return isSamhsaCodingForSystem(coding, icd10DiagnosisCodes, IcdCode.CODING_SYSTEM_ICD_10);
  }

  /**
   * @param coding the diagnosis {@link Coding} to check
   * @return <code>true</code> if the specified precedure {@link Coding} matches one of the {@link
   *     AbstractSamhsaMatcher#icd10ProcedureCodes} entries, <code>false</code> if it does not
   */
  @VisibleForTesting
  boolean isSamhsaIcd10Procedure(Coding coding) {
    return isSamhsaCodingForSystem(coding, icd10ProcedureCodes, IcdCode.CODING_SYSTEM_ICD_10);
  }

  @VisibleForTesting
  boolean isSamhsaCodingForSystem(Coding coding, List<String> samhsaCodes, String system) {
    if (!system.equals(coding.getSystem())) {
      throw new IllegalArgumentException("Illegal coding system: '" + coding.getSystem() + "'");
    }

    return coding.getCode() != null && samhsaCodes.contains(normalizeIcdCode(coding.getCode()));
  }

  /**
   * @param coding the procedure {@link Coding} to check
   * @return <code>true</code> if the specified procedure {@link Coding} matches one of the {@link
   *     AbstractSamhsaMatcher#cptCodes} entries, <code>false</code> if it does not
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

  @VisibleForTesting
  boolean isSamhsaCoding(
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
