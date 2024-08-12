package gov.cms.bfd.model.rif.views;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

/** Entity that represents the current_beneficiaries materialized view. */
@Entity
@Immutable
@Getter
@Subselect("SELECT bene_id, xref_grp_id FROM ccw.current_beneficiaries")
public class CurrentBeneficiary {
  /** The beneficiary ID. */
  @Column(name = "bene_id", nullable = false)
  @Id
  private Long beneficiaryId;

  /** The xref group ID. */
  @Column(name = "xref_grp_id", nullable = false)
  private Long xrefGroupId;
}
