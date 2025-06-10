package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
public class TypeOfBillCode {
  @Column(name = "clm_bill_fac_type_cd")
  private String facilityTypeCode;

  @Column(name = "clm_bill_clsfctn_cd")
  private String billClassificationCode;

  @Column(name = "clm_bill_freq_cd")
  private String billFrequencyCode;

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(CarinSupportingInfoCategory.TYPE_OF_BILL_CODE.toFhir())
        .setCode(
            new CodeableConcept()
                .addCoding(
                    new Coding()
                        .setSystem(SystemUrls.NUBC_TYPE_OF_BILL)
                        .setCode(
                            "0" + facilityTypeCode + billClassificationCode + billFrequencyCode))
                .addCoding(
                    new Coding()
                        .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_BILL_CLASSIFICATION_CODE)
                        .setCode(facilityTypeCode))
                .addCoding(
                    new Coding()
                        .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_BILL_CLASSIFICATION_CODE)
                        .setCode(billClassificationCode))
                .addCoding(
                    new Coding()
                        .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_BILL_FREQUENCY_CODE)
                        .setCode(billFrequencyCode)));
  }
}
