package gov.cms.bfd.server.ng.claim.model.institutional;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Extension;

/** Embedded container for institutional claim line extensions. */
@Embeddable
public class ClaimLineInstitutionalExtensions {
  @Embedded ClaimRevenueDiscountIndicatorCode claimRevenueDiscountIndicatorCode;

  @Column(name = "clm_rev_packg_ind_cd")
  Optional<ClaimRevenuePackageIndicatorCode> claimRevenuePackageIndicatorCode;

  @Embedded ClaimRevenuePaymentMethodCode claimRevenuePaymentMethodCode;

  /**
   * Creates Fhir Extensions.
   *
   * @return List
   */
  List<Extension> toFhir() {
    return Stream.of(
            claimRevenueDiscountIndicatorCode.toFhir(),
            claimRevenuePackageIndicatorCode.map(ClaimRevenuePackageIndicatorCode::toFhir),
            claimRevenuePaymentMethodCode.toFhir())
        .flatMap(Optional::stream)
        .toList();
  }
}
