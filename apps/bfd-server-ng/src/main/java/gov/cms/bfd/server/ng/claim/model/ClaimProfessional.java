package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Extension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Professional claims table. */
@Getter
@Entity
@Table(name = "claim_professional", schema = "idr")
public class ClaimProfessional {
    @Id
    @Column(name = "clm_uniq_id")
    private long claimUniqueId;

    @OneToOne(mappedBy = "claimProfessional")
    private Claim claim;

    @Column(name = "clm_carr_pmt_dnl_cd")
    private Optional<ClaimPaymentDenialCode> claimPaymentDenialCode;

    @Column(name = "clm_mdcr_prfnl_prvdr_asgnmt_sw")
    private Optional<ProviderAssignmentIndicatorSwitch> providerAssignmentIndicatorSwitch;

    @Column(name = "clm_mdcr_prfnl_prmry_pyr_amt")
    private double primaryProviderPaidAmount;

    @Embedded ClinicalTrialNumber clinicalTrialNumber;

    List<Extension> toFhirExtension() {
        return Stream.of(
                    claimPaymentDenialCode.map(ClaimPaymentDenialCode::toFhir),
                    providerAssignmentIndicatorSwitch.map(ProviderAssignmentIndicatorSwitch::toFhir),
                    clinicalTrialNumber.toFhir()
                )
                .flatMap(Optional::stream)
                .toList();
    }

    ExplanationOfBenefit.TotalComponent toFhirTotal() {
        return AdjudicationChargeType.PAYER_PAID_AMOUNT.toFhirTotal(primaryProviderPaidAmount);
    }
}
