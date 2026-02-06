package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.FhirUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.time.LocalDate;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim line info. */
@Embeddable
@Getter
@SuppressWarnings("java:S2201")
public class ClaimLineRxInfo {

  @Column(name = "clm_line_from_dt")
  private Optional<LocalDate> fromDate;

  @Embedded private ClaimLineNdc ndc;
  @Embedded private ClaimLineServiceUnitQuantity serviceUnitQuantity;
  @Embedded private ClaimLineAdjudicationChargeRxInfo adjudicationCharge;
  @Embedded private ClaimLineRxSupportingInfo claimRxSupportingInfo;

  Optional<ExplanationOfBenefit.ItemComponent> toFhirItemComponent() {

    var line = new ExplanationOfBenefit.ItemComponent();
    line.setSequence(1);
    var productOrService = new CodeableConcept();
    var quantity = serviceUnitQuantity.toFhir();
    claimRxSupportingInfo.toFhirNdcCompound().ifPresent(productOrService::addCoding);

    if (productOrService.isEmpty()) {
      ndc.toFhirCoding().ifPresent(productOrService::addCoding);
      ndc.getQualifier().ifPresent(quantity::setUnit);
    }

    line.setProductOrService(FhirUtil.checkDataAbsent(productOrService));
    ndc.toFhirDetail().ifPresent(line::addDetail);
    line.setQuantity(quantity);

    fromDate.map(d -> line.setServiced(new DateType(DateUtil.toDate(d))));

    adjudicationCharge.toFhir().forEach(line::addAdjudication);

    return Optional.of(line);
  }
}
