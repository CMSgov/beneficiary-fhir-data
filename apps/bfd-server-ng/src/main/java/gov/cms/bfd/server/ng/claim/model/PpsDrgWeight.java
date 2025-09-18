package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Extension;

@Embeddable
class PpsDrgWeight {
  @Column(name = "clm_mdcr_ip_pps_drg_wt_num")
  private double ppsDrgWeight;

  ExplanationOfBenefit.AdjudicationComponent toFhir() {
    var adjudication = new ExplanationOfBenefit.AdjudicationComponent();
    adjudication.addExtension(
        new Extension()
            .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_PPS_DRG_WEIGHT_NUMBER)
            .setValue(new DecimalType(ppsDrgWeight)));
    adjudication.setReason(
        new CodeableConcept(
            new Coding()
                .setSystem(SystemUrls.CARIN_CODE_SYSTEM_PAYER_ADJUDICATION_STATUS)
                .setCode("other")
                .setDisplay("Other")));
    adjudication.setCategory(
        new CodeableConcept(
            new Coding()
                .setSystem(SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION_DISCRIMINATOR)
                .setCode("benefitpaymentstatus")
                .setDisplay("Benefit Payment Status")));
    return adjudication;
  }
}
