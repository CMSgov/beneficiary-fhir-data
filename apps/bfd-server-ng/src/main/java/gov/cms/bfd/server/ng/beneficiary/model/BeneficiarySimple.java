package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.converter.DefaultFalseBooleanConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.ZonedDateTime;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

/**
 * A simplified beneficiary model for use with claims data. Excess data is omitted to avoid the
 * performance penality of over-fetching data not needed specifically for claims.
 */
@Entity
@Getter
@Subselect(BeneficiaryBase.LATEST_VALID_BENE_VIEW)
@Immutable
public class BeneficiarySimple {
  @Id
  @Column(name = "bene_sk")
  private long beneSk;

  @Column(name = "bene_mbi_id")
  private String mbi;

  @Column(name = "bene_xref_efctv_sk")
  private long xrefSk;

  @Column(name = "idr_trans_efctv_ts")
  private ZonedDateTime effectiveTimestamp;

  @Convert(converter = DefaultFalseBooleanConverter.class)
  @Column(name = "idr_ltst_trans_flg")
  private Boolean latestTransactionFlag;
}
