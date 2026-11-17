package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;

/** Embedded container for institutional claim line extensions. */
@Embeddable
public class ClaimLineInstitutionalExtensions {
  @Embedded ClaimRevenueDiscountIndicatorCode claimRevenueDiscountIndicatorCode;

  @Column(name = "clm_rev_packg_ind_cd")
  Optional<ClaimRevenuePackageIndicatorCode> claimRevenuePackageIndicatorCode;

  @Column(name = "clm_fac_prvdr_oscar_num")
  private Optional<String> clmFacPrvdrOscarNum;

  @Embedded ClaimRevenuePaymentMethodCode claimRevenuePaymentMethodCode;

  /**
   * Creates Fhir Extensions.
   *
   * @return List
   */
  List<Extension> toFhir() {
    var clmFacPrvdrOscarNumExtension =
        clmFacPrvdrOscarNum.map(
            s ->
                new Extension(SystemUrls.EXT_CLM_FAC_PRVDR_OSCAR_NUM_URL)
                    .setValue(new StringType(s)));

    return Stream.of(
            claimRevenueDiscountIndicatorCode.toFhir(),
            claimRevenuePackageIndicatorCode.map(ClaimRevenuePackageIndicatorCode::toFhir),
            claimRevenuePaymentMethodCode.toFhir(),
            clmFacPrvdrOscarNumExtension)
        .flatMap(Optional::stream)
        .toList();
  }
}
