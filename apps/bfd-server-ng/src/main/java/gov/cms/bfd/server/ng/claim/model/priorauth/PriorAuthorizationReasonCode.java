package gov.cms.bfd.server.ng.claim.model.priorauth;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Optional;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/** Reason code associated with a prior authorization decision. */
public record PriorAuthorizationReasonCode(String code) {

  /**
   * Converts from a database code.
   *
   * @param code database code
   * @return prior authorization reason, or empty if null or blank
   */
  public static Optional<PriorAuthorizationReasonCode> tryFromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(new PriorAuthorizationReasonCode(code));
  }

  /**
   * Maps this reason code to an item-level FHIR extension.
   *
   * @return the FHIR extension
   */
  public Extension toFhir() {
    return new Extension(SystemUrls.EXT_PR_REASON_CODE_1_URL).setValue(new Coding().setCode(code));
  }
}
