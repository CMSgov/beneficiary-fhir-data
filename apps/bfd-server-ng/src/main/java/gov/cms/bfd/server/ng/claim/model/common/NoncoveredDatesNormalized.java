package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Latest Claim Occurrence event for "Not Covered From Date", the beginning of the period. */
@Embeddable
public class NoncoveredDatesNormalized {

  // CLM_DT_SGNTR NCVRD
  @Column(name = "clm_ncvrd_from_dt")
  private Optional<LocalDate> noncoveredFromDate;

  @Column(name = "clm_ncvrd_thru_dt")
  private Optional<LocalDate> noncoveredThroughDate;

  // CLM_OCRNC_SGNTR_MBR NCVRD
  @Column(name = "bfd_clm_ncvrd_from_dt")
  private Optional<LocalDate> bfdNoncoveredFromDate;

  @Column(name = "bfd_clm_ncvrd_thru_dt")
  private Optional<LocalDate> bfdNoncoveredThroughDate;

  @SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    // date compare against LocalDate.min for possible null values going last
    Comparator<Optional<LocalDate>> date = Comparator.comparing(o -> o.orElse(LocalDate.MIN));
    // decision-hierarchy per BFD-4907 research:
    // One set complete: the complete set wins on the first comparison, whatever the other set’s
    // thru is.
    // Both complete: the higher thru wins, and its own from comes with it.
    // Matching thrus: a complete set beats a partial one. If both are complete, the higher from
    // wins.
    // Both incomplete: a set with a thru beats a set without one, and the higher thru wins. If
    // neither has a thru, the higher from wins.
    Comparator<List<Optional<LocalDate>>> best =
        Comparator.<List<Optional<LocalDate>>, Boolean>comparing(
                s -> s.getFirst().isPresent() && s.getLast().isPresent())
            .thenComparing(List::getLast, date)
            .thenComparing(List::getFirst, date);

    return Stream.of(
            List.of(noncoveredFromDate, noncoveredThroughDate),
            List.of(bfdNoncoveredFromDate, bfdNoncoveredThroughDate))
        .max(best)
        .map(
            s ->
                Stream.of(
                        s.getFirst()
                            .map(
                                d ->
                                    toSupportingInfo(
                                        d,
                                        BlueButtonSupportingInfoCategory.CLM_NCVRD_FROM_DT,
                                        supportingInfoFactory)),
                        s.getLast()
                            .map(
                                d ->
                                    toSupportingInfo(
                                        d,
                                        BlueButtonSupportingInfoCategory.CLM_NCVRD_THRU_DT,
                                        supportingInfoFactory)))
                    .flatMap(Optional::stream)
                    .toList())
        .orElseGet(List::of);
  }

  private ExplanationOfBenefit.SupportingInformationComponent toSupportingInfo(
      LocalDate localDate,
      BlueButtonSupportingInfoCategory category,
      SupportingInfoFactory supportingInfoFactory) {
    if (Objects.isNull(localDate)) return null;
    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(category.toFhir())
        .setTiming(new DateType().setValue(DateUtil.toDate(localDate)));
  }
}
