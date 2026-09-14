package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
@Embeddable
public class ClaimLineNdcQuantity {

  @Embedded ClaimLineNdc claimLineNdc;

  @Column(name = "clm_line_ndc_qty")
  private Optional<Double> ndcQuantity;

  @Column(name = "clm_line_ndc_qty_qlfyr_cd")
  private Optional<IdrUnit> ndcQuantityQualifierCode;

  public Optional<ExplanationOfBenefit.DetailComponent> toFhirDetail() {
    if (claimLineNdc.getNdcCode().isEmpty()) {
      return Optional.empty();
    }
    var detail = new ExplanationOfBenefit.DetailComponent();
    detail.setSequence(1);
    detail.setProductOrService(
        new CodeableConcept(
            new Coding().setSystem(SystemUrls.NDC).setCode(claimLineNdc.getNdcCode().get())));
    ndcQuantityQualifierCode.ifPresent(c -> detail.setQuantity(c.toFhir(ndcQuantity.get())));
    return Optional.of(detail);
  }

  public Optional<Coding> toFhirCoding() {
    return claimLineNdc.getNdcCode().map(c -> new Coding().setSystem(SystemUrls.NDC).setCode(c));
  }

  public Optional<String> getQualifier() {
    return ndcQuantityQualifierCode.map(Object::toString);
  }
}
