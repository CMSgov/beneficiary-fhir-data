package gov.cms.bfd.model.rda.samhsa;

import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** The composite key for RDA tags. */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class RdaTagKey implements Serializable {
  /** The associated claim. */
  private String claim;

  /** The tag code. */
  private String code;

  /** {@inheritDoc} */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof RdaTagKey rdaTagKey)) return false;
    return this.claim.equals(rdaTagKey.getClaim()) && this.code.equals(rdaTagKey.getCode());
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return Objects.hash(claim, code);
  }
}
