package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Extension;

/** Embedded container for professional claim line NCH extensions. */
@Embeddable
public class ClaimLineProfessionalNchExtensions {

  @Column(name = "clm_mtus_ind_cd")
  private Optional<CarrierLineMTUSIndicatorCode> unitsIndicatorCode;

  @Column(name = "clm_line_prfnl_mtus_cnt")
  private BigDecimal totalUnitsCount;

  @Column(name = "clm_prcng_lclty_cd")
  private Optional<ClaimPricingLocalityCode> pricingLocalityCode;

  @Column(name = "clm_physn_astnt_cd")
  private Optional<ReducedPaymentPhysicianAssistantCode> reducedPaymentPhysicianAssistantCode;

  @Column(name = "clm_line_carr_hpsa_scrcty_cd")
  private Optional<HealthProfessionalShortageAreaScarcityCode>
      healthProfessionalShortageAreaScarcityCode;

  @Column(name = "clm_prmry_pyr_cd")
  private Optional<ClaimPrimaryPayerCode> primaryPayerCode;

  @Column(name = "clm_prcsg_ind_cd")
  private Optional<ClaimProcessingIndicatorCode> processingIndicatorCode;

  @Column(name = "clm_line_ansthsa_unit_cnt")
  private BigDecimal anesthesiaUnitCount;

  @Column(name = "clm_rndrg_prvdr_type_cd")
  private Optional<ClaimSupplierTypeCode> renderingProviderTypeCode;

  List<Extension> toFhir() {
    var totalUnitsCountExtension =
        new Extension(SystemUrls.EXT_CLM_LINE_PRFNL_MTUS_CNT_URL)
            .setValue(new DecimalType(totalUnitsCount));

    var anesthesiaUnitCountExtension =
        new Extension(SystemUrls.EXT_CLM_LINE_ANSTHSA_UNIT_CNT_URL)
            .setValue(new DecimalType(anesthesiaUnitCount));

    return Stream.of(
            unitsIndicatorCode.map(CarrierLineMTUSIndicatorCode::toFhir),
            pricingLocalityCode.map(ClaimPricingLocalityCode::toFhir),
            reducedPaymentPhysicianAssistantCode.map(ReducedPaymentPhysicianAssistantCode::toFhir),
            healthProfessionalShortageAreaScarcityCode.map(
                HealthProfessionalShortageAreaScarcityCode::toFhir),
            primaryPayerCode.map(ClaimPrimaryPayerCode::toFhir),
            processingIndicatorCode.map(ClaimProcessingIndicatorCode::toFhir),
            renderingProviderTypeCode.map(ClaimSupplierTypeCode::toFhir),
            Optional.of(anesthesiaUnitCountExtension),
            Optional.of(totalUnitsCountExtension))
        .flatMap(Optional::stream)
        .toList();
  }
}
