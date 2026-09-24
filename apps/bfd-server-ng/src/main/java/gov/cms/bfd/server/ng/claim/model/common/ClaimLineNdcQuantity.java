package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.converter.NonZeroBigDecimalConverter;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.math.BigDecimal;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
@Embeddable
public class ClaimLineNdcQuantity {

  @Embedded ClaimLineNdc claimLineNdc;

  @Column(name = "clm_line_ndc_qty")
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> ndcQuantity;

  @Column(name = "clm_line_ndc_qty_qlfyr_cd")
  private Optional<IdrUnit> ndcQuantityQualifierCode;

  public Optional<ExplanationOfBenefit.DetailComponent> toFhirDetail() {
    return claimLineNdc
        .getNdcCode()
        .map(
            code -> {
              var detail = new ExplanationOfBenefit.DetailComponent();
              detail.setSequence(1);
              detail.setProductOrService(
                  new CodeableConcept(new Coding().setSystem(SystemUrls.NDC).setCode(code)));
              ndcQuantityQualifierCode
                  .flatMap(c -> ndcQuantity.map(c::toFhir))
                  .ifPresent(detail::setQuantity);
              return detail;
            });
  }

  public Optional<Coding> toFhirCoding() {
    return claimLineNdc.getNdcCode().map(c -> new Coding().setSystem(SystemUrls.NDC).setCode(c));
  }

  public Optional<String> getQualifier() {
    return ndcQuantityQualifierCode.map(Object::toString);
  }
}
