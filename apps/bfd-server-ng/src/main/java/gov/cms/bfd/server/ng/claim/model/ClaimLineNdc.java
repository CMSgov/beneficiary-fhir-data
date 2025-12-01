package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class ClaimLineNdc {
  @Column(name = "clm_line_ndc_cd")
  private Optional<String> ndcCode;

  @Column(name = "clm_line_ndc_qty")
  private double ndcQuantity;

  @Column(name = "clm_line_ndc_qty_qlfyr_cd")
  private Optional<IdrUnit> ndcQuantityQualifierCode;

  Optional<ExplanationOfBenefit.DetailComponent> toFhirDetail() {
    if (ndcCode.isEmpty()) {
      return Optional.empty();
    }
    var detail = new ExplanationOfBenefit.DetailComponent();
    detail.setSequence(1);
    detail.setProductOrService(
        new CodeableConcept(new Coding().setSystem(SystemUrls.NDC).setCode(ndcCode.get())));
    ndcQuantityQualifierCode.ifPresent(c -> detail.setQuantity(c.toFhir(ndcQuantity)));
    return Optional.of(detail);
  }

  Optional<Coding> toFhir() {
    if (ndcCode.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new Coding().setSystem(SystemUrls.NDC).setCode(ndcCode.get()));
  }

  Optional<String> getQualifier() {
    if (ndcQuantityQualifierCode.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(ndcQuantityQualifierCode.get().toString());
  }
}
