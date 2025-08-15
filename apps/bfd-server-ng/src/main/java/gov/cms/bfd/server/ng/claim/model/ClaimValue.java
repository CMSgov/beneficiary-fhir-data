package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.converter.NonZeroIntConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import lombok.Getter;

@Getter
@Embeddable
class ClaimValue {
  @Convert(converter = NonZeroIntConverter.class)
  @Column(name = "clm_val_sqnc_num_val")
  private Optional<Integer> sequenceNumber;

  @Column(name = "clm_val_cd")
  private Optional<String> claimValueCode;

  @Column(name = "clm_val_amt")
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
    return claimValueCode.flatMap(
        c -> {
          if (c.equals(code)) {
            return Optional.of(claimValueAmount);
          } else {
            return Optional.empty();
          }
        });
  }
}
