package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimItemId;
import gov.cms.bfd.server.ng.claim.model.common.ProcedureBase;
import gov.cms.bfd.server.ng.claim.model.institutional.ClaimLineInstitutionalCmsSharedSystems;
import gov.cms.bfd.server.ng.claim.model.institutional.ClaimValue;
import gov.cms.bfd.server.ng.claim.model.institutional.ProcedureInstitutional;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim item table. */
@Getter
@Entity
@EqualsAndHashCode
@Table(name = "claim_item_institutional_ss", schema = "idr")
public class ClaimItemInstitutionalCmsSharedSystems implements ClaimItemBase {

  @EmbeddedId private ClaimItemId claimItemId;
  @Embedded private ClaimLineInstitutionalCmsSharedSystems claimLine;
  @Embedded private ProcedureInstitutional claimProcedure;
  @Embedded private ClaimValue claimValue;

  @JoinColumn(name = "clm_uniq_id")
  @ManyToOne
  private ClaimInstitutionalCmsSharedSystems claim;

  @Column(name = "clm_line_msp_coinsrnc_amt")
  private Optional<BigDecimal> benePaymentAmount;

  @Override
  public Optional<ProcedureBase> getProcedureOptional() {
    return Optional.of(claimProcedure);
  }

  /**
   * Creates FHIR ExplanationOfBenefit.AdjudicationComponent.
   *
   * @return Optional contains a list of ExplanationOfBenefit.AdjudicationComponent
   */
  public Optional<List<ExplanationOfBenefit.AdjudicationComponent>> toFhir() {
    return benePaymentAmount.map(
        bigDecimal ->
            List.of(AdjudicationChargeType.LINE_MSP_COINSRNC_AMT.toFhirAdjudication(bigDecimal)));
  }
}
