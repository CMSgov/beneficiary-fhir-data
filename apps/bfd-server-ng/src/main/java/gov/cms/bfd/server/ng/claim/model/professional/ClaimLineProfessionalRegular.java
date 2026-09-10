package gov.cms.bfd.server.ng.claim.model.professional;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/** Claim line, professional, regular profile base. */
@MappedSuperclass
@Getter
abstract class ClaimLineProfessionalRegular extends ClaimLineProfessionalBase {}
