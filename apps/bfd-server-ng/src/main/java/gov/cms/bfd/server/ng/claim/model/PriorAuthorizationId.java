package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Comparator;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode
@NoArgsConstructor
@Getter
@AllArgsConstructor
@Embeddable
class PriorAuthorizationId implements Serializable, Comparable<PriorAuthorizationId> {
  @Column(name = "mbi_num", insertable = false, updatable = false)
  private String mbi;

  @Column(name = "utn", insertable = false, updatable = false)
  private String uniqueTrackingNumber;

  @Override
  public int compareTo(@NotNull PriorAuthorizationId o) {
    return Comparator.comparing((PriorAuthorizationId id) -> id.mbi)
        .thenComparing(id -> id.uniqueTrackingNumber)
        .compare(this, o);
  }
}
