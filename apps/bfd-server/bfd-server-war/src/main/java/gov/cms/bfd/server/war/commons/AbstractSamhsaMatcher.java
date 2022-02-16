package gov.cms.bfd.server.war.commons;

import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.server.war.adapters.CodeableConcept;
import gov.cms.bfd.server.war.adapters.Coding;
import gov.cms.bfd.server.war.adapters.DiagnosisComponent;
import gov.cms.bfd.server.war.adapters.ItemComponent;
import gov.cms.bfd.server.war.adapters.ProcedureComponent;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.hl7.fhir.exceptions.FHIRException;

/**
 * Common SAMHSA check logic that can be used by all current FHIR resources and resource verions.
 *
 * <p>The common logic is based off abstract wrappers ({@link gov.cms.bfd.server.war.adapters}) that
 * make the checks agnostic toward the specific resource type or version.
 *
 * <p>Individual implementations of this class will define specifically which resource/version is
 * being checked, and what attributes are checked for it.
 *
 * @param <T> The FHIR resource type being checked.
 */
public abstract class AbstractSamhsaMatcher<T> implements Predicate<T> {

  /** The {@link CSVFormat} used to parse the SAMHSA-related code CSV files. */
  private static final CSVFormat CSV_FORMAT = CSVFormat.EXCEL.withHeader();

