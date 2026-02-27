package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Embeddable;
import lombok.Getter;

/** Procedure and diagnosis info. */
@Embeddable
@Getter
public class ClaimProcedureProfessional extends ClaimProcedureBase {}
