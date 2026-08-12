package gov.cms.bfd.server.ng.claim.model;

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
  @Column(name = "bfd_prvdr_prscrbng_npi_type")
  private Optional<Integer> npiType;

  @Column(name = "prvdr_prsbng_id_qlfyr_cd")
  private Optional<ProviderIdQualifierCode> providerQualifierCode;

  @Override
  protected CareTeamType getCareTeamType() {
    return CareTeamType.PRESCRIBING;
  }

  @Override
  protected NpiType getNpiType() {
    return NpiType.fromNpiTypeCode(npiType);
  }

  @Override
  Optional<ExplanationOfBenefit.CareTeamComponent> toFhirCareTeamComponent(
      Integer sequence, Optional<ClaimContext> claimContext) {

    return getProviderNpiNumber()
        .flatMap(
            npi ->
                providerQualifierCode.flatMap(
                    qualifier -> {
                      var providerReference =
                          ProviderFhirHelper.createProviderReferenceWithQualifier(
                              npi, qualifier, getProviderName());

                      var npiType = getNpiType();
                      if (npiType != NpiType.UNKNOWN) {
                        providerReference.setType(npiType.getType());
                      }

                      return getCareTeamComponent(sequence, providerReference);
                    }));
  }
}
