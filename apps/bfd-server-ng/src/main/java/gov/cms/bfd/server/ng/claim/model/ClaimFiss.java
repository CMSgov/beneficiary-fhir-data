package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

import java.util.Optional;

/** Fiscal Intermediary Standard System claims table. */
@Getter
@Entity
@Table(name = "claim_fiss", schema = "idr")
public class ClaimFiss {
    @Id
    @Column(name = "clm_uniq_id")
    private long claimUniqueId;

    @OneToOne(mappedBy = "claimFiss")
    private Claim claim;

    @Column(name = "clm_crnt_stus_cd")
    private ClaimCurrentStatusCode claimCurrentStatusCode;

    Optional <ExplanationOfBenefit.RemittanceOutcome> toFhirOutcome(int claimTypecode) {
        if (claimTypecode >= 2000 && claimTypecode < 3000) {
            return Optional.ofNullable(claimCurrentStatusCode.getOutcome());
        }
        return Optional.empty();
    }
}