package gov.cms.bfd.server.ng.claim.model.rx.entities;

import jakarta.annotation.Generated;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The basis profile of a Rx Claim. */
@Generated("TODO - Remove after query optimization implementation")
public class ClaimBasisRx extends ClaimRxBase {
  @Override
  public List<ExplanationOfBenefit.SupportingInformationComponent> buildHeaderSupportingInfo() {
    return List.of();
  }
}
