package gov.cms.bfd.server.ng.claim.model.common;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Latest Claim Occurrence event for "Not Covered From Date", the beginning of the period. */
@Embeddable
public class NoncoveredDatesNormalized {

  // from
  @Column(name = "bfd_clm_ncvrd_from_dt")
  private Optional<LocalDate> bfdNoncoveredFromDate;

  @Column(name = "clm_ncvrd_from_dt")
  private Optional<LocalDate> noncoveredFromDate;

  // thru
  @Column(name = "bfd_clm_ncvrd_thru_dt")
  private Optional<LocalDate> bfdNoncoveredThroughDate;

  @Column(name = "clm_ncvrd_thru_dt")
  private Optional<LocalDate> noncoveredThroughDate;

  @SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    // hierarchy per Clarissa's advisement

    //    return bfdNoncoveredFromDate.map(
    //        localDate ->
    //            supportingInfoFactory
    //                .createSupportingInfo()
    //                .setCategory(BlueButtonSupportingInfoCategory.CLM_NCVRD_FROM_DT.toFhir())
    //                .setTiming(new DateType().setValue(DateUtil.toDate(localDate))));
  }
}
