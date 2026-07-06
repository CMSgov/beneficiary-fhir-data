package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.ClaimFilterOptions;
import gov.cms.bfd.server.ng.claim.model.common.ClaimFederalTypeOfServiceCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimPaymentCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimServiceDeductibleCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSupplierTypeCode;
import gov.cms.bfd.server.ng.claim.model.common.ProviderSpecialtyCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Extension;

/** Embedded container for professional claim line extensions. */
@SuppressWarnings("checkstyle:MissingJavadocMethod")
@Embeddable
public class ClaimLineProfessionalExtensions {

  @Column(name = "clm_suplr_type_cd")
  private Optional<ClaimSupplierTypeCode> supplierTypeCode;

  @Column(name = "clm_fed_type_srvc_cd")
  private Optional<ClaimFederalTypeOfServiceCode> federalTypeOfServiceCode;

  @Column(name = "clm_pmt_80_100_cd")
  private Optional<ClaimPaymentCode> paymentCode;

  @Column(name = "clm_prvdr_spclty_cd")
  private Optional<ProviderSpecialtyCode> providerSpecialtyCode;

  @Column(name = "clm_srvc_ddctbl_sw")
  private Optional<ClaimServiceDeductibleCode> serviceDeductibleCode;

  @Embedded ClaimTaxNumberCode claimTaxNumberCode;

  public List<Extension> toFhir(ClaimFilterOptions options) {
    return Stream.of(
            supplierTypeCode.map(ClaimSupplierTypeCode::toFhir),
            federalTypeOfServiceCode.map(ClaimFederalTypeOfServiceCode::toFhir),
            paymentCode.map(ClaimPaymentCode::toFhir),
            providerSpecialtyCode.map(ProviderSpecialtyCode::toFhirExtension),
            serviceDeductibleCode.map(ClaimServiceDeductibleCode::toFhir),
            options.isIncludeTaxNumber()
                ? claimTaxNumberCode.toFhir()
                : Optional.<Extension>empty())
        .flatMap(Optional::stream)
        .toList();
  }
}
