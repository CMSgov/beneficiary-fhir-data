package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;

/** Beneficiary race code. */
@Getter
@AllArgsConstructor
public enum RaceCode {
  // Empty comments here are used to force consistent formatting

  /** White race code. */
  WHITE(
      "1",
      "2106-3",
      "White", //
      SystemUrls.US_CORE_RACE,
      SystemUrls.CDC_RACE_ETHNICITY),
  /** Black race code. */
  BLACK(
      "2",
      "2054-5",
      "Black or African American",
      SystemUrls.US_CORE_RACE,
      SystemUrls.CDC_RACE_ETHNICITY),
  /** Other race code. */
  OTHER(
      "3",
      "2131-1",
      "Other Race", //
      SystemUrls.US_CORE_RACE,
      SystemUrls.CDC_RACE_ETHNICITY),
  /** Asian race code. */
  ASIAN(
      "4",
      "2028-9",
      "Asian", //
      SystemUrls.US_CORE_RACE,
      SystemUrls.CDC_RACE_ETHNICITY),
  /** Hispanic race code. */
  HISPANIC(
      "5",
      "2135-2",
      "Hispanic or Latino",
      SystemUrls.US_CORE_ETHNICITY,
      SystemUrls.CDC_RACE_ETHNICITY),
  /** Native American race code. */
  NATIVE_AMERICAN(
      "6",
      "1002-5",
      "American Indian or Alaska Native", //
      SystemUrls.US_CORE_RACE,
      SystemUrls.CDC_RACE_ETHNICITY),
  /** Unknown race code. */
  UNKNOWN(
      "",
      "UNK",
      "Unknown", //
      SystemUrls.US_CORE_RACE,
      SystemUrls.HL7_NULL_FLAVOR);

  private final String idrCode;
  private final String uscdiCode;
  private final String uscdiDisplay;
  private final String extensionSystem;
  private final String ombSystem;

  /**
   * Converts the IDR race code to its corresponding {@link RaceCode} representation.
   *
   * @param idrCode IDR race code
   * @return {@link RaceCode} representation
   */
  public static RaceCode fromIdrCode(String idrCode) {
    return Arrays.stream(values())
        .filter(v -> v.idrCode.equals(idrCode))
        .findFirst()
        .orElse(RaceCode.UNKNOWN);
  }

  Extension toFhir() {
    var ombExtension =
        new Extension()
            .setUrl(SystemUrls.OMB_CATEGORY)
            .setValue(
                new Coding().setSystem(ombSystem).setCode(uscdiCode).setDisplay(uscdiDisplay));
    var displayExtension = new Extension().setUrl("text").setValue(new StringType(uscdiDisplay));

    var ethnicityExtension = new Extension().setUrl(extensionSystem);
    ethnicityExtension.addExtension(ombExtension).addExtension(displayExtension);
    return ethnicityExtension;
  }
}
