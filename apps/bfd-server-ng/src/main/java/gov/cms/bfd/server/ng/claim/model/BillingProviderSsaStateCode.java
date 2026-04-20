package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SsaToFipsStateCode;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

@Embeddable
class BillingProviderSsaStateCode {
  @Column(name = "geo_blg_ssa_state_cd")
  private Optional<String> value;

  Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return value
        .flatMap(SsaToFipsStateCode::toFips)
        .map(
            fips -> supportingInfoFactory
                .createSupportingInfo()
                .setCategory(BlueButtonSupportingInfoCategory.PRVDR_FAC_FIPS_ST_CD.toFhir())
                .setCode(new CodeableConcept().addCoding(
                    new Coding()
                        .setSystem(SystemUrls.US_FIPS_STATE_CODES)
                        .setCode(fips))));
  }
}
