package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.SystemUrl;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;

@AllArgsConstructor
public enum SexCode {
  MALE("1", Enumerations.AdministrativeGender.MALE, "248153007"),
  FEMALE("2", Enumerations.AdministrativeGender.FEMALE, "248152002");

  private final String idrCode;
  private final Enumerations.AdministrativeGender administrativeGender;
  private final String usCoreSexCode;

  public static Optional<SexCode> tryFromIdrCode(String idrCode) {
    return Arrays.stream(values()).filter(v -> v.idrCode.equals(idrCode)).findFirst();
  }

  Enumerations.AdministrativeGender toFhirAdministrativeGender() {
    return this.administrativeGender;
  }

  Extension toFhirSexExtension() {
    return new Extension()
        .setValue(new CodeType().setValue(this.usCoreSexCode))
        .setUrl(SystemUrl.US_CORE_SEX);
  }
}
