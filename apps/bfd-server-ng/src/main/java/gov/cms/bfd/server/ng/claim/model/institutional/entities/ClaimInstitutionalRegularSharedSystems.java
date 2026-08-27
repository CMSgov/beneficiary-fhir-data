package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import gov.cms.bfd.server.ng.claim.model.institutional.AdjudicationChargeRegularSharedSystems;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import javax.annotation.processing.Generated;
import lombok.Getter;

/** The institutional claim, regular profile, sourced from shared systems. */
@Getter
@Entity
@Table(name = "claim_institutional_ss", schema = "idr")
@Generated("TODO - Remove after query optimization implementation")
public class ClaimInstitutionalRegularSharedSystems extends ClaimInstitutionalRegularBase {

  @Embedded private AdjudicationChargeRegularSharedSystems adjudicationCharge;
}
