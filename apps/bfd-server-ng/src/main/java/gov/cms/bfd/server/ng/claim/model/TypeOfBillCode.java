package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class TypeOfBillCode {
  @Column(name = "clm_bill_fac_type_cd")
  private Optional<String> facilityTypeCode;

  @Column(name = "clm_bill_clsfctn_cd")
  private Optional<String> billClassificationCode;

  @Column(name = "clm_bill_freq_cd")
  private Optional<String> billFrequencyCode;

  ExplanationOfBenefit.SupportingInformationComponent toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    var billClassificationCodeNormalized = billClassificationCode.orElse("");
    var facilityTypeCodeNormalized = facilityTypeCode.orElse("");
    var billFrequencyCodeNormalized = billFrequencyCode.orElse("");
    var codeableConcept =
        new CodeableConcept()
            .addCoding(
                new Coding()
                    .setSystem(SystemUrls.NUBC_TYPE_OF_BILL)
                    .setCode(
                        "0"
                            + facilityTypeCodeNormalized
                            + billClassificationCodeNormalized
                            + billFrequencyCodeNormalized));
    if (!facilityTypeCodeNormalized.isEmpty()) {
      codeableConcept.addCoding(
          new Coding()
              .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_FACILITY_TYPE_CODE)
              .setCode(facilityTypeCodeNormalized));
    }
    var billClassification = facilityTypeCodeNormalized + billClassificationCodeNormalized;
    if (!billClassification.isEmpty()) {
      codeableConcept.addCoding(
          new Coding()
              .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_BILL_CLASSIFICATION_CODE)
              .setCode(billClassification));
    }
    var frequencyCode =
        facilityTypeCodeNormalized + billClassificationCodeNormalized + billFrequencyCodeNormalized;
    if (!frequencyCode.isEmpty()) {
      codeableConcept.addCoding(
          new Coding()
              .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_BILL_FREQUENCY_CODE)
              .setCode(frequencyCode));
    }
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(CarinSupportingInfoCategory.TYPE_OF_BILL_CODE.toFhir())
        .setCode(codeableConcept);
  }
}
