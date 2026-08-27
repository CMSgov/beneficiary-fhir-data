package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import gov.cms.bfd.server.ng.claim.model.institutional.AdjudicationChargeRegular;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import javax.annotation.processing.Generated;
import lombok.Getter;

/** The institutional claim, regular profile, sourced from nch. */
@Getter
@Entity
@Table(name = "claim_institutional_nch", schema = "idr")
@Generated("TODO - Remove after query optimization implementation")
public class ClaimInstitutionalRegularNch extends ClaimInstitutionalRegularBase {

  @Embedded private AdjudicationChargeRegular adjudicationCharge;
}
