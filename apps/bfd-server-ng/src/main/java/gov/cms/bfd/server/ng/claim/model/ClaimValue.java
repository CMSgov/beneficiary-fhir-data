package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.converter.NonZeroIntConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
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
  private BigDecimal claimValueAmount;

  Optional<BigDecimal> getClaimValueAmount(String code) {
    return getAmountForCode(code);
  }

  // todo integer version for blood type etc
  private Optional<BigDecimal> getAmountForCode(String code) {
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
