package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.SimpleQuantity;

import java.util.Arrays;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum IdrUnit {
  ML("ML", "mL"),
  ME("ME", "mg"),
  UN("UN", "[arb'U]"),
  F2("F2", "[IU]"),
  GR("GR", "g"),
  EA("EA", "[arb'U]");

  private String idrCode;
  private String fhirUnit;

  public static Optional<IdrUnit> tryFromCode(String idrCode) {
    return Arrays.stream(values()).filter(v -> v.idrCode.equals(idrCode)).findFirst();
  }

  SimpleQuantity toFhir(float value) {
    var quantity = new SimpleQuantity();
    quantity
        .setValue(value)
        .setSystem(SystemUrls.UNITS_OF_MEASURE)
        .setCode(fhirUnit)
        .setUnit(idrCode);
    return quantity;
  }
}
