package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;

@Embeddable
public class ClaimRenderingProvider {

  @Column(name = "clm_rndrg_prvdr_tax_num")
  private Optional<String> taxNumber;

  @Column(name = "clm_rndrg_prvdr_npi_num")
  private Optional<String> npiNumber;

  @Column(name = "clm_rndrg_prvdr_prtcptg_cd")
  private Optional<String> participatingIndicatorCode;

  @Column(name = "clm_rndrg_prvdr_type_cd")
  private Optional<String> typeCode;

  Optional<Practitioner> toFhirPractitioner(Optional<Integer> claimLineNum) {

    if (npiNumber.isEmpty()
        && taxNumber.isEmpty()
        && participatingIndicatorCode.isEmpty()
        && typeCode.isEmpty()) {
      return Optional.empty();
    }


        var practitioner = new Practitioner();
        practitioner.setId("careteam-provider-line-"+claimLineNum);
        practitioner.setMeta(
                new Meta()
                        .addProfile(SystemUrls.PROFILE_CARIN_BB_PRACTITIONER_2_1_0)
                        .addProfile(SystemUrls.PROFILE_US_CORE_PRACTITIONER_6_1_0));
        npiNumber.ifPresent(s -> practitioner.addIdentifier(
                new Identifier()
                    .setType(
                        new CodeableConcept(
                            new Coding().setSystem(SystemUrls.HL7_IDENTIFIER).setCode("NPI")))
                    .setSystem(SystemUrls.NPI)
                    .setValue(s)));
    taxNumber.ifPresent(
        s ->
            practitioner.addIdentifier(
                new Identifier()
                    .setType(
                        new CodeableConcept(
                            new Coding().setSystem(SystemUrls.HL7_IDENTIFIER).setCode("TAX")))
                    .setSystem("urn:oid:2.16.840.1.113883.4.4")
                    .setValue(s)));
    practitioner.addName(new HumanName().setFamily("MOCK-FAMILY-NAME"));
    participatingIndicatorCode.ifPresent(
        s ->
            practitioner.addExtension(
                new Extension()
                    .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_PROVIDER_PARTICIPATING_CODE)
                    .setValue(
                        new Coding()
                            .setCode(s)
                            .setSystem(
                                SystemUrls.BLUE_BUTTON_CODE_SYSTEM_PROVIDER_PARTICIPATING_CODE))));
    typeCode.ifPresent(
        s ->
            practitioner.addExtension(
                new Extension()
                    .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_PROVIDER_TYPE_CODE)
                    .setValue(
                        new Coding()
                            .setCode(s)
                            .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_PROVIDER_TYPE_CODE))));

    return Optional.of(practitioner);
  }

    Optional<ExplanationOfBenefit.CareTeamComponent> toFhirCareTeam(Optional<Integer> claimLineNum) {
        if (claimLineNum.isEmpty()){
            return Optional.empty();
        }
        return Optional.of(new ExplanationOfBenefit.CareTeamComponent()
                .setSequence(claimLineNum.get())
                .setRole(
                        new CodeableConcept(
                                new Coding()
                                        .setSystem(SystemUrls.CARIN_CODE_SYSTEM_CLAIM_CARE_TEAM_ROLE)
                                        .setCode(CareTeamType.RENDERING.roleCode)
                                        .setDisplay(CareTeamType.RENDERING.roleDisplay)))
                .setProvider(new Reference("#careteam-provider-line-" + claimLineNum.get())));
    }
}
