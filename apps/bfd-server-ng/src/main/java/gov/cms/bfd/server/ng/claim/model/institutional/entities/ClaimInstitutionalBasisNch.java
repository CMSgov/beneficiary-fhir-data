package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

/** the institutional claim, basis profile, from nch. */
@Getter
@Entity
@Table(name = "claim_institutional_nch", schema = "idr")
public class ClaimInstitutionalBasisNch extends ClaimInstitutionalBasisBase {}
