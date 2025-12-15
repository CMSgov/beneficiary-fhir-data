package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.converter.NonZeroIntConverter;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.SimpleQuantity;

@Embeddable
class BloodPints {
  @Convert(converter = NonZeroIntConverter.class)
  @Column(name = "clm_blood_pt_frnsh_qty")
  private Optional<Integer> bloodPints;

  Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return bloodPints.map(
        pints ->
            supportingInfoFactory
                .createSupportingInfo()
                .setCategory(BlueButtonSupportingInfoCategory.CLM_BLOOD_PT_FRNSH_QTY.toFhir())
                .setValue(
                    new SimpleQuantity()
                        .setValue(pints)
                        .setSystem(SystemUrls.UNITS_OF_MEASURE)
                        .setUnit("pint")
                        .setCode("[pt_us]")));
  }
}
