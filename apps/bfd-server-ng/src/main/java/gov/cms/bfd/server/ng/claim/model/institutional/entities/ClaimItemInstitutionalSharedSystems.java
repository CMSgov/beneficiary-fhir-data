package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import gov.cms.bfd.server.ng.claim.model.common.ClaimItemBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimItemId;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineHcpcsCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimProcedureBase;
import gov.cms.bfd.server.ng.claim.model.institutional.ClaimLineInstitutionalSharedSystems;
import gov.cms.bfd.server.ng.claim.model.institutional.ClaimProcedureInstitutional;
import gov.cms.bfd.server.ng.claim.model.institutional.ClaimValue;
import gov.cms.bfd.server.ng.converter.OptionalBigDecimalConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
public class ClaimItemInstitutionalSharedSystems implements ClaimItemBase {
  @EmbeddedId private ClaimItemId claimItemId;
  @Embedded private ClaimLineInstitutionalSharedSystems claimLine;
  @Embedded private ClaimProcedureInstitutional claimProcedure;
  @Embedded private ClaimValue claimValue;

  @JoinColumn(name = "clm_uniq_id")
  @ManyToOne
  private ClaimInstitutionalCmsSharedSystems claim;

  @Convert(converter = OptionalBigDecimalConverter.class)
  @Column(name = "clm_line_msp_coinsrnc_amt")
  private Optional<BigDecimal> benePaymentAmount;

  @Override
  public Optional<ClaimProcedureBase> getProcedure() {
    return Optional.of(claimProcedure);
  }

  @Override
  public Optional<ClaimLineHcpcsCode> getClaimLineHcpcsCode() {
    return Optional.of(claimLine.getHcpcsCode());
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
