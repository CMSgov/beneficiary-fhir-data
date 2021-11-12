package gov.cms.bfd.server.war.r4.providers;

import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.server.war.commons.AbstractSamhsaMatcher;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.IcdCode;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

public abstract class AbstractR4SamhsaMatcher<T> extends AbstractSamhsaMatcher<T> {

  /**
   * Checks that for the specified {@link CodeableConcept}, the Codings (if any) within, contain a
   * blacklisted MHSA procedure code. If any of the systemms within th {@link CodeableConcept} are
   * not known/expected, returns {@code true} and assuems the system is SAMHSA as a safety fallback.
   *
   * @param procedureConcept the procedure {@link CodeableConcept} to check
   * @return <code>true</code> if the specified procedure {@link CodeableConcept} contains any
   *     {@link Coding}s that match any of the {@link #cptCodes} or has unknown coding systems,
   *     <code>false</code> otherwise.
   */
  protected boolean containsSamhsaProcedureCode(CodeableConcept procedureConcept) {
    return !procedureConcept.getCoding().isEmpty()
        && (hasHcpcsAndSamhsaCptCode(procedureConcept)
            || !containsOnlyKnownSystems(procedureConcept));
  }

  /**
   * Checks that for a {@link CodeableConcept} the {@link Coding}s contain a HCPCS system and at
   * least one blacklisted CPT code.
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

    // If no HCPCS system, it may be DME
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

    String hcpcsCdSystem = CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.HCPCS_CD);

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
                    !AbstractR4SamhsaMatcher.DRG.equals(coding.getSystem())
                        || isSamhsaDrgCode(coding));
  }

  /**
   * @param coding the code {@link Coding} to check
   * @return <code>true</code> if the specified code {@link Coding} matches one of the {@link
   *     #drgCodes} entries, <code>false</code> if it does not
   */
  @VisibleForTesting
  boolean isSamhsaDrgCode(Coding coding) {
    return isSamhsaCodingForSystem(coding, drgCodes, AbstractR4SamhsaMatcher.DRG);
  }

  /**
   * @param coding the diagnosis {@link Coding} to check
   * @return <code>true</code> if the specified diagnosis {@link Coding} matches one of the {@link
   *     AbstractR4SamhsaMatcher#icd9DiagnosisCodes} entries, <code>false</code> if it does not
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
   *     AbstractR4SamhsaMatcher#icd10DiagnosisCodes} entries, <code>false</code> if it does not
   */
  @VisibleForTesting
  boolean isSamhsaIcd10Diagnosis(Coding coding) {
    return isSamhsaCodingForSystem(coding, icd10DiagnosisCodes, IcdCode.CODING_SYSTEM_ICD_10);
  }

  /**
   * @param coding the diagnosis {@link Coding} to check
   * @return <code>true</code> if the specified precedure {@link Coding} matches one of the {@link
   *     AbstractR4SamhsaMatcher#icd10ProcedureCodes} entries, <code>false</code> if it does not
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
   *     AbstractR4SamhsaMatcher#cptCodes} entries, <code>false</code> if it does not
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
}
