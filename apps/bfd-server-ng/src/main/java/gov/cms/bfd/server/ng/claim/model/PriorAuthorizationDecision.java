package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/** Prior Authorization Decision Codes. */
public sealed interface PriorAuthorizationDecision
    permits PriorAuthorizationDecision.Valid, PriorAuthorizationDecision.Invalid {

  /**
   * Gets the code value.
   *
   * @return the code
   */
  String getCode();

  /**
   * Gets the display value.
   *
   * @return the display
   */
  String getDisplay();

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return prior authorization decision or empty Optional if code is null or blank
   */
  static Optional<PriorAuthorizationDecision> tryFromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(c -> c.code.equals(code))
            .map(c -> (PriorAuthorizationDecision) c)
            .findFirst()
            .orElseGet(() -> new Invalid(code)));
  }

  /**
   * Maps enum/record to FHIR spec.
   *
   * @return extension
   */
  default Extension toFhir() {
    return new Extension(SystemUrls.EXT_PA_DECISION_URL)
        .setValue(new Coding(SystemUrls.SYS_PA_DECISION, getCode(), getDisplay()));
  }

  /** Enum for all known, valid codes. */
  @AllArgsConstructor
  @Getter
  enum Valid implements PriorAuthorizationDecision {
    /** A - Affirmed. */
    A("A", "Affirmed"),
    /** P - Pending. */
    P("P", "Pending"),
    /** N - Non-affirmed. */
    N("N", "Non-affirmed");

    private final String code;
    private final String display;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements PriorAuthorizationDecision {
    @Override
    public String getDisplay() {
      return "";
    }

    @Override
    public String getCode() {
      return code;
    }
  }
}
