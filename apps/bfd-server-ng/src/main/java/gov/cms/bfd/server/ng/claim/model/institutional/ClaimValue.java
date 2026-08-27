package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.converter.NonZeroIntConverter;
import gov.cms.bfd.server.ng.converter.OptionalBigDecimalToAmountConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.Getter;

/**
 * Represents various amounts associated with a claim, either as an Adjudication or SupportingInfo
 * on the EOB.
 *
 * <ul>
 *   <li>{@code sequenceNumber}: The sequence number for the claim value.
 *   <li>{@code claimValueCode}: The code representing kind of value amount.
 *   <li>{@code claimValueAmount}: The amount relating to the claim.
 * </ul>
 */
@Getter
@Embeddable
public class ClaimValue {
  @Convert(converter = NonZeroIntConverter.class)
  @Column(name = "clm_val_sqnc_num_val")
  private Optional<Integer> sequenceNumber;

  @Column(name = "clm_val_cd")
  private Optional<String> claimValueCode;

  @Convert(converter = OptionalBigDecimalToAmountConverter.class)
  @Column(name = "clm_val_amt")
  private Optional<BigDecimal> claimValueAmount;

  Optional<BigDecimal> getClaimValueAmount(String code) {
    return getAmountForCode(code);
  }

  Optional<Integer> getClaimValueQuantity(String code) {
    return getQuantityForCode(code);
  }

  private Optional<BigDecimal> getAmountForCode(String code) {
    return claimValueCode.filter(c -> c.equals(code)).flatMap(c -> claimValueAmount);
  }

  private Optional<Integer> getQuantityForCode(String code) {
    return claimValueCode
        .filter(c -> c.equals(code))
        .flatMap(c -> claimValueAmount)
        .map(BigDecimal::intValue);
  }
}
