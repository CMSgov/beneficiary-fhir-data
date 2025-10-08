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

/** Represents the composite primary key for the {@link BeneficiaryThirdParty} entity. */
@EqualsAndHashCode
@NoArgsConstructor
@Getter
@AllArgsConstructor
@Embeddable
public class BeneficiaryThirdPartyId implements Serializable, Comparable<BeneficiaryThirdPartyId> {

  @Column(name = "bene_sk")
  private Long beneSk;

  @Column(name = "bene_tp_type_cd")
  private String thirdPartyTypeCode;

  @Override
  public int compareTo(@NotNull BeneficiaryThirdPartyId o) {
    return Comparator.comparing((BeneficiaryThirdPartyId id) -> id.beneSk)
        .thenComparing(id -> id.thirdPartyTypeCode)
        .compare(this, o);
  }
}
