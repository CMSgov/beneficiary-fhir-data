package gov.cms.bfd.server.war.stu3.providers;

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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.DiagnosisComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ProcedureComponent;
import org.hl7.fhir.exceptions.FHIRException;
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
public final class SamhsaMatcher implements Predicate<ExplanationOfBenefit> {
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
   * Constructs a new {@link SamhsaMatcher}, loading the lists of SAMHSA-related codes from the
   * classpath.
   */
  public SamhsaMatcher() {
    this.drgCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList("samhsa-related-codes/codes-drg.csv", "MS-DRGs").stream()
                .map(SamhsaMatcher::normalizeDrgCode)
                .collect(Collectors.toList()));
    this.cptCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList("samhsa-related-codes/codes-cpt.csv", "CPT Code"));
    this.icd9ProcedureCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList("samhsa-related-codes/codes-icd-9-procedure.csv", "ICD-9-CM")
                .stream()
                .map(SamhsaMatcher::normalizeIcd9Code)
                .collect(Collectors.toList()));
    this.icd9DiagnosisCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList(
                    "samhsa-related-codes/codes-icd-9-diagnosis.csv", "ICD-9-CM Diagnosis Code")
                .stream()
                .map(SamhsaMatcher::normalizeIcd9Code)
                .collect(Collectors.toList()));
    this.icd10ProcedureCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList(
                    "samhsa-related-codes/codes-icd-10-procedure.csv", "ICD-10-PCS Code")
                .stream()
                .map(SamhsaMatcher::normalizeIcd10Code)
                .collect(Collectors.toList()));
    this.icd10DiagnosisCodes =
        Collections.unmodifiableList(
            resourceCsvColumnToList(
                    "samhsa-related-codes/codes-icd-10-diagnosis.csv", "ICD-10-CM Diagnosis Code")
                .stream()
                .map(SamhsaMatcher::normalizeIcd10Code)
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
    ClaimType claimType = TransformerUtils.getClaimType(eob);
    if (claimType == ClaimType.CARRIER) {
      return testCarrierOrDmeClaim(eob);
    } else if (claimType == ClaimType.DME) {
      return testCarrierOrDmeClaim(eob);
    } else if (claimType == ClaimType.HHA) {
      return testHhaClaim(eob);
    } else if (claimType == ClaimType.HOSPICE) {
      return testHospiceClaim(eob);
    } else if (claimType == ClaimType.INPATIENT) {
      return testInpatientClaim(eob);
    } else if (claimType == ClaimType.OUTPATIENT) {
      return testOutpatientClaim(eob);
    } else if (claimType == ClaimType.SNF) {
      return testSnfClaim(eob);
    } else if (claimType == ClaimType.PDE) {
      return testPartDEvent(eob);
    } else throw new BadCodeMonkeyException("Unsupported claim type: " + claimType);
  }

  /**
   * @param eob the {@link ClaimType#CARRIER} {@link ExplanationOfBenefit} to check
   * @return <code>true</code> if the specified {@link ClaimType#CARRIER} {@link
   *     ExplanationOfBenefit} contains any known-SAMHSA-related codes, <code>false</code> if it
   *     does not
   */
  private boolean testCarrierOrDmeClaim(ExplanationOfBenefit eob) {
    if (!(TransformerUtils.getClaimType(eob) == ClaimType.CARRIER
        || TransformerUtils.getClaimType(eob) == ClaimType.DME))
      throw new IllegalArgumentException();

    if (containsSamhsaIcdCode(eob.getDiagnosis())) return true;

    for (ExplanationOfBenefit.ItemComponent eobItem : eob.getItem()) {
      if (containsSamhsaProcedureCode(eobItem.getService())) return true;
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
    if (TransformerUtils.getClaimType(eob) != ClaimType.HHA) throw new IllegalArgumentException();

    if (containsSamhsaIcdCode(eob.getDiagnosis())) return true;

    for (ExplanationOfBenefit.ItemComponent eobItem : eob.getItem()) {
      if (containsSamhsaProcedureCode(eobItem.getService())) return true;
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
    if (TransformerUtils.getClaimType(eob) != ClaimType.HOSPICE)
      throw new IllegalArgumentException();

    if (containsSamhsaIcdCode(eob.getDiagnosis())) return true;

    for (ExplanationOfBenefit.ItemComponent eobItem : eob.getItem()) {
      if (containsSamhsaProcedureCode(eobItem.getService())) return true;
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
    if (TransformerUtils.getClaimType(eob) != ClaimType.INPATIENT)
      throw new IllegalArgumentException();

    if (containsSamhsaIcdCode(eob.getDiagnosis())) return true;

    if (containsSamhsaIcdProcedueCode(eob.getProcedure())) return true;

    for (ExplanationOfBenefit.ItemComponent eobItem : eob.getItem()) {
      if (containsSamhsaProcedureCode(eobItem.getService())) return true;
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
    if (TransformerUtils.getClaimType(eob) != ClaimType.OUTPATIENT)
      throw new IllegalArgumentException();

    if (containsSamhsaIcdCode(eob.getDiagnosis())) return true;

    if (containsSamhsaIcdProcedueCode(eob.getProcedure())) return true;

    for (ExplanationOfBenefit.ItemComponent eobItem : eob.getItem()) {
      if (containsSamhsaProcedureCode(eobItem.getService())) return true;
    }

    // No blacklisted codes found: this claim isn't SAMHSA-related.
    return false;
  }

  /**
   * @param eob the {@link ClaimType#SNF} {@link ExplanationOfBenefit} to check
   * @return <code>true</code> if the specified {@link ClaimType#SNF} {@link ExplanationOfBenefit}
   *     contains any known-SAMHSA-related codes, <code>false</code> if it does not
   */
  private boolean testSnfClaim(ExplanationOfBenefit eob) {
    if (TransformerUtils.getClaimType(eob) != ClaimType.SNF) throw new IllegalArgumentException();

    if (containsSamhsaIcdCode(eob.getDiagnosis())) return true;

    if (containsSamhsaIcdProcedueCode(eob.getProcedure())) return true;

    for (ExplanationOfBenefit.ItemComponent eobItem : eob.getItem()) {
      if (containsSamhsaProcedureCode(eobItem.getService())) return true;
    }

    // No blacklisted codes found: this claim isn't SAMHSA-related.
    return false;
  }

  /**
   * @param eob the {@link ClaimType#PDE} {@link ExplanationOfBenefit} to check
   * @return <code>true</code> if the specified {@link ClaimType#PDE} {@link ExplanationOfBenefit}
   *     contains any known-SAMHSA-related codes, <code>false</code> if it does not
   */
  private boolean testPartDEvent(ExplanationOfBenefit eob) {
    if (TransformerUtils.getClaimType(eob) != ClaimType.PDE) throw new IllegalArgumentException();

    // There are no SAMHSA fields in PDE claims
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
        if (SamhsaMatcher.DRG.equals(packageCoding.getSystem())) {
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
    if (!SamhsaMatcher.DRG.equals(coding.getSystem())) throw new IllegalArgumentException();

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
   *     {@link Coding}s that match any of the {@link #cptCodes}, <code>false</code> if they all do
   *     not
   */
  private boolean containsSamhsaProcedureCode(CodeableConcept procedureConcept) {
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

  /**
   * @param procedureCoding the procedure {@link Coding} to check
   * @return <code>true</code> if the specified procedure {@link Coding} matches one of the {@link
   *     #cptCodes} entries, <code>false</code> if it does not
   */
  private boolean isSamhsaCptCode(Coding procedureCoding) {
    /*
     * Note: CPT codes represent a subset of possible HCPCS codes (but are the only
     * subset that we blacklist from).
     */
    if (!TransformerConstants.CODING_SYSTEM_HCPCS.equals(procedureCoding.getSystem()))
      throw new IllegalArgumentException();

    /*
     * Note: per XXX all codes in icd10DiagnosisCodes are already normalized.
     */
    return cptCodes.contains(normalizeHcpcsCode(procedureCoding.getCode()));
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
