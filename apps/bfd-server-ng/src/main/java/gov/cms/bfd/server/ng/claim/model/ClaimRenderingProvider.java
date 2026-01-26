package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Reference;

/** Claim Rendering provider info. * */
@Embeddable
public class ClaimRenderingProvider {

  @Column(name = "clm_rndrg_prvdr_prtcptg_cd")
  private Optional<String> participatingIndicatorCode;

  @Column(name = "clm_rndrg_prvdr_type_cd")
  private Optional<String> typeCode;

  Optional<CareTeamType.CareTeamComponents> toFhirCareTeam(
      Optional<Integer> claimLineNum, Optional<ProviderHistory> renderingProviderOpt) {
    if (claimLineNum.isEmpty() || renderingProviderOpt.isEmpty()) {
      return Optional.empty();
    }
    var renderingProvider = renderingProviderOpt.get();
    var practitioner =
        ProviderFhirHelper.createPractitioner(
            "careteam-provider-line-" + claimLineNum.get(),
            renderingProvider.getProviderNpiNumber(),
            renderingProvider.toFhirName());

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

    var component =
        new ExplanationOfBenefit.CareTeamComponent()
            .setSequence(claimLineNum.get())
            .setRole(
                new CodeableConcept(
                    new Coding()
                        .setSystem(SystemUrls.CARIN_CODE_SYSTEM_CLAIM_CARE_TEAM_ROLE)
                        .setCode(CareTeamType.RENDERING.getRoleCode())
                        .setDisplay(CareTeamType.RENDERING.getRoleDisplay())))
            .setProvider(new Reference("#careteam-provider-line-" + claimLineNum.get()));

    return Optional.of(new CareTeamType.CareTeamComponents(practitioner, component));
  }
}
