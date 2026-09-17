package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.ClaimLineHcpcsCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineNdc;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineServiceUnitQuantity;
import gov.cms.bfd.server.ng.util.FhirUtil;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;

/** Embeddable that contains core NCH clm_line properties and logic. */
@Embeddable
public class ClaimLineProfessionalNchCore {

  @Column(name = "clm_line_carr_clncl_lab_num") // ALL PROFILES
  private Optional<String> claimLineCarrierClinicalLabNumber;

  @Embedded private ClaimLineNdc ndc;

  // region Shared NCH Logic
  void populateProductAndQuantity(
      ExplanationOfBenefit.ItemComponent item,
      ClaimLineHcpcsCode hcpcsCode,
      ClaimLineServiceUnitQuantity serviceUnitQuantity) {
    var productOrService = new CodeableConcept();
    hcpcsCode.toFhir().ifPresent(productOrService::addCoding);
    ndc.toFhirDetail().ifPresent(item::addDetail);
    item.setQuantity(serviceUnitQuantity.toFhir());
    item.setProductOrService(FhirUtil.checkDataAbsent(productOrService));
  }

  void addClinicalLabPerformer(Observation observation) {
    claimLineCarrierClinicalLabNumber.ifPresent(
        labNumber -> {
          var identifier = new Identifier().setSystem(SystemUrls.CLIA).setValue(labNumber);
          observation.addPerformer(new Reference().setIdentifier(identifier));
        });
  }
  // endregion
}
