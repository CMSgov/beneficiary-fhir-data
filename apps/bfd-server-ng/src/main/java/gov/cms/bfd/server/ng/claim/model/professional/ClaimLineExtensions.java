package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.ClaimServiceDeductibleCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSupplierTypeCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Extension;

/** Embedded container for professional claim line extensions. */
@Embeddable
public class ClaimLineExtensions {

  @Column(name = "clm_suplr_type_cd")
  private Optional<ClaimSupplierTypeCode> supplierTypeCode;

  @Column(name = "clm_srvc_ddctbl_sw")
  private Optional<ClaimServiceDeductibleCode> serviceDeductibleCode;

  /**
   * Return the shared extensions among profiles in professional claims.
   *
   * @return A list of Extensions
   */
  public List<Extension> toFhir() {
    return Stream.of(
            supplierTypeCode.map(ClaimSupplierTypeCode::toFhir),
            serviceDeductibleCode.map(ClaimServiceDeductibleCode::toFhir))
        .flatMap(Optional::stream)
        .toList();
  }
}
