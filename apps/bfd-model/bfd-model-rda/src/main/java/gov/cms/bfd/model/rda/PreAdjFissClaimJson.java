package gov.cms.bfd.model.rda;

import java.time.Instant;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * JPA entity class for the table that holds FISS claim data in a JSONB column. Other columns
 * besides the JSON column are defined so that they can be indexed for query optimization.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
@Table(name = "`FissClaimsJson`", schema = "`pre_adj`")
public class PreAdjFissClaimJson {
  public PreAdjFissClaimJson(PreAdjFissClaim claim) {
    this(
        claim.getDcn(),
        claim.getMbi(),
        claim.getMbiHash(),
        claim.getStmtCovToDate(),
        claim.getLastUpdated(),
        claim.getSequenceNumber(),
        claim);
  }

  @Id
  @Column(name = "`dcn`", length = 23, nullable = false)
  @EqualsAndHashCode.Include
  private String dcn;

  @Column(name = "`mbi`", length = 13)
  private String mbi;

  @Column(name = "`mbiHash`", length = 64)
  private String mbiHash;

  @Column(name = "`stmtCovToDate`")
  private LocalDate stmtCovToDate;

  @Column(name = "`lastUpdated`", nullable = false)
  private Instant lastUpdated;

  @Column(name = "`sequenceNumber`", nullable = false)
  private Long sequenceNumber;

  @Column(name = "`claim`", nullable = false, columnDefinition = "jsonb")
  @Convert(converter = PreAdjFissClaimConverter.class)
  private PreAdjFissClaim claim;
}
