package gov.cms.bfd.server.ng.claim.model.rx;

import gov.cms.bfd.server.ng.claim.model.common.ClaimLineBrandGenericCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineCompoundCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimPrescriptionOriginCode;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoFactory;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Rx claim line info for BASIS, REGULAR, and CMS profiles. */
@SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
@Embeddable
@Getter
public class ClaimLineRxSupportingInfo {

  @Column(name = "clm_line_rx_orgn_cd")
  private Optional<ClaimPrescriptionOriginCode> claimPrescriptionOriginCode;

  @Column(name = "clm_brnd_gnrc_cd")
  private Optional<ClaimLineBrandGenericCode> brandGenericCode;

  @Column(name = "clm_cmpnd_cd")
  private Optional<ClaimLineCompoundCode> compoundCode;

  @Column(name = "clm_daw_prod_slctn_cd")
  private Optional<ClaimDispenseAsWrittenCode> claimDispenseAsWrittenCode;

  @Embedded private ClaimLineRxDaysSupplyQuantity daysSupply;
  @Embedded private ClaimLineRxFillNumber fillNumber;

  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return Stream.of(
            claimPrescriptionOriginCode.map(c -> c.toFhir(supportingInfoFactory)),
            brandGenericCode.map(s -> s.toFhir(supportingInfoFactory)),
            compoundCode.map(s -> s.toFhir(supportingInfoFactory)),
            claimDispenseAsWrittenCode.map(c -> c.toFhir(supportingInfoFactory)),
            Optional.of(daysSupply.toFhir(supportingInfoFactory)),
            Optional.of(fillNumber.toFhir(supportingInfoFactory)))
        .flatMap(Optional::stream)
        .toList();
  }

  /**
   * Per C4BB, if compound code = 2 -> populate productOrService with "compound".
   *
   * @return Optional containing the coding if applicable, otherwise empty
   */
  public Optional<Coding> toFhirNdcCompound() {
    return compoundCode
        .filter(c -> c.getCode().equals("2"))
        .map(c -> new Coding().setSystem(SystemUrls.CARIN_COMPOUND_LITERAL).setCode("compound"));
  }
}
