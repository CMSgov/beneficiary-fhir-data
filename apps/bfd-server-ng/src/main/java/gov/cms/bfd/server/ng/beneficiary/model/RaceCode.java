package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.SystemUrl;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

@Getter
@AllArgsConstructor
public enum RaceCode {
  // Empty comments here are used to force consistent formatting
  WHITE(
      "1",
      "2106-3",
      "White", //
      SystemUrl.US_CORE_RACE,
      SystemUrl.CDC_RACE_ETHNICITY),
  BLACK(
      "2",
      "2054-5",
      "Black or African American",
      SystemUrl.US_CORE_RACE,
      SystemUrl.CDC_RACE_ETHNICITY),
  OTHER(
      "3",
      "2131-1",
      "Other Race", //
      SystemUrl.US_CORE_RACE,
      SystemUrl.CDC_RACE_ETHNICITY),
  ASIAN(
      "4",
      "2028-9",
      "Asian", //
      SystemUrl.US_CORE_RACE,
      SystemUrl.CDC_RACE_ETHNICITY),
  HISPANIC(
      "5",
      "2135-2",
      "Hispanic or Latino",
      SystemUrl.US_CORE_ETHNICITY,
      SystemUrl.CDC_RACE_ETHNICITY),
  NORTH_AMERICAN(
      "6",
      "1002-5",
      "American Indian or Alaska Native", //
      SystemUrl.US_CORE_RACE,
      SystemUrl.CDC_RACE_ETHNICITY),
  UNKNOWN(
      "",
      "UNK",
      "Unknown", //
      SystemUrl.US_CORE_RACE,
      SystemUrl.HL7_NULL_FLAVOR);

  private final String idrCode;
  private final String uscdiCode;
  private final String uscdiDisplay;
  private final String extensionSystem;
  private final String ombSystem;

  public static RaceCode fromIdrCode(String idrCode) {
    return Arrays.stream(values())
        .filter(v -> v.idrCode.equals(idrCode))
        .findFirst()
        .orElse(RaceCode.UNKNOWN);
  }

  Extension toFhir() {
    var ombExtension =
        new Extension()
            .setUrl("ombCategory")
            .setValue(
                new Coding().setSystem(ombSystem).setCode(uscdiCode).setDisplay(uscdiDisplay));
    var displayExtension =
        new Extension().setUrl("text").setValue(new CodeType().setValue(uscdiDisplay));

    var ethnicityExtension = new Extension().setUrl(extensionSystem);
    ethnicityExtension.addExtension(ombExtension).addExtension(displayExtension);
    return ethnicityExtension;
  }
}
