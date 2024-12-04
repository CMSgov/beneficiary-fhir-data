package gov.cms.bfd.model.rif.samhsa;

import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** The composite key for CCW tags. */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CcwTagKey implements Serializable {
  /** The associated claim. */
  private Long claim;

  /** The tag code. */
  private String code;

  /** {@inheritDoc} */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CcwTagKey ccwTagKey)) return false;
    return this.claim.equals(ccwTagKey.getClaim()) && this.code.equals(ccwTagKey.getCode());
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return Objects.hash(claim, code);
  }
}
