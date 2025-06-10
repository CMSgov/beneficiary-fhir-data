package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.SimpleQuantity;

@Embeddable
public class BloodPints {
  @Column(name = "clm_blood_pt_frnsh_qty")
  private int bloodPints;

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(BlueButtonSupportingInfoCategory.BLOOD_PINTS.toFhir())
        .setValue(
            new SimpleQuantity()
                .setValue(bloodPints)
                .setSystem(SystemUrls.UNITS_OF_MEASURE)
                .setUnit("pint")
                .setCode("[pt_us]"));
  }
}
