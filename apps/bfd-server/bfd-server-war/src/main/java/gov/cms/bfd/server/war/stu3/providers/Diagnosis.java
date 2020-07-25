package gov.cms.bfd.server.war.stu3.providers;

import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.DiagnosisComponent;
import org.hl7.fhir.dstu3.model.codesystems.ExDiagnosistype;

/** Models a diagnosis code entry in a claim. */
final class Diagnosis extends IcdCode {

  private final Character presentOnAdmission;
  private final Set<DiagnosisLabel> labels;

  /**
   * Constructs a new {@link Diagnosis}.
   *
   * @param icdCode the ICD code of the diagnosis, if any
   * @param icdVersionCode the CCW encoding (per <a href=
   *     "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prncpal_dgns_vrsn_cd.txt">
   *     CCW Data Dictionary: PRNCPAL_DGNS_VRSN_CD</a> and other similar fields) of the code's ICD
   *     version, if any
   * @param labels the value to use for {@link #getLabels()}
   * @return the new {@link Diagnosis}, or {@link Optional#empty()} if no <code>icdCode</code> was
   *     present
   */
  private Diagnosis(
      Optional<String> icdCode, Optional<Character> icdVersionCode, DiagnosisLabel... labels) {
    super(icdCode, icdVersionCode);
    Objects.requireNonNull(icdCode);
    Objects.requireNonNull(icdVersionCode);
    Objects.requireNonNull(labels);

    this.presentOnAdmission = null;
    this.labels = new HashSet<>(Arrays.asList(labels));
  }

  /**
   * Constructs a new {@link Diagnosis}.
   *
   * @param icdCode the ICD code of the diagnosis, if any
   * @param icdVersionCode the CCW encoding (per <a href=
   *     "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prncpal_dgns_vrsn_cd.txt">
   *     CCW Data Dictionary: PRNCPAL_DGNS_VRSN_CD</a> and other similar fields) of the code's ICD
   *     version, if any
   * @param presentOnAdmission the value to use for {@link #getPresentOnAdmission}
   * @param labels the value to use for {@link #getLabels()}
   * @return the new {@link Diagnosis}, or {@link Optional#empty()} if no <code>icdCode</code> was
   *     present
   */
  private Diagnosis(
      Optional<String> icdCode,
      Optional<Character> icdVersionCode,
      Optional<Character> presentOnAdmission,
      DiagnosisLabel... labels) {
    super(icdCode, icdVersionCode);
    Objects.requireNonNull(icdCode);
    Objects.requireNonNull(icdVersionCode);
    Objects.requireNonNull(presentOnAdmission);
    Objects.requireNonNull(labels);

    this.presentOnAdmission = presentOnAdmission.orElse(null);
    this.labels = new HashSet<>(Arrays.asList(labels));
  }

  /** @return the ICD label */
  Set<DiagnosisLabel> getLabels() {
    return labels;
  }

  public void setLabels(DiagnosisLabel label) {
    this.labels.add(label);
  }

  /** @return the ICD presentOnAdmission indicator */
  Optional<Character> getPresentOnAdmission() {
    return Optional.ofNullable(presentOnAdmission);
  }

  /**
   * Constructs a new {@link Diagnosis}, if the specified <code>icdCode</code> is present.
   *
   * @param icdCode the ICD code of the diagnosis, if any
   * @param icdVersionCode the CCW encoding (per <a href=
   *     "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prncpal_dgns_vrsn_cd.txt">
   *     CCW Data Dictionary: PRNCPAL_DGNS_VRSN_CD</a> and other similar fields) of the code's ICD
   *     version, if any
   * @param labels the value to use for {@link #getLabels()}
   * @return the new {@link Diagnosis}, or {@link Optional#empty()} if no <code>icdCode</code> was
   *     present
   */
  static Optional<Diagnosis> from(
      Optional<String> icdCode, Optional<Character> icdVersionCode, DiagnosisLabel... labels) {
    if (!icdCode.isPresent()) return Optional.empty();
    return Optional.of(new Diagnosis(icdCode, icdVersionCode, labels));
  }

  /**
   * Constructs a new {@link Diagnosis}, if the specified <code>code</code> is present.
   *
   * @param icdCode the ICD code of the diagnosis, if any
   * @param icdVersionCode the CCW encoding (per <a href=
   *     "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prncpal_dgns_vrsn_cd.txt">
   *     CCW Data Dictionary: PRNCPAL_DGNS_VRSN_CD</a> and other similar fields) of the code's ICD
   *     version, if any
   * @param presentOnAdmission the value to use for {@link #getPresentOnAdmission}
   * @param labels the value to use for {@link #getLabels()}
   * @return the new {@link Diagnosis}, or {@link Optional#empty()} if no <code>code</code> was
   *     present
   */
  static Optional<Diagnosis> from(
      Optional<String> icdCode,
      Optional<Character> icdVersionCode,
      Optional<Character> presentOnAdmission,
      DiagnosisLabel... labels) {
    if (!icdCode.isPresent()) return Optional.empty();
    return Optional.of(new Diagnosis(icdCode, icdVersionCode, presentOnAdmission, labels));
  }

  /**
   * Enumerates the various labels/tags that are used to distinguish between the various diagnoses
   * in a claim.
   */
  static enum DiagnosisLabel {
    /** Note: display text matches {@link ExDiagnosistype#PRINCIPAL}. */
    PRINCIPAL(
        "principal",
        "The single medical diagnosis that is most relevant to the patient's chief complaint"
            + " or need for treatment."),

    /** Note: display text matches {@link ExDiagnosistype#ADMITTING}. */
    ADMITTING(
        "admitting",
        "The diagnosis given as the reason why the patient was admitted to the hospital."),

    /** Note: display text (mostly) matches {@link CcwCodebookVariable#FST_DGNS_E_CD}. */
    FIRSTEXTERNAL(
        "external-first",
        "The code used to identify the 1st external cause of injury, poisoning, or other adverse effect."),

    /** Note: display text (mostly) matches {@link CcwCodebookVariable#FST_DGNS_E_CD}. */
    EXTERNAL(
        "external",
        "A code used to identify an external cause of injury, poisoning, or other adverse effect."),

    /** Note: display text (mostly) matches {@link CcwCodebookVariable#RSN_VISIT_CD1}. */
    REASONFORVISIT(
        "reason-for-visit",
        "A diagnosis code used to identify the patient's reason for the visit.");

    private final String fhirCode;
    private final String fhirDisplay;

    /**
     * Enum constant constructor.
     *
     * @param fhirCode the value to use for {@link #toCode()}
     * @param fhirDisplay the value to use for {@link #getDisplay()}
     */
    private DiagnosisLabel(String fhirCode, String fhirDisplay) {
      this.fhirCode = fhirCode;
      this.fhirDisplay = fhirDisplay;
    }

    /**
     * @return the FHIR {@link Coding#getSystem()} to use for the {@link
     *     DiagnosisComponent#getType()} that this {@link DiagnosisLabel} should be mapped to
     */
    public String getSystem() {
      return TransformerConstants.CODING_SYSTEM_BBAPI_DIAGNOSIS_TYPE;
    }

    /**
     * @return the FHIR {@link Coding#getCode()} to use for the {@link DiagnosisComponent#getType()}
     *     that this {@link DiagnosisLabel} should be mapped to
     */
    public String toCode() {
      return fhirCode;
    }

    /**
     * @return the FHIR {@link Coding#getDisplay()} to use for the {@link
     *     DiagnosisComponent#getType()} that this {@link DiagnosisLabel} should be mapped to
     */
    public String getDisplay() {
      return fhirDisplay;
    }
  }
}
