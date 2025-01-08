package gov.cms.bfd.server.war.r4.providers.pac;

import gov.cms.bfd.model.rda.entities.RdaFissDiagnosisCode;
import lombok.Getter;

/**
 * Adapter for combining {@link RdaFissDiagnosisCode} items with other diagnosis codes found on the
 * claim.
 */
@Getter
public class FissDiagnosisAdapterV2 {
  /** Diagnosis code. */
  private final String diagnosisCode;

  /** Present on admission indicator. */
  private final String poaIndicator;

  /**
   * Creates a new instance from the {@link RdaFissDiagnosisCode}.
   *
   * @param diagnosisCode Diagnosis code.
   */
  public FissDiagnosisAdapterV2(RdaFissDiagnosisCode diagnosisCode) {
    this.diagnosisCode = diagnosisCode.getDiagCd2();
    this.poaIndicator = diagnosisCode.getDiagPoaInd();
  }

  /**
   * Creates a new instance from a generic diagnosis code.
   *
   * @param diagnosisCode Diagnosis code.
   */
  public FissDiagnosisAdapterV2(String diagnosisCode) {
    this.diagnosisCode = diagnosisCode;
    this.poaIndicator = "";
  }

  @Override
  public boolean equals(Object other) {
    // When getting a distinct list of codes, we only care about the diagnosis code itself
    if (other instanceof FissDiagnosisAdapterV2 otherDiagnosis) {
      if (otherDiagnosis.diagnosisCode == null) {
        return this.diagnosisCode == null;
      }
      return otherDiagnosis.getDiagnosisCode().equals(this.diagnosisCode);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.diagnosisCode == null ? 0 : this.diagnosisCode.hashCode();
  }
}
