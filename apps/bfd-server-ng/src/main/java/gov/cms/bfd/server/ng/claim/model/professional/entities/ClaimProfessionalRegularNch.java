package gov.cms.bfd.server.ng.claim.model.professional.entities;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationEmbedded;
import gov.cms.bfd.server.ng.claim.model.common.ClaimRecordType;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSourceId;
import gov.cms.bfd.server.ng.claim.model.common.NchClaim;
import gov.cms.bfd.server.ng.claim.model.professional.AdjudicationProfessionalNch;
import gov.cms.bfd.server.ng.claim.model.professional.ClaimProfessionalNchCore;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import javax.annotation.processing.Generated;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The professional claim, regular profile, sourced from nch. */
@Entity
@Table(name = "claim_professional_nch", schema = "idr")
@Generated("TODO - Remove after query optimization implementation")
public class ClaimProfessionalRegularNch extends ClaimProfessionalRegularBase implements NchClaim {

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "clm_uniq_id")
  private SortedSet<ClaimItemProfessionalRegularNch> claimItems;

  @Embedded private AdjudicationProfessionalNch adjudicationCharge;
  @Embedded private ClaimProfessionalNchCore nchCore;

  @Override
  protected List<ExplanationOfBenefit.SupportingInformationComponent> getSubclassSupportingInfo() {
    var result = new ArrayList<>(super.getSubclassSupportingInfo());
    nchCore.getClaimQueryCode().map(c -> c.toFhir(supportingInfoFactory)).ifPresent(result::add);
    return result;
  }

  @Override
  protected void addSubclassCareTeam(
      ExplanationOfBenefit eob, SequenceGenerator sequenceGenerator) {
    nchCore.addServiceCareTeam(eob, sequenceGenerator, getClaimTypeCode());
  }

  @Override
  public Optional<ClaimRecordType> getClaimRecordTypeOptional() {
    return Optional.of(nchCore.getClaimRecordType());
  }

  @Override
  public ClaimSourceId getClaimSourceId() {
    return ClaimSourceId.NATIONAL_CLAIMS_HISTORY;
  }

  @Override
  public Optional<AdjudicationEmbedded> getAdjudication() {
    return Optional.of(adjudicationCharge);
  }
}
