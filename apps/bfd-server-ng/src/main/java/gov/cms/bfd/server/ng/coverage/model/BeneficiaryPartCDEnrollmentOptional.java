package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.claim.model.Contract;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Optional;

@Embeddable
class BeneficiaryPartCDEnrollmentOptional {
  // Hack to ensure this entity is not null. See DESIGN.md for explanation/rationale.
  @Column(name = "bene_sk", insertable = false, updatable = false)
  private Long beneSk;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(
      name = "bene_cntrct_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "cntrct_num")
  @JoinColumn(
      name = "bene_pbp_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "cntrct_pbp_num")
  private Contract enrollmentContract;

  Optional<Contract> getEnrollmentContract() {
    return Optional.ofNullable(enrollmentContract);
  }
}
