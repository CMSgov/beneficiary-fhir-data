package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.ClaimFiscalIntermediaryActionCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimNonpaymentReasonCode;
import gov.cms.bfd.server.ng.claim.model.common.McoPaidSwitch;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoComponentBase;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoFactory;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Supporting info for CMS profile shared systems source. */
public class InstitutionalSupportingInfoCmsSharedSystems implements SupportingInfoComponentBase {

  @Embedded private InstitutionalSupportingInfo claimInstitutionalSupportingInfo;

  @Column(name = "clm_mdcr_instnl_mco_pd_sw")
  private Optional<McoPaidSwitch> mcoPaidSwitch;

  @Column(name = "clm_mdcr_npmt_rsn_cd")
  private Optional<ClaimNonpaymentReasonCode> nonpaymentReasonCode;

  // This is the fiscal intermediary action code, not final action!
  @Column(name = "clm_fi_actn_cd")
  private Optional<ClaimFiscalIntermediaryActionCode> claimFiscalIntermediaryActionCode;

  @Override
  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return Stream.concat(
            claimInstitutionalSupportingInfo.toFhir(supportingInfoFactory).stream(),
            Stream.of(
                    mcoPaidSwitch.map(s -> s.toFhir(supportingInfoFactory)),
                    nonpaymentReasonCode.map(c -> c.toFhir(supportingInfoFactory)),
                    claimFiscalIntermediaryActionCode.map(c -> c.toFhir(supportingInfoFactory)))
                .flatMap(Optional::stream))
        .toList();
  }
}
