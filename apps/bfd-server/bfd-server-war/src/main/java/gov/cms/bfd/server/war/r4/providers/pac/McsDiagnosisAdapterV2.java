package gov.cms.bfd.server.war.r4.providers.pac;

import gov.cms.bfd.model.rda.entities.RdaMcsDetail;
import gov.cms.bfd.model.rda.entities.RdaMcsDiagnosisCode;
import lombok.Getter;

/**
 * Adapter to handle both {@link RdaMcsDiagnosisCode} and {@link RdaMcsDetail} for diagnosis info.
 */
@Getter
public class McsDiagnosisAdapterV2 {
  /** Diagnosis code. */
  private final String diagnosisCode;

  /** ICD type. */
  private final String icdType;

  /**
   * Create a new object from a {@link RdaMcsDiagnosisCode}.
   *
   * @param diagnosisCode diagnosis code
   */
  public McsDiagnosisAdapterV2(RdaMcsDiagnosisCode diagnosisCode) {
    this.diagnosisCode = diagnosisCode.getIdrDiagCode();
    this.icdType = diagnosisCode.getIdrDiagIcdType();
  }

  /**
   * Create a new object from a {@link RdaMcsDetail}.
   *
   * @param mcsDetail MCS detail
   */
  public McsDiagnosisAdapterV2(RdaMcsDetail mcsDetail) {
    this.diagnosisCode = mcsDetail.getIdrDtlPrimaryDiagCode();
    this.icdType = mcsDetail.getIdrDtlDiagIcdType();
  }

  @Override
  public boolean equals(Object other) {
    // When getting a distinct list of codes, we only care about the diagnosis code itself
    if (other instanceof McsDiagnosisAdapterV2 otherDiagnosis) {
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
