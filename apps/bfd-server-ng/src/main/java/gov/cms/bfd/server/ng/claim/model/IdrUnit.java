package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.SimpleQuantity;

@Getter
@AllArgsConstructor
public enum IdrUnit {
  ML("ML", "mL"),
  ME("ME", "mg"),
  UN("UN", "[arb'U]"),
  F2("F2", "[IU]"),
  GR("GR", "g"),
  EA("EA", "[arb'U]");

  private final String idrCode;
  private final String fhirUnit;

  public static Optional<IdrUnit> tryFromCode(String idrCode) {
    return Arrays.stream(values()).filter(v -> v.idrCode.equals(idrCode)).findFirst();
  }

  SimpleQuantity toFhir(double value) {
    var quantity = new SimpleQuantity();
    quantity
        .setValue(value)
        .setSystem(SystemUrls.UNITS_OF_MEASURE)
        .setCode(fhirUnit)
        .setUnit(idrCode);
    return quantity;
  }
}
