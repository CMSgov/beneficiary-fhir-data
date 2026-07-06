package gov.cms.bfd.server.ng.claim.model.rx;

import gov.cms.bfd.server.ng.claim.model.common.ProviderFhirHelper;
import gov.cms.bfd.server.ng.claim.model.common.ProviderIdQualifierCode;
import gov.cms.bfd.server.ng.claim.model.common.CareTeamType;
import gov.cms.bfd.server.ng.claim.model.common.ClaimContext;
import gov.cms.bfd.server.ng.claim.model.common.ProviderHistoryBase;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Prescribing Provider History. * */
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
  public CareTeamType getCareTeamType() {
    return CareTeamType.PRESCRIBING;
  }

  @Override
  public Optional<ExplanationOfBenefit.CareTeamComponent> toFhirCareTeamComponent(
      Integer sequence, Optional<ClaimContext> claimContext) {

    return getProviderNpiNumber()
        .flatMap(
            npi ->
                providerQualifierCode.flatMap(
                    qualifier -> {
                      var providerReference =
                          ProviderFhirHelper.createProviderReferenceWithQualifier(
                              npi, qualifier, getProviderName());
                      providerReference.setType(NpiType.INDIVIDUAL.getType());

                      return getCareTeamComponent(sequence, providerReference);
                    }));
  }
}
