package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.Optional;

@Entity
@Table(name = "claim_value", schema = "idr")
public class ClaimValue {
  @EmbeddedId private ClaimValueId claimValueId;

  @Column(name = "clm_val_cd")
  private String claimValueCode;

  @Column(name = "claim_val_amt")
  private double claimValueAmount;

  @ManyToOne
  @JoinColumn(name = "clm_uniq_id")
  private Claim claim;

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
