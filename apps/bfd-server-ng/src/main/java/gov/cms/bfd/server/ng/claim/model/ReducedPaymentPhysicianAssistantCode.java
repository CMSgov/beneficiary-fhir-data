package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/**
 * Carrier line reduced payment physician assistant codes. Suppress SonarQube warning that constant
 * names should comply with naming conventions.
 */
@AllArgsConstructor
@Getter
public enum ReducedPaymentPhysicianAssistantCode {
  /** 0 - N/A. */
  _0("0", "N/A"),
  /** 1 - 65% of payment. Either physician assistants assisting in surgery or nurse midwives. */
  _1("1", "65% of payment. Either physician assistants assisting in surgery or nurse midwives"),
  /**
   * 2 - 75% of payment. Either physician assistants performing services in a hospital (other than
   * assisting surgery) or nurse practitioners/clinical nurse specialist performing services in
   * rural areas or clinical social worker services.
   */
  _2(
      "2",
      "75% of payment. Either physician assistants performing services in a hospital (other than assisting surgery) or nurse practitioners/clinical nurse specialist performing services in rural areas or clinical social worker services"),
  /**
   * 3 - 85% of payment. Either physician assistant services for other than assisting surgery or
   * other hospital services or nurse practitioners services (not in rural areas).
   */
  _3(
      "3",
      "85% of payment. Either physician assistant services for other than assisting surgery or other hospital services or nurse practitioners services (not in rural areas)");

  private final String code;
  private final String display;

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return carrier line reduced payment physician assistant code
   */
  public static Optional<ReducedPaymentPhysicianAssistantCode> fromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst();
  }

  Extension toFhir() {
    return new Extension(SystemUrls.EXT_CLM_PHYSN_ASTNT_CD_URL)
        .setValue(new Coding(SystemUrls.SYS_CLM_PHYSN_ASTNT_CD, code, display));
  }
}
