package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;

/** Beneficiary sex code. */
@Getter
@AllArgsConstructor
public enum SexCode {
  /** Male sex code. */
  MALE("1", Enumerations.AdministrativeGender.MALE, "248153007"),
  /** Female sex code. */
  FEMALE("2", Enumerations.AdministrativeGender.FEMALE, "248152002");

  private final String idrCode;
  private final Enumerations.AdministrativeGender administrativeGender;
  private final String usCoreSexCode;

  /**
   * Attempts to convert the IDR sex code to a valid representation, returning an empty value if it
   * is not found.
   *
   * @param idrCode IDR sex code
   * @return the {@link SexCode} if it is found, else None.
   */
  public static Optional<SexCode> tryFromIdrCode(String idrCode) {
    return Arrays.stream(values()).filter(v -> v.idrCode.equals(idrCode)).findFirst();
  }

  Enumerations.AdministrativeGender toFhirAdministrativeGender() {
    return this.administrativeGender;
  }

  Extension toFhirSexExtension() {
    return new Extension()
        .setValue(new CodeType().setValue(this.usCoreSexCode))
        .setUrl(SystemUrls.US_CORE_SEX);
  }
}
