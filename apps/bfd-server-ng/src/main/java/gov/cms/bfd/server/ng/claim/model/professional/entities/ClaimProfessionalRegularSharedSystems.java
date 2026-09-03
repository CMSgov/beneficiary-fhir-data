package gov.cms.bfd.server.ng.claim.model.professional.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import javax.annotation.processing.Generated;

/** The professional claim, regular profile, sourced from shared systems. */
@Entity
@Table(name = "claim_professional_ss", schema = "idr")
@Generated("TODO - Remove after query optimization implementation")
public class ClaimProfessionalRegularSharedSystems extends ClaimProfessionalRegularBase {}
