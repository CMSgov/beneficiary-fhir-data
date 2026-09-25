package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Optional;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
@Embeddable
public class QualifyStayFromDate {

  @Column(name = "bfd_clm_qlfy_stay_from_dt")
  private Optional<LocalDate> bfdQualifyStayFromDate;

  @SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
  public Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    if (bfdQualifyStayFromDate.isEmpty()) {
      return Optional.empty();
    }

    var component =
        supportingInfoFactory
            .createSupportingInfo()
            .setCategory(BlueButtonSupportingInfoCategory.CLM_QLFY_STAY_FROM_DT.toFhir())
            .setTiming(new DateType().setValue(DateUtil.toDate(bfdQualifyStayFromDate.get())));
    return Optional.of(component);
  }
}
