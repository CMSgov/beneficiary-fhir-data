package gov.cms.bfd.server.ng.claim.model.rx;

import gov.cms.bfd.server.ng.claim.model.common.*;
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
@AttributeOverride(name = "npiType", column = @Column(name = "bfd_prvdr_prscrbng_npi_type"))
public class PrescribingCareTeam extends ProviderHistoryBase {

  @Column(name = "prvdr_prsbng_id_qlfyr_cd")
  private Optional<ProviderIdQualifierCode> providerQualifierCode;

  @Override
  public CareTeamType getCareTeamType(Optional<ClaimTypeCode> claimTypeCode) {
    return CareTeamType.PRESCRIBING;
  }

  @Override
  public Optional<ExplanationOfBenefit.CareTeamComponent> toFhirCareTeamComponent(
      Integer sequence, Optional<ClaimTypeCode> claimTypeCode) {

    return getProviderNpiNumber()
        .flatMap(
            npi ->
                providerQualifierCode.flatMap(
                    qualifier -> {
                      var providerReference =
                          ProviderFhirHelper.createProviderReferenceWithQualifier(
                              npi, qualifier, getProviderName());

                      var providerNpiType = getNpiType();
                      if (providerNpiType != NpiType.UNKNOWN) {
                        providerReference.setType(providerNpiType.getType());
                      }

                      return getCareTeamComponent(sequence, providerReference, claimTypeCode);
                    }));
  }
}
