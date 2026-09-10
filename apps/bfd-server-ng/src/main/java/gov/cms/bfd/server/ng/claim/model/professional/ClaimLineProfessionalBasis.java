package gov.cms.bfd.server.ng.claim.model.professional;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@MappedSuperclass
@Getter
abstract class ClaimLineProfessionalBasis extends ClaimLineProfessionalBase {}
