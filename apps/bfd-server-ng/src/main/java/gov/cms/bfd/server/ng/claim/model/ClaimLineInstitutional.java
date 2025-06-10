package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import lombok.Getter;

@Getter
@Entity
public class ClaimLineInstitutional {
  @Embedded private ClaimLineHippsCode hippsCode;
}
