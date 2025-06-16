package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ClaimLineDeductibleCoinsuranceCode {
  _0("0", "Charges are subject to deductible and coinsurance"),
  _1("1", "Charges are not subject to deductible"),
  _2("2", "Charges are not subject to coinsurance"),
  _3("3", "Charges are not subject to deductible or coinsurance"),
  _4(
      "4",
      "No charge or units associated with this revenue center code. (For multiple HCPCS per single revenue center code) For revenue center code 0001, the following MSP override values may be present:"),
  M("M", "Override code; EGHP (employer group health plan) services involved"),
  N("N", "Override code; non-EGHP services involved"),
  X("X", "Override code: MSP (Medicare is secondary payer) cost avoided");

  private final String code;
  private final String display;

  public static ClaimLineDeductibleCoinsuranceCode fromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst().get();
  }

  Coding toFhir() {
    return new Coding()
        .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_DEDUCTIBLE_COINSURANCE_CODE)
        .setCode(code)
        .setDisplay(display);
  }
}
