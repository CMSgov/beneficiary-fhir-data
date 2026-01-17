package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.coverage.model.ContractPlanContactInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/***
 * Optional child entities for {@link Contract}.
 */
@Embeddable
public class ContractOptional {
  // Hack to ensure this entity is not null. See DESIGN.md for explanation/rationale.
  @Column(name = "cntrct_pbp_sk", insertable = false, updatable = false)
  private long contractPbpSk;

  @Nullable
  @OneToOne
  @JoinColumn(name = "cntrct_pbp_sk")
  private ContractPlanContactInfo contractPlanContactInfo;

  /**
   * Gets the {@link ContractPlanContactInfo}.
   *
   * @return contact info
   */
  public Optional<ContractPlanContactInfo> getContractPlanContactInfo() {
    return Optional.ofNullable(contractPlanContactInfo);
  }
}
