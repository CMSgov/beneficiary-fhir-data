package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

@Embeddable
class ClaimLineInstitutionalOptional {
  // Hack to ensure this entity is not null. See DESIGN.md for explanation/rationale.
  @Column(name = "clm_uniq_id", insertable = false, updatable = false)
  private long claimUniqueId;

  @Nullable
  @OneToOne
  @JoinColumn(name = "clm_ansi_sgntr_sk")
  private ClaimAnsiSignature ansiSignature;

  public Optional<ClaimAnsiSignature> getAnsiSignature() {
    return Optional.ofNullable(ansiSignature);
  }
}
