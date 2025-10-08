package gov.cms.bfd.server.ng.coverage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Comparator;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/** Represents the composite primary key for the {@link BeneficiaryEntitlement} entity. */
@EqualsAndHashCode
@NoArgsConstructor
@Getter
@AllArgsConstructor
@Embeddable
public class BeneficiaryEntitlementId
    implements Serializable, Comparable<BeneficiaryEntitlementId> {

  @Column(name = "bene_sk")
  private Long beneSk;

  @Column(name = "bene_mdcr_entlmt_type_cd")
  private String medicareEntitlementTypeCode;

  @Override
  public int compareTo(@NotNull BeneficiaryEntitlementId o) {
    return Comparator.comparing((BeneficiaryEntitlementId id) -> id.beneSk)
        .thenComparing(id -> id.medicareEntitlementTypeCode)
        .compare(this, o);
  }
}
