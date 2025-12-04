package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.SimpleQuantity;

@Embeddable
class BloodPints {
  @Column(name = "clm_blood_pt_frnsh_qty")
  private int bloodPints;

  Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    if (bloodPints <= 0) {
      return Optional.empty();
    }
    return Optional.of(
        supportingInfoFactory
            .createSupportingInfo()
            .setCategory(BlueButtonSupportingInfoCategory.CLM_BLOOD_PT_FRNSH_QTY.toFhir())
            .setValue(
                new SimpleQuantity()
                    .setValue(bloodPints)
                    .setSystem(SystemUrls.UNITS_OF_MEASURE)
                    .setUnit("pint")
                    .setCode("[pt_us]")));
  }
}
