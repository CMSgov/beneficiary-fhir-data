package gov.cms.bfd.model.rda.samhsa;

import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** The composite key for the FISS tags. */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class TagKey implements Serializable {
  /** The associated claim. */
  private String claim;

  /** The tag code. */
  private String code;

  /** {@inheritDoc} */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TagKey tagKey)) return false;
    return this.claim.equals(tagKey.getClaim()) && this.code.equals(tagKey.getCode());
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return Objects.hash(claim, code);
  }
}
