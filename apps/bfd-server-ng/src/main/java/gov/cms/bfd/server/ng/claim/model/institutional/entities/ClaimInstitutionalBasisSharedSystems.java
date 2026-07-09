package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

/** the institutional claim, basis profile, from shared systems. */
@Getter
@Entity
@Table(name = "claim_institutional_ss", schema = "idr")
public class ClaimInstitutionalBasisSharedSystems extends ClaimInstitutionalBasisBase {}
