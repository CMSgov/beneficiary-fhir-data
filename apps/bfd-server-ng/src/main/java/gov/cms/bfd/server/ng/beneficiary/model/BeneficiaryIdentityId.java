package gov.cms.bfd.server.ng.beneficiary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Represents the composite primary key for the {@link BeneficiaryIdentity} entity. */
@EqualsAndHashCode
@NoArgsConstructor
@Getter
@AllArgsConstructor
@Embeddable
public class BeneficiaryIdentityId implements Serializable {

  @Column(name = "bene_sk")
  protected long beneSk;

  @Column(name = "bene_mbi_id")
  private String mbi;

  @Column(name = "bene_xref_efctv_sk_computed")
  private long xrefSk;

  @Column(name = "bene_mbi_efctv_dt")
  private Optional<LocalDate> mbiEffectiveDate;

  @Column(name = "bene_mbi_obslt_dt")
  private Optional<LocalDate> mbiObsoleteDate;
}
