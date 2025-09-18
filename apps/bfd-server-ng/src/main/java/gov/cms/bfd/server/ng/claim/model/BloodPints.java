package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.SimpleQuantity;

@Embeddable
class BloodPints {
  @Column(name = "clm_blood_pt_frnsh_qty")
  private int bloodPints;

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(BlueButtonSupportingInfoCategory.CLM_BLOOD_PT_FRNSH_QTY.toFhir())
        .setValue(
            new SimpleQuantity()
                .setValue(bloodPints)
                .setSystem(SystemUrls.UNITS_OF_MEASURE)
                .setUnit("pint")
                .setCode("[pt_us]"));
  }
}
