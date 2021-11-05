package gov.cms.bfd.server.war.r4.providers;

import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.IcdCode;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
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
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.DiagnosisComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ProcedureComponent;
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
public final class R4SamhsaMatcher implements Predicate<ExplanationOfBenefit> {
  /** The {@link CSVFormat} used to parse the SAMHSA-related code CSV files. */
  private static final CSVFormat CSV_FORMAT = CSVFormat.EXCEL.withHeader();

  private static final String DRG =
      CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.CLM_DRG_CD);

  private final List<String> drgCodes;
  private final List<String> cptCodes;
  private final List<String> icd9ProcedureCodes;
  private final List<String> icd9DiagnosisCodes;
  private final List<String> icd10ProcedureCodes;
  private final List<String> icd10DiagnosisCodes;

  /**
   * Constructs a new {@link R4SamhsaMatcher}, loading the lists of SAMHSA-related codes from the
   * classpath.
   */
  public R4SamhsaMatcher() {
    this.drgCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList("samhsa-related-codes/codes-drg.csv", "MS-DRGs").stream()
                .map(R4SamhsaMatcher::normalizeDrgCode)
                .collect(Collectors.toList()));
    this.cptCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList("samhsa-related-codes/codes-cpt.csv", "CPT Code"));
    this.icd9ProcedureCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList("samhsa-related-codes/codes-icd-9-procedure.csv", "ICD-9-CM")
                .stream()
                .map(R4SamhsaMatcher::normalizeIcd9Code)
                .collect(Collectors.toList()));
    this.icd9DiagnosisCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList(
                    "samhsa-related-codes/codes-icd-9-diagnosis.csv", "ICD-9-CM Diagnosis Code")
                .stream()
                .map(R4SamhsaMatcher::normalizeIcd9Code)
                .collect(Collectors.toList()));
    this.icd10ProcedureCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList(
                    "samhsa-related-codes/codes-icd-10-procedure.csv", "ICD-10-PCS Code")
                .stream()
                .map(R4SamhsaMatcher::normalizeIcd10Code)
                .collect(Collectors.toList()));
    this.icd10DiagnosisCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList(
                    "samhsa-related-codes/codes-icd-10-diagnosis.csv", "ICD-10-CM Diagnosis Code")
                .stream()
                .map(R4SamhsaMatcher::normalizeIcd10Code)
                .collect(Collectors.toList()));
  }

  /**
   * @param csvResourceName the classpath resource name of the CSV file to parse
   * @param columnToReturn the name of the column to return from the CSV file
   * @return a {@link List} of values from the specified column of the specified CSV file
   */
  private static List<String> resourceCsvColumnToList(
      String csvResourceName, String columnToReturn) {
    CSVParser csvParser = null;
    try (InputStream csvStream =
            Thread.currentThread().getContextClassLoader().getResourceAsStream(csvResourceName);
        InputStreamReader csvReader = new InputStreamReader(csvStream, StandardCharsets.UTF_8); ) {
      csvParser = new CSVParser(csvReader, CSV_FORMAT);
      List<String> columnValues = new ArrayList<>();
      csvParser.forEach(
          record -> {
            columnValues.add(record.get(columnToReturn));
          });
      return columnValues;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      if (csvParser != null) {
        try {
          csvParser.close();
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
    }
  }

  /** @see java.util.function.Predicate#test(java.lang.Object) */
  @Override
  public boolean test(ExplanationOfBenefit eob) {
    ClaimTypeV2 claimType = TransformerUtilsV2.getClaimType(eob);

    switch (TransformerUtilsV2.getClaimType(eob)) {
      case CARRIER:
      case DME:
        return testCarrierOrDmeClaim(eob);
      case HHA:
        return testHhaClaim(eob);
      case HOSPICE:
        return testHospiceClaim(eob);
      case INPATIENT:
        return testInpatientClaim(eob);
      case OUTPATIENT:
        return testOutpatientClaim(eob);
      case SNF:
        return testSnfClaim(eob);
      case PDE:
        return testPartDEvent(eob);
      default:
        throw new BadCodeMonkeyException("Unsupported claim type: " + claimType);
    }
  }

  /**
   * @param eob the {@link ClaimType#SNF} {@link ExplanationOfBenefit} to check
   * @return <code>true</code> if the specified {@link ClaimType#SNF} {@link ExplanationOfBenefit}
   *     contains any known-SAMHSA-related codes, <code>false</code> if it does not
   */
  private boolean testSnfClaim(ExplanationOfBenefit eob) {
    if (TransformerUtilsV2.getClaimType(eob) != ClaimTypeV2.SNF) {
      throw new IllegalArgumentException();
    }

    if (containsSamhsaIcdCode(eob.getDiagnosis())) {
      return true;
    }

    if (containsSamhsaIcdProcedueCode(eob.getProcedure())) {
      return true;
    }

    for (ExplanationOfBenefit.ItemComponent eobItem : eob.getItem()) {
      if (containsSamhsaProcedureCode(eobItem.getProductOrService())) {
        return true;
      }
    }

    // No blacklisted codes found: this claim isn't SAMHSA-related.
    return false;
  }

  /**
   * @param eob the {@link ClaimType#OUTPATIENT} {@link ExplanationOfBenefit} to check
   * @return <code>true</code> if the specified {@link ClaimType#OUTPATIENT} {@link
   *     ExplanationOfBenefit} contains any known-SAMHSA-related codes, <code>false</code> if it
   *     does not
   */
  private boolean testOutpatientClaim(ExplanationOfBenefit eob) {
    if (TransformerUtilsV2.getClaimType(eob) != ClaimTypeV2.OUTPATIENT) {
      throw new IllegalArgumentException();
    }

    if (containsSamhsaIcdCode(eob.getDiagnosis())) {
      return true;
    }

    if (containsSamhsaIcdProcedueCode(eob.getProcedure())) {
      return true;
    }

    for (ExplanationOfBenefit.ItemComponent eobItem : eob.getItem()) {
      if (containsSamhsaProcedureCode(eobItem.getProductOrService())) {
        return true;
      }
    }

    // No blacklisted codes found: this claim isn't SAMHSA-related.
    return false;
  }

  /**
   * @param eob the {@link ClaimType#INPATIENT} {@link ExplanationOfBenefit} to check
   * @return <code>true</code> if the specified {@link ClaimType#INPATIENT} {@link
   *     ExplanationOfBenefit} contains any known-SAMHSA-related codes, <code>false</code> if it
   *     does not
   */
  private boolean testInpatientClaim(ExplanationOfBenefit eob) {
    if (TransformerUtilsV2.getClaimType(eob) != ClaimTypeV2.INPATIENT)
      throw new IllegalArgumentException();

    if (containsSamhsaIcdCode(eob.getDiagnosis())) {
      return true;
    }

    if (containsSamhsaIcdProcedueCode(eob.getProcedure())) {
      return true;
    }

    for (ExplanationOfBenefit.ItemComponent eobItem : eob.getItem()) {
      if (containsSamhsaProcedureCode(eobItem.getProductOrService())) {
        return true;
      }
    }

    // No blacklisted codes found: this claim isn't SAMHSA-related.
    return false;
  }

  /**
   * @param eob the {@link ClaimType#HOSPICE} {@link ExplanationOfBenefit} to check
   * @return <code>true</code> if the specified {@link ClaimType#HOSPICE} {@link
   *     ExplanationOfBenefit} contains any known-SAMHSA-related codes, <code>false</code> if it
   *     does not
   */
  private boolean testHospiceClaim(ExplanationOfBenefit eob) {
    if (TransformerUtilsV2.getClaimType(eob) != ClaimTypeV2.HOSPICE) {
      throw new IllegalArgumentException();
    }

    if (containsSamhsaIcdCode(eob.getDiagnosis())) {
      return true;
    }

    for (ExplanationOfBenefit.ItemComponent eobItem : eob.getItem()) {
      if (containsSamhsaProcedureCode(eobItem.getProductOrService())) {
        return true;
      }
    }

    // No blacklisted codes found: this claim isn't SAMHSA-related.
    return false;
  }

  /**
   * @param eob the {@link ClaimType#HHA} {@link ExplanationOfBenefit} to check
   * @return <code>true</code> if the specified {@link ClaimType#HHA} {@link ExplanationOfBenefit}
   *     contains any known-SAMHSA-related codes, <code>false</code> if it does not
   */
  private boolean testHhaClaim(ExplanationOfBenefit eob) {
    if (TransformerUtilsV2.getClaimType(eob) != ClaimTypeV2.HHA) {
      throw new IllegalArgumentException();
    }

    if (containsSamhsaIcdCode(eob.getDiagnosis())) return true;

    for (ExplanationOfBenefit.ItemComponent eobItem : eob.getItem()) {
      if (containsSamhsaProcedureCode(eobItem.getProductOrService())) return true;
    }

    // No blacklisted codes found: this claim isn't SAMHSA-related.
    return false;
  }

  /**
   * @param eob the {@link ClaimTypeV2#PDE} {@link ExplanationOfBenefit} to check
   * @return <code>true</code> if the specified {@link ClaimTypeV2#PDE} {@link ExplanationOfBenefit}
   *     contains any known-SAMHSA-related codes, <code>false</code> if it does not
   */
  private boolean testPartDEvent(ExplanationOfBenefit eob) {
    if (TransformerUtilsV2.getClaimType(eob) != ClaimTypeV2.PDE) {
      throw new IllegalArgumentException();
    }

    // There are no SAMHSA fields in PDE claims
    return false;
  }

  /**
   * @param eob the {@link ClaimType#CARRIER} {@link ExplanationOfBenefit} to check
   * @return <code>true</code> if the specified {@link ClaimType#CARRIER} {@link
   *     ExplanationOfBenefit} contains any known-SAMHSA-related codes, <code>false</code> if it
   *     does not
   */
  private boolean testCarrierOrDmeClaim(ExplanationOfBenefit eob) {
    if (!(TransformerUtilsV2.getClaimType(eob) == ClaimTypeV2.CARRIER
        || TransformerUtilsV2.getClaimType(eob) == ClaimTypeV2.DME)) {
      throw new IllegalArgumentException();
    }

    if (containsSamhsaIcdCode(eob.getDiagnosis())) {
      return true;
    }

    for (ExplanationOfBenefit.ItemComponent eobItem : eob.getItem()) {
      if (containsSamhsaProcedureCode(eobItem.getProductOrService())) {
        return true;
      }
    }

    // No blacklisted codes found: this claim isn't SAMHSA-related.
    return false;
  }

  /**
   * @param diagnoses the {@link DiagnosisComponent}s to check
   * @return <code>true</code> if any of the specified {@link DiagnosisComponent}s match any of the
   *     {@link #icd9DiagnosisCodes} or {@link #icd10DiagnosisCodes} entries, <code>false</code> if
   *     they all do not
   */
  private boolean containsSamhsaIcdCode(List<DiagnosisComponent> diagnoses) {
    return diagnoses.stream().anyMatch(this::isSamhsaDiagnosis);
  }

  /**
   * @param procedure the {@link ProcedureComponent}s to check
   * @return <code>true</code> if any of the specified {@link ProcedureComponent}s match any of the
   *     {@link #icd9ProcedureCodes} or {@link #icd10ProcedureCodes} entries, <code>false</code> if
   *     they all do not
   */
  private boolean containsSamhsaIcdProcedueCode(List<ProcedureComponent> procedure) {
    return procedure.stream().anyMatch(this::isSamhsaIcdProcedure);
  }

  /**
   * @param diagnosis the {@link DiagnosisComponent} to check
   * @return <code>true</code> if the specified {@link DiagnosisComponent} matches one of the {@link
   *     #icd9DiagnosisCodes} or {@link #icd10DiagnosisCodes}, or {@link #drgCodes} entries, <code>
   *     false</code> if it does not
   */
  private boolean isSamhsaDiagnosis(DiagnosisComponent diagnosis) {
    CodeableConcept diagnosisConcept;
    try {
      diagnosisConcept = diagnosis.getDiagnosisCodeableConcept();
    } catch (FHIRException e) {
      /*
       * This will only be thrown if the DiagnosisComponent doesn't have a
       * CodeableConcept, which isn't how we build ours.
       */
      throw new BadCodeMonkeyException(e);
    }

    if (diagnosisConcept != null) {
      for (Coding diagnosisCoding : diagnosisConcept.getCoding()) {
        if (IcdCode.CODING_SYSTEM_ICD_9.equals(diagnosisCoding.getSystem())) {
          if (isSamhsaIcd9Diagnosis(diagnosisCoding)) return true;
        } else if (IcdCode.CODING_SYSTEM_ICD_10.equals(diagnosisCoding.getSystem())) {
          if (isSamhsaIcd10Diagnosis(diagnosisCoding)) return true;
        } else {
          // Fail safe: if we don't know the ICD version, assume the code is SAMHSA.
          return true;
        }
      }
    }

    CodeableConcept packageConcept = diagnosis.getPackageCode();
    if (packageConcept != null) {
      for (Coding packageCoding : packageConcept.getCoding()) {
        if (R4SamhsaMatcher.DRG.equals(packageCoding.getSystem())) {
          if (isSamhsaDrgCode(packageCoding)) return true;
        } else {
          // Fail safe: if we don't know the package coding system, assume the code is
          // SAMHSA.
          return true;
        }
      }
    }

    // No blacklisted diagnosis Codings found: this diagnosis isn't SAMHSA-related.
    return false;
  }

  /**
   * @param procedure the {@link ProcedureComponent} to check
   * @return <code>true</code> if the specified {@link ProcedureComponent} matches one of the {@link
   *     #icd9ProcedureCodes} or {@link #icd10ProcedureCodes} entries, <code>false</code> if it does
   *     not
   */
  private boolean isSamhsaIcdProcedure(ProcedureComponent procedure) {
    CodeableConcept concept;
    try {
      concept = procedure.getProcedureCodeableConcept();
    } catch (FHIRException e) {
      /*
       * This will only be thrown if the ProcedureComponent doesn't have a
       * CodeableConcept, which isn't how we build ours.
       */
      throw new BadCodeMonkeyException(e);
    }

    for (Coding coding : concept.getCoding()) {
      if (IcdCode.CODING_SYSTEM_ICD_9.equals(coding.getSystem())) {
        if (isSamhsaIcd9Procedure(coding)) return true;
      } else if (IcdCode.CODING_SYSTEM_ICD_10.equals(coding.getSystem())) {
        if (isSamhsaIcd10Procedure(coding)) return true;
      } else {
        // Fail safe: if we don't know the ICD version, assume the code is SAMHSA.
        return true;
      }
    }

    // No blacklisted diagnosis Codings found: this diagnosis isn't SAMHSA-related.
    return false;
  }

  /**
   * @param diagnosisCoding the diagnosis {@link Coding} to check
   * @return <code>true</code> if the specified diagnosis {@link Coding} matches one of the {@link
   *     #icd9DiagnosisCodes} entries, <code>false</code> if it does not
   */
  private boolean isSamhsaIcd9Diagnosis(Coding diagnosisCoding) {
    if (!IcdCode.CODING_SYSTEM_ICD_9.equals(diagnosisCoding.getSystem()))
      throw new IllegalArgumentException();

    /*
     * Note: per XXX all codes in icd9DiagnosisCodes are already normalized.
     */
    return icd9DiagnosisCodes.contains(normalizeIcd9Code(diagnosisCoding.getCode()));
  }

  /**
   * @param coding the procedure {@link Coding} to check
   * @return <code>true</code> if the specified procedure {@link Coding} matches one of the {@link
   *     #icd9ProcedureCodes} entries, <code>false</code> if it does not
   */
  private boolean isSamhsaIcd9Procedure(Coding coding) {
    if (!IcdCode.CODING_SYSTEM_ICD_9.equals(coding.getSystem()))
      throw new IllegalArgumentException();

    return icd9ProcedureCodes.contains(normalizeIcd9Code(coding.getCode()));
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
   * @param coding the code {@link Coding} to check
   * @return <code>true</code> if the specified code {@link Coding} matches one of the {@link
   *     #drgCodes} entries, <code>false</code> if it does not
   */
  private boolean isSamhsaDrgCode(Coding coding) {
    if (!R4SamhsaMatcher.DRG.equals(coding.getSystem())) throw new IllegalArgumentException();

    // Per the CCW Codebook DRG codes in the CCW are already normalized to the 3
    // digit code.
    return drgCodes.contains(coding.getCode());
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
   * @param diagnosisCoding the diagnosis {@link Coding} to check
   * @return <code>true</code> if the specified diagnosis {@link Coding} matches one of the {@link
   *     #icd10DiagnosisCodes} entries, <code>false</code> if it does not
   */
  private boolean isSamhsaIcd10Diagnosis(Coding diagnosisCoding) {
    if (!IcdCode.CODING_SYSTEM_ICD_10.equals(diagnosisCoding.getSystem()))
      throw new IllegalArgumentException();

    /*
     * Note: per XXX all codes in icd10DiagnosisCodes are already normalized.
     */
    return icd10DiagnosisCodes.contains(normalizeIcd10Code(diagnosisCoding.getCode()));
  }

  private boolean isSamhsaIcd10Procedure(Coding coding) {
    if (!IcdCode.CODING_SYSTEM_ICD_10.equals(coding.getSystem()))
      throw new IllegalArgumentException();

    return icd10ProcedureCodes.contains(normalizeIcd10Code(coding.getCode()));
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
   * @param procedureConcept the procedure {@link CodeableConcept} to check
   * @return <code>true</code> if the specified procedure {@link CodeableConcept} contains any
   *     {@link Coding}s that match any of the {@link #cptCodes} and has no unknown {@link System}s,
   *     <code>false</code> if they all do not
   */
  private boolean containsSamhsaProcedureCode(CodeableConcept procedureConcept) {
    // If there are no procedure codes, then we cannot have any blacklisted codes
    if (procedureConcept.getCoding().isEmpty()) {
      return false;
    }

    // Does the CodeableConcept have a legit HCPCS Coding?
    boolean hasHcpcsCoding = findHcpcsCoding(procedureConcept);

    // Check that Coding to see if it's blacklisted.
    if (hasHcpcsCoding && isSamhsaCptCode(procedureConcept)) {
      return true;
    } else if (!containsOnlyKnownSystems(procedureConcept)) {
      /*
       * Fail safe: if we don't know the procedure Coding system, assume the code is
       * SAMHSA.
       */
      return true;
    } else {
      // Otherwise, this only has known & non-SAMHSA-blacklisted procedure codes.
      return false;
    }
  }

  /**
   * @param procedureConcept the procedure {@link CodeableConcept} to check
   * @return <code>true</code> if the specified procedure {@link CodeableConcept} contains the
   *     {@link Coding} with the HCPCS {@link System}, <code>false</code> if it does not
   */
  private boolean findHcpcsCoding(CodeableConcept procedureConcept) {
    for (Coding procedureCoding : procedureConcept.getCoding()) {
      if (TransformerConstants.CODING_SYSTEM_HCPCS.equals(procedureCoding.getSystem())) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param procedureConcept the procedure {@link CodeableConcept} to check
   * @return <code>false</code> if the specified procedure {@link CodeableConcept} contains any
   *     {@link Coding}s that do not contain any of the {@link System}s <code>true</code> if they do
   */
  private boolean containsOnlyKnownSystems(CodeableConcept procedureConcept) {
    Set<String> codingSystems =
        procedureConcept.getCoding().stream().map(Coding::getSystem).collect(Collectors.toSet());

    String hcpcsCdSystem = CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.HCPCS_CD);
    String hcpcsSystem = TransformerConstants.CODING_SYSTEM_HCPCS;

    // Valid system url for productOrService coding
    Set<String> knownHcpcsSystem = Set.of(hcpcsSystem);

    // Additional valid coding system URL for backwards-compatibility
    // See: https://jira.cms.gov/browse/BFD-1345
    Set<String> backwardsCompatibleHcpcsSystem = Set.of(hcpcsSystem, hcpcsCdSystem);

    return codingSystems.equals(knownHcpcsSystem)
        || codingSystems.equals(backwardsCompatibleHcpcsSystem);
  }

  /**
   * @param procedureConcept the procedure {@link CodeableConcept} to check
   * @return <code>true</code> if the specified procedure {@link CodeableConcept} contains any
   *     {@link Coding}s that match any of the {@link #cptCodes}, <code>false</code> if they all do
   *     not
   */
  private boolean isSamhsaCptCode(CodeableConcept procedureConcept) {
    /*
     * Note: CPT codes represent a subset of possible HCPCS codes (but are the only
     * subset that we blacklist from).
     */
    Set<String> codingSystems =
        procedureConcept.getCoding().stream().map(Coding::getSystem).collect(Collectors.toSet());

    if (!codingSystems.contains(TransformerConstants.CODING_SYSTEM_HCPCS)) {
      throw new IllegalArgumentException();
    }

    /*
     * Note: per XXX all codes in icd10DiagnosisCodes are already normalized.
     */
    return procedureConcept.getCoding().stream()
        .anyMatch(
            procedureCoding -> cptCodes.contains(normalizeHcpcsCode(procedureCoding.getCode())));
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
