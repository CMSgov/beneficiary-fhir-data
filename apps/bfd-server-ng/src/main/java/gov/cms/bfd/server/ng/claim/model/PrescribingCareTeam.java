package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Attending Provider History. * */
@Embeddable
@AttributeOverride(
    name = "providerNpiNumber",
    column = @Column(name = "prvdr_prscrbng_prvdr_npi_num"))
@AttributeOverride(
    name = "providerName",
    column = @Column(name = "bfd_prvdr_prscrbng_careteam_name"))
public class PrescribingCareTeam extends ProviderHistoryBase {

  @Column(name = "prvdr_prsbng_id_qlfyr_cd")
  private Optional<ProviderIdQualifierCode> providerQualifierCode;

  @Override
  protected CareTeamType getCareTeamType() {
    return CareTeamType.PRESCRIBING;
  }

  @Override
  Optional<ExplanationOfBenefit.CareTeamComponent> toFhirCareTeamComponent(Integer sequence) {

    return getProviderNpiNumber()
        .flatMap(
            npi ->
                providerQualifierCode.map(
                    qualifier -> {
                      var providerReference =
                          ProviderFhirHelper.createProviderReference(
                              npi, qualifier, getProviderName());

                      var role = getCareTeamType();

                      return new ExplanationOfBenefit.CareTeamComponent()
                          .setSequence(sequence)
                          .setRole(
                              new CodeableConcept(
                                  new Coding()
                                      .setSystem(SystemUrls.CARIN_CODE_SYSTEM_CLAIM_CARE_TEAM_ROLE)
                                      .setCode(role.getRoleCode())
                                      .setDisplay(role.getRoleDisplay())))
                          .setProvider(providerReference);
                    }));
  }
}
