package gov.cms.bfd.server.ng.claim.model;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;

/** Clam Sub type. */
@Getter
@AllArgsConstructor
public enum ClaimSubtype {
  /** Represents CARRIER type. */
  CARRIER("carrier"),
  /** Represents DME type. */
  DME("dme"),
  /** Represents HHA type. */
  HHA("hha"),
  /** Represents HOSPICE type. */
  HOSPICE("hospice"),
  /** Represents INPATIENT type. */
  INPATIENT("inpatient"),
  /** Represents OUTPATIENT type. */
  OUTPATIENT("outpatient"),
  /** Represents PDE type. */
  PDE("pde"),
  /** Represents SNF type. */
  SNF("snf");

  private final String code;

  Coding toFhir() {
    return new Coding().setSystem(SystemUrls.CARIN_CLAIM_SUBTYPE).setCode(code);
  }

  /**
   * Gets ClaimSubtype from code.
   *
   * @param code code
   * @return ClaimSubtype
   */
  public static ClaimSubtype fromCode(String code) {
    return Arrays.stream(values())
        .filter(v -> v.code.equalsIgnoreCase(code))
        .findFirst()
        .orElseThrow(() -> new InvalidRequestException("Unsupported claim type. "));
  }
}
