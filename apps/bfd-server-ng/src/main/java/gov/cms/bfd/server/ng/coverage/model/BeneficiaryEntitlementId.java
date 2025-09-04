package gov.cms.bfd.server.ng.coverage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Represents the composite primary key for the {@link BeneficiaryEntitlement} entity. */
@EqualsAndHashCode
@NoArgsConstructor
@Getter
@AllArgsConstructor
@Embeddable
public class BeneficiaryEntitlementId implements Serializable {

  @Column(name = "bene_sk")
  private Long beneSk;

  @Column(name = "bene_mdcr_entlmt_type_cd")
  private String medicareEntitlementTypeCode;
}
