package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Organization;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum ClaimTypeCode {
  PART_D_ORIGINAL(
      1,
      ClaimType.PHARMACY,
      Optional.empty(),
      SystemUrls.CARIN_STRUCTURE_DEFINITION_PHARMACY,
      "MEDICARE PART D ORIGINAL CLAIM");

  private int code;
  private ClaimType claimType;
  private Optional<ClaimSubtype> claimSubtype;
  private String profileMetaUrl;
  private String display;
  private static final String INSURER_ORG = "insurer-org";

  public static ClaimTypeCode fromCode(int code) {
    return Arrays.stream(values()).filter(c -> c.code == code).findFirst().get();
  }

  CodeableConcept toFhirType() {
    return new CodeableConcept()
        .setCoding(
            List.of(
                new Coding()
                    .setSystem(SystemUrls.BLUE_BUTTON_CLAIM_TYPE_CODE)
                    .setCode(String.valueOf(code))
                    .setDisplay(display),
                new Coding().setSystem(SystemUrls.HL7_CLAIM_TYPE).setCode(claimType.getCode())));
  }

  Optional<CodeableConcept> toFhirSubtype() {
    return claimSubtype.map(
        subtype ->
            new CodeableConcept()
                .setCoding(
                    List.of(
                        new Coding()
                            .setSystem(SystemUrls.CARIN_CLAIM_SUBTYPE)
                            .setCode(subtype.getCode()))));
  }

  Optional<Organization> toFhirInsurerPartAB() {
    if (!isBetween(5, 3999)) {
      return Optional.empty();
    }

    var organization = OrganizationFactory.toFhir();
    organization.setId(INSURER_ORG);
    organization.setName("Centers for Medicare and Medicaid Services");
    return Optional.of(organization);
  }

  boolean isBetween(int lower, int upper) {
    return (code >= lower) && (code <= upper);
  }
}