  protected static final String DRG =
      CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.CLM_DRG_CD);

  private final Set<String> drgCodes;
  private final Set<String> cptCodes;
  private final Set<String> icd9ProcedureCodes;
  private final Set<String> icd9DiagnosisCodes;
  private final Set<String> icd10ProcedureCodes;
  private final Set<String> icd10DiagnosisCodes;

  /**
   * Constructs a new {@link AbstractSamhsaMatcher}, loading the lists of SAMHSA-related codes from
   * the classpath. The list data is normalized as it is loaded.
   */
  protected AbstractSamhsaMatcher() {
    this.drgCodes =
        resourceCsvColumnToList("samhsa-related-codes/codes-drg.csv", "MS-DRGs").stream()
            .map(AbstractSamhsaMatcher::normalizeDrgCode)
            .collect(Collectors.toUnmodifiableSet());
    this.cptCodes =
        resourceCsvColumnToList("samhsa-related-codes/codes-cpt.csv", "CPT Code").stream()
            .map(AbstractSamhsaMatcher::normalizeHcpcsCode)
            .collect(Collectors.toUnmodifiableSet());
    this.icd9ProcedureCodes =
        resourceCsvColumnToList("samhsa-related-codes/codes-icd-9-procedure.csv", "ICD-9-CM")
            .stream()
            .map(AbstractSamhsaMatcher::normalizeIcdCode)
            .collect(Collectors.toUnmodifiableSet());
    this.icd9DiagnosisCodes =
        resourceCsvColumnToList(
                "samhsa-related-codes/codes-icd-9-diagnosis.csv", "ICD-9-CM Diagnosis Code")
            .stream()
            .map(AbstractSamhsaMatcher::normalizeIcdCode)
            .collect(Collectors.toUnmodifiableSet());
    this.icd10ProcedureCodes =
        resourceCsvColumnToList(
                "samhsa-related-codes/codes-icd-10-procedure.csv", "ICD-10-PCS Code")
            .stream()
            .map(AbstractSamhsaMatcher::normalizeIcdCode)
            .collect(Collectors.toUnmodifiableSet());
    this.icd10DiagnosisCodes =
        resourceCsvColumnToList(
                "samhsa-related-codes/codes-icd-10-diagnosis.csv", "ICD-10-CM Diagnosis Code")
            .stream()
            .map(AbstractSamhsaMatcher::normalizeIcdCode)
            .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Pulls codes from the given column of the given file, returning them as a list.
   *
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
   * Checks if the given {@link ProcedureComponent} list contains any SAMHSA data.
   *
   * @param procedure the {@link ProcedureComponent}s to check
   * @return <code>true</code> if any of the specified {@link ProcedureComponent}s match any of the
   *     {@link AbstractSamhsaMatcher#icd9ProcedureCodes} or {@link
   *     AbstractSamhsaMatcher#icd10ProcedureCodes} entries, <code>false</code> if they all do not
   */
  protected boolean containsSamhsaIcdProcedureCode(List<ProcedureComponent> procedure) {
    return procedure.stream().anyMatch(this::isSamhsaIcdProcedure);
  }

  /**
   * Checks if the given {@link DiagnosisComponent} list contains any SAMHSA data.
   *
   * @param diagnoses the {@link DiagnosisComponent}s to check
   * @return <code>true</code> if any of the specified {@link DiagnosisComponent}s match any of the
   *     {@link AbstractSamhsaMatcher#icd9DiagnosisCodes} or {@link
   *     AbstractSamhsaMatcher#icd10DiagnosisCodes} entries, <code>false</code> if they all do not
   */
  protected boolean containsSamhsaIcdDiagnosisCode(List<DiagnosisComponent> diagnoses) {
    return diagnoses.stream().anyMatch(this::isSamhsaDiagnosis);
  }

  /**
   * Checks if the given {@link ItemComponent} list contains any SAMHSA data.
   *
   * @param items The {@link ItemComponent} list to check
   * @return <code>true</code> if any {@link ItemComponent} contains SAMHSA data, <code>false</code>
   *     otherwise.
   */
  protected boolean containsSamhsaLineItem(List<ItemComponent> items) {
    return items.stream().anyMatch(c -> containsSamhsaProcedureCode(c.getProductOrService()));
  }

  /**
   * Checks if the given {@link ProcedureComponent} contains SAMHSA data.
   *
   * @param procedure the {@link ProcedureComponent} to check
   * @return <code>true</code> if the specified {@link ProcedureComponent} matches one of the {@link
   *     AbstractSamhsaMatcher#icd9ProcedureCodes} or {@link
   *     AbstractSamhsaMatcher#icd10ProcedureCodes} entries, <code>false</code> if it does not
   */
  @VisibleForTesting
  boolean isSamhsaIcdProcedure(ProcedureComponent procedure) {
    try {
      return isSamhsaIcdProcedure(procedure.getProcedureCodeableConcept());
    } catch (FHIRException e) {
      /*
       * This will only be thrown if the ProcedureComponent doesn't have a
       * CodeableConcept, which isn't how we build ours.
       */
      throw new BadCodeMonkeyException(e);
    }
  }

  /**
   * Checks if the given {@link DiagnosisComponent} contains SAMHSA data.
   *
   * @param diagnosis the {@link DiagnosisComponent} to check
   * @return <code>true</code> if the specified {@link DiagnosisComponent} matches one of the {@link
   *     AbstractSamhsaMatcher#icd9DiagnosisCodes} or {@link
   *     AbstractSamhsaMatcher#icd10DiagnosisCodes}, or {@link AbstractSamhsaMatcher#drgCodes}
   *     entries, <code>
   *     false</code> if it does not
   */
  @VisibleForTesting
  boolean isSamhsaDiagnosis(DiagnosisComponent diagnosis) {
    try {
      return isSamhsaDiagnosis(diagnosis.getDiagnosisCodeableConcept())
          || isSamhsaPackageCode(diagnosis.getPackageCode());
    } catch (FHIRException e) {
      /*
       * This will only be thrown if the DiagnosisComponent doesn't have a
       * CodeableConcept, which isn't how we build ours.
       */
      throw new BadCodeMonkeyException(e);
    }
  }

  /**
   * Checks that for the specified {@link CodeableConcept}, the Codings (if any) within, contain a
   * blacklisted SAMHSA procedure code. If any of the systems within the {@link CodeableConcept} are
   * not known/expected, it assumes the system is SAMHSA and returns {@code true} as a safety
   * fallback.
   *
   * @param procedureConcept the procedure {@link CodeableConcept} to check
   * @return <code>true</code> if the specified procedure {@link CodeableConcept} contains any
   *     {@link Coding}s that match any of the {@link #cptCodes} or has unknown coding systems,
   *     <code>false</code> otherwise.
   */
  protected boolean containsSamhsaProcedureCode(CodeableConcept procedureConcept) {
    return !procedureConcept.getCoding().isEmpty()
        && (hasHcpcsSystemAndSamhsaCptCode(procedureConcept)
            || !containsOnlyKnownSystems(procedureConcept));
  }

  /**
   * Checks that for a {@link CodeableConcept} the {@link Coding}s contain a HCPCS system and at
   * least one blacklisted CPT code.
   *
   * <p>CPT codes are a subset of HCPCS codes, but they are the only ones we blacklist
   *
   * <p>If there is no HCPCS system, it may be a DME claim, which should return false for SAMHSA
   *
   * @param procedureConcept the procedure {@link CodeableConcept} to check
   * @return <code>true</code> if the specified procedure {@link CodeableConcept} contains a HCPCS
   *     system as well as a CPT samhsa coding, (matched with {@link #cptCodes}), <code>false</code>
   *     otherwise.
   */
  @VisibleForTesting
  boolean hasHcpcsSystemAndSamhsaCptCode(CodeableConcept procedureConcept) {
    return procedureConcept.getCoding().stream()
            .anyMatch(code -> TransformerConstants.CODING_SYSTEM_HCPCS.equals(code.getSystem()))
        && procedureConcept.getCoding().stream().anyMatch(this::isSamhsaCptCode);
  }

  /**
   * Checks if the given {@link CodeableConcept} contains only known coding systems.
   *
   * @param procedureConcept the procedure {@link CodeableConcept} to check
   * @return <code>true</code> if the specified procedure {@link CodeableConcept} contains at least
   *     one {@link Coding} and only contains {@link Coding}s that have known coding systems, <code>
   *     false</code> otherwise
   */
  protected abstract boolean containsOnlyKnownSystems(CodeableConcept procedureConcept);

  /**
   * Checks if the given {@link CodeableConcept} contains any SAMHSA data.
   *
   * @param packageConcept The {@link CodeableConcept} to check.
   * @return <code>true</code> if the given {@link CodeableConcept} is not null and has any non-DRG
   *     codes or SAMHSA DRG codes. <code>false</code> otherwise.
   */
  protected boolean isSamhsaPackageCode(CodeableConcept packageConcept) {
    return packageConcept != null
        && packageConcept.getCoding().stream()
            .anyMatch(
                coding ->
                    !AbstractSamhsaMatcher.DRG.equals(coding.getSystem())
                        || isSamhsaDrgCode(coding));
  }

  /**
   * Checks if the given {@link Coding} contains a SAMHSA DRG Code
   *
   * @param coding the code {@link Coding} to check
   * @return <code>true</code> if the specified code {@link Coding} matches one of the {@link
   *     #drgCodes} entries, <code>false</code> if it does not
   * @throws IllegalArgumentException if the given {@link Coding} system is not DRG
   */
  @VisibleForTesting
  boolean isSamhsaDrgCode(Coding coding) {
    return isSamhsaCodingForSystem(coding, drgCodes, AbstractSamhsaMatcher.DRG);
  }

  /**
   * Checks if the given {@link Coding} contains a SAMHSA ICD9 Diagnosis Code
   *
   * @param coding the diagnosis {@link Coding} to check
   * @return <code>true</code> if the specified diagnosis {@link Coding} matches one of the {@link
   *     AbstractSamhsaMatcher#icd9DiagnosisCodes} entries, <code>false</code> if it does not
   * @throws IllegalArgumentException if the given {@link Coding} system is not ICD9
   */
  @VisibleForTesting
  boolean isSamhsaIcd9Diagnosis(Coding coding) {
    return isSamhsaCodingForSystem(coding, icd9DiagnosisCodes, IcdCode.CODING_SYSTEM_ICD_9);
  }

  /**
   * Checks if the given {@link Coding} contains a SAMHSA ICD9 Procedure Code
   *
   * @param coding the procedure {@link Coding} to check
   * @return <code>true</code> if the specified procedure {@link Coding} matches one of the {@link
   *     #icd9ProcedureCodes} entries, <code>false</code> if it does not
   * @throws IllegalArgumentException if the given {@link Coding} system is not ICD9
   */
  @VisibleForTesting
  boolean isSamhsaIcd9Procedure(Coding coding) {
    return isSamhsaCodingForSystem(coding, icd9ProcedureCodes, IcdCode.CODING_SYSTEM_ICD_9);
  }

  /**
   * Checks if the given {@link Coding} contains a SAMHSA ICD10 Diagnosis Code
   *
   * @param coding the diagnosis {@link Coding} to check
   * @return <code>true</code> if the specified diagnosis {@link Coding} matches one of the {@link
   *     AbstractSamhsaMatcher#icd10DiagnosisCodes} entries, <code>false</code> if it does not
   * @throws IllegalArgumentException if the given {@link Coding} system is not ICD10
   */
  @VisibleForTesting
  boolean isSamhsaIcd10Diagnosis(Coding coding) {
    return isSamhsaCodingForSystem(coding, icd10DiagnosisCodes, IcdCode.CODING_SYSTEM_ICD_10);
  }

  /**
   * Checks if the given {@link Coding} contains a SAMHSA ICD10 Procedure Code
   *
   * @param coding the diagnosis {@link Coding} to check
   * @return <code>true</code> if the specified precedure {@link Coding} matches one of the {@link
   *     AbstractSamhsaMatcher#icd10ProcedureCodes} entries, <code>false</code> if it does not
   * @throws IllegalArgumentException if the given {@link Coding} system is not ICD10
   */
  @VisibleForTesting
  boolean isSamhsaIcd10Procedure(Coding coding) {
    return isSamhsaCodingForSystem(coding, icd10ProcedureCodes, IcdCode.CODING_SYSTEM_ICD_10);
  }

  /**
   * Checks if the given {@link Coding} is in the given {@link Set<String>} of SAMHSA codes.
   *
   * @param coding The {@link Coding} to check.
   * @param samhsaCodes The {@link Set<String>} of defined SAMHSA codes to compare against.
   * @param requireeSystem The expected {@link Coding} system of the given {@link Coding}.
   * @return <code>true</code> if the given {@link Set<String>} of SAMHSA codes includes the given
   *     {@link Coding} code. <code>false</code> otherwise.
   * @throws IllegalArgumentException if the given {@link Coding} system is not the same as the
   *     given requiredSystem
   */
  @VisibleForTesting
  boolean isSamhsaCodingForSystem(Coding coding, Set<String> samhsaCodes, String requireeSystem) {
    if (!requireeSystem.equals(coding.getSystem())) {
      throw new IllegalArgumentException("Illegal coding system: '" + coding.getSystem() + "'");
    }

    return coding.getCode() != null && samhsaCodes.contains(normalizeIcdCode(coding.getCode()));
  }

  /**
   * Checks if the given {@link Coding} contains a SAMHSA CPT code.
   *
   * @param coding the procedure {@link Coding} to check
   * @return <code>true</code> if the specified procedure {@link Coding} matches one of the {@link
   *     AbstractSamhsaMatcher#cptCodes} entries, <code>false</code> if it does not or is null.
   */
  protected boolean isSamhsaCptCode(Coding coding) {
    /*
     * Note: per XXX all codes in icd10DiagnosisCodes are already normalized.
     */
    return coding.getCode() != null && cptCodes.contains(normalizeHcpcsCode(coding.getCode()));
  }

  /**
   * Checks if the given {@link CodeableConcept} contains SAMHSA diagnosis data.
   *
   * <p>ICD version specific codes are checked based on the given {@link CodeableConcept}'s {@link
   * Coding} system.
   *
   * @param concept The diagnosis related {@link CodeableConcept} to check.
   * @return <code>true</code> if the given {@link CodeableConcept} contains diagnosis related
   *     SAMHSA data, <code>false</code> otherwise.
   */
  protected boolean isSamhsaDiagnosis(CodeableConcept concept) {
    return isSamhsaCoding(concept, this::isSamhsaIcd9Diagnosis, this::isSamhsaIcd10Diagnosis);
  }

  /**
   * Checks if the given {@link CodeableConcept} contains SAMHSA procedure data.
   *
   * <p>ICD version specific codes are checked based on the given {@link CodeableConcept}'s {@link
   * Coding} system.
   *
   * @param concept The procedure related {@link CodeableConcept} to check.
   * @return <code>true</code> if the given {@link CodeableConcept} contains procedure related
   *     SAMHSA data, <code>false</code> otherwise.
   */
  protected boolean isSamhsaIcdProcedure(CodeableConcept concept) {
    return isSamhsaCoding(concept, this::isSamhsaIcd9Procedure, this::isSamhsaIcd10Procedure);
  }

  /**
   * Checks if the given {@link CodeableConcept} contains SAMHSA data based on it's inner {@link
   * Coding}s system types.
   *
   * @param concept The {@link CodeableConcept} to check.
   * @param icd9Check The ICD-9 based SAMHSA check logic to use.
   * @param icd10Check The ICD-10 based SAMHSA check logic to use.
   * @return <code>true</code> if the given {@link CodeableConcept} contains any {@link Coding}s
   *     with SAMHSA data for it's respective system, or if the system is not ICD9/10. <code>false
   *     </code> otherwise.
   */
  @VisibleForTesting
  boolean isSamhsaCoding(
      CodeableConcept concept,
      final Predicate<Coding> icd9Check,
      final Predicate<Coding> icd10Check) {
    boolean containsSamhsa = false;

    if (concept != null && concept.getCoding() != null) {
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
   * Normalizes the DRG code to make for easier and more consistent comparisons.
   *
   * <p>Example input: MS-DRG 522 Example output: 522
   *
   * @param code The drg code to normalize.
   * @return the specified DRG code, but with the "MS-DRG" prefix and space removed.
   */
  @VisibleForTesting
  static String normalizeDrgCode(String code) {
    code = code.trim();
    code = code.replace("MS-DRG ", "");
    return code;
  }

  /**
   * Normalizes the ICD code to make for easier and more consistent comparisons.
   *
   * @param icdCode the ICD-9 or ICD-10 diagnosis code to normalize
   * @return the specified ICD-9 or ICD-10 code, but with whitespace trimmed, the first (if any)
   *     decimal point removed, and converted to all-caps
   */
  @VisibleForTesting
  static String normalizeIcdCode(String icdCode) {
    icdCode = icdCode.trim();
    icdCode = icdCode.replaceFirst("\\.", "");
    icdCode = icdCode.toUpperCase();

    return icdCode;
  }

  /**
   * Normalizes the HCPCS code to make for easier and more consistent comparisons.
   *
   * @param hcpcsCode the HCPCS code to normalize
   * @return the specified HCPCS code, but with whitespace trimmed and converted to all-caps
   */
  @VisibleForTesting
  static String normalizeHcpcsCode(String hcpcsCode) {
    hcpcsCode = hcpcsCode.trim();
    hcpcsCode = hcpcsCode.toUpperCase();

    return hcpcsCode;
  }
}
