package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

public class ClaimItemOptional {
  @Column(name = "clm_uniq_id", insertable = false, updatable = false)
  private long claimUniqueId;

  @Nullable
  @JoinColumn(
      name = "clm_uniq_id",
      insertable = false,
      updatable = false,
      referencedColumnName = "clm_uniq_id")
  @JoinColumn(
      name = "clm_line_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "clm_line_num")
  @OneToOne
  private ClaimLineInstitutional claimLineInstitutional;

  @Nullable
  @JoinColumn(
      name = "clm_uniq_id",
      insertable = false,
      updatable = false,
      referencedColumnName = "clm_uniq_id")
  @JoinColumn(
      name = "clm_line_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "clm_line_num")
  @OneToOne
  private ClaimLineProfessional claimLineProfessional;

  @Nullable
  @JoinColumn(
      name = "clm_uniq_id",
      insertable = false,
      updatable = false,
      referencedColumnName = "clm_uniq_id")
  @JoinColumn(
      name = "clm_line_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "clm_line_num")
  @OneToOne
  private ClaimLineRx claimLineRx;

  Optional<ClaimLineInstitutional> getClaimLineInstitutional() {
    return Optional.ofNullable(claimLineInstitutional);
  }

  Optional<ClaimLineProfessional> getClaimLineProfessional() {
    return Optional.ofNullable(claimLineProfessional);
  }

  Optional<ClaimLineRx> getClaimLineRx() {
    return Optional.ofNullable(claimLineRx);
  }
}
