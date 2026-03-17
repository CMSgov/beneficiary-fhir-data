package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.converter.DefaultFalseBooleanConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * A simplified beneficiary model for use with claims data. Excess data is omitted to avoid the
 * performance penality of over-fetching data not needed specifically for claims.
 */
@Entity
@Getter
@Table(name = "valid_beneficiary", schema = "idr")
public class BeneficiarySimple {
  @Id
  @Column(name = "bene_sk")
  private long beneSk;

  @Column(name = "bene_xref_efctv_sk")
  private long xrefSk;

  @Convert(converter = DefaultFalseBooleanConverter.class)
  @Column(name = "idr_ltst_trans_flg")
  private Boolean latestTransactionFlag;
}
