package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.ClaimPricingLocalityCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSupplierTypeCode;
import gov.cms.bfd.server.ng.claim.model.common.ExtensionEmbedded;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Extension;

/** extensions for all NCH Professional claims. */
@Embeddable
public class ExtensionsProfessionalAllNch implements ExtensionEmbedded {

  @Column(name = "clm_line_ansthsa_unit_cnt") // ALL PROFILES
  private Optional<BigDecimal> anesthesiaUnitCount;

  @Column(name = "clm_rndrg_prvdr_type_cd") // ALL PROFILES
  private Optional<ClaimSupplierTypeCode> renderingProviderTypeCode;

  @Column(name = "clm_prcng_lclty_cd") // ALL PROFILES
  private Optional<ClaimPricingLocalityCode> pricingLocalityCode;

  @Override
  public List<Extension> toFhir() {
    var anesthesiaUnitCountExtension =
        anesthesiaUnitCount.map(
            v ->
                new Extension(SystemUrls.EXT_CLM_LINE_ANSTHSA_UNIT_CNT_URL)
                    .setValue(new DecimalType(v)));

    return Stream.of(
            pricingLocalityCode.map(ClaimPricingLocalityCode::toFhir),
            renderingProviderTypeCode.map(ClaimSupplierTypeCode::toFhir),
            anesthesiaUnitCountExtension)
        .flatMap(Optional::stream)
        .toList();
  }
}
