package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.util.Optional;

@Entity
public class ClaimValue {
  @Column(name = "clm_uniq_id")
  private long claimUniqueId;

  @Column(name = "clm_val_cd")
  private String claimValueCode;

  @Column(name = "claim_val_amt")
  private double claimValueAmount;

  private static final String VALUE_CODE_DISPROPORTIONATE = "18";
  private static final String VALUE_CODE_IME = "19";

  Optional<Double> getDisproportionateAmount() {
    return getAmountForCode(VALUE_CODE_DISPROPORTIONATE);
  }

  Optional<Double> getImeAmount() {
    return getAmountForCode(VALUE_CODE_IME);
  }

  private Optional<Double> getAmountForCode(String code) {
    if (claimValueCode.equals(code)) {
      return Optional.of(claimValueAmount);
    } else {
      return Optional.empty();
    }
  }
}
