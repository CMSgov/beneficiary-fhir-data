package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.SimpleQuantity;

/** Types of units. */
@Getter
@AllArgsConstructor
public enum IdrUnit {
  /** Milliliters. */
  ML("ML", "mL"),
  /** Milligrams. */
  ME("ME", "mg"),
  /** Unit. */
  UN("UN", "[arb'U]"),
  /** International Unit. */
  F2("F2", "[IU]"),
  /** Grams. */
  GR("GR", "g"),
  /** Each. */
  EA("EA", "[arb'U]");

  private final String idrCode;
  private final String fhirUnit;

  /**
   * Convert from a database code.
   *
   * @param idrCode database code
   * @return IDR unit
   */
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
