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
class PriorAuthorizationItemId implements Serializable, Comparable<PriorAuthorizationItemId> {
  @Column(name = "mbi_num", insertable = false, updatable = false)
  private String mbi;

  @Column(name = "utn", insertable = false, updatable = false)
  private String uniqueTrackingNumber;

  @Column(name = "current_segment", insertable = false, updatable = false)
  private int currentSegment;

  @Override
  public int compareTo(@NotNull PriorAuthorizationItemId o) {
    return Comparator.comparing((PriorAuthorizationItemId id) -> id.mbi)
        .thenComparing(id -> id.uniqueTrackingNumber)
        .thenComparing(id -> id.currentSegment)
        .compare(this, o);
  }
}
