package gov.cms.bfd.model.rda;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/** JPA class for the McsAdjustments table */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@IdClass(RdaMcsAdjustment.PK.class)
@Table(name = "mcs_adjustments", schema = "rda")
public class RdaMcsAdjustment {
  @Id
  @Column(name = "idr_clm_hd_icn", length = 15, nullable = false)
  @EqualsAndHashCode.Include
  private String idrClmHdIcn;

  @Id
  @Column(name = "priority", nullable = false)
  @EqualsAndHashCode.Include
  private short priority;

  @Column(name = "last_updated")
  private Instant lastUpdated;

  @Column(name = "idr_adj_date")
  private LocalDate idrAdjDate;

  @Column(name = "idr_xref_icn", length = 15)
  private String idrXrefIcn;

  @Column(name = "idr_adj_clerk", length = 4)
  private String idrAdjClerk;

  @Column(name = "idr_init_ccn", length = 15)
  private String idrInitCcn;

  @Column(name = "idr_adj_chk_wrt_dt")
  private LocalDate idrAdjChkWrtDt;

  @Column(name = "idr_adj_b_eomb_amt", columnDefinition = "decimal(7,2)")
  private BigDecimal idrAdjBEombAmt;

  @Column(name = "idr_adj_p_eomb_amt", columnDefinition = "decimal(7,2)")
  private BigDecimal idrAdjPEombAmt;

  /** PK class for the McsAdjustments table */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PK implements Serializable {
    private String idrClmHdIcn;

    private short priority;
  }
}
