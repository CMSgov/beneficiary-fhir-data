package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
@Getter
@Embeddable
public class ClaimLineNdc {

  @Column(name = "clm_line_ndc_cd")
  private Optional<String> ndcCode;

  public Optional<ExplanationOfBenefit.DetailComponent> toFhirDetail() {
    if (ndcCode.isEmpty()) {
      return Optional.empty();
    }
    var detail = new ExplanationOfBenefit.DetailComponent();
    detail.setSequence(1);
    detail.setProductOrService(
        new CodeableConcept(new Coding().setSystem(SystemUrls.NDC).setCode(ndcCode.get())));
    return Optional.of(detail);
  }

  public Optional<Coding> toFhirCoding() {
    return ndcCode.map(c -> new Coding().setSystem(SystemUrls.NDC).setCode(c));
  }
}
