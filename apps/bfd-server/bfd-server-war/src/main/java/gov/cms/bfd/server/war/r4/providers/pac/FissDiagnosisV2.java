package gov.cms.bfd.server.war.r4.providers.pac;

import gov.cms.bfd.model.rda.entities.RdaFissDiagnosisCode;
import lombok.Getter;

/** test. */
@Getter
public class FissDiagnosisV2 {
  /** test. */
  private String diagnosisCode;

  /** test. */
  private String poaIndicator;

  /**
   * test.
   *
   * @param diagnosisCode test
   */
  public FissDiagnosisV2(RdaFissDiagnosisCode diagnosisCode) {
    this.diagnosisCode = diagnosisCode.getDiagCd2();
    this.poaIndicator = diagnosisCode.getDiagPoaInd();
  }

  /**
   * test.
   *
   * @param diagnosisCode test
   */
  public FissDiagnosisV2(String diagnosisCode) {
    this.diagnosisCode = diagnosisCode;
    this.poaIndicator = "";
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof FissDiagnosisV2) {
      return ((FissDiagnosisV2) other).diagnosisCode.equals(this.diagnosisCode);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.diagnosisCode == null ? 0 : this.diagnosisCode.hashCode();
  }
}
