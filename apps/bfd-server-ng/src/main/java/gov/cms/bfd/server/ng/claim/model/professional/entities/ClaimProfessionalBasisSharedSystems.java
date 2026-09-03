package gov.cms.bfd.server.ng.claim.model.professional.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import javax.annotation.processing.Generated;

/** The professional claim, basis profile, sourced from shared system. */
@Entity
@Table(name = "claim_professional_ss", schema = "idr")
@Generated("TODO - Remove after query optimization implementation")
public class ClaimProfessionalBasisSharedSystems extends ClaimProfessionalBasisBase {}
