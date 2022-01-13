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
@IdClass(PartAdjMcsAdjustment.PK.class)
@Table(name = "`McsAdjustments`", schema = "`part_adj`")
public class PartAdjMcsAdjustment {
  @Id
  @Column(name = "`idrClmHdIcn`", length = 15, nullable = false)
  @EqualsAndHashCode.Include
  private String idrClmHdIcn;

  @Id
  @Column(name = "`priority`", nullable = false)
  @EqualsAndHashCode.Include
  private short priority;

  @Column(name = "`lastUpdated`")
  private Instant lastUpdated;

  @Column(name = "`idrAdjDate`")
  private LocalDate idrAdjDate;

  @Column(name = "`idrXrefIcn`", length = 15)
  private String idrXrefIcn;

  @Column(name = "`idrAdjClerk`", length = 4)
  private String idrAdjClerk;

  @Column(name = "`idrInitCcn`", length = 15)
  private String idrInitCcn;

  @Column(name = "`idrAdjChkWrtDt`")
  private LocalDate idrAdjChkWrtDt;

  @Column(name = "`idrAdjBEombAmt`", columnDefinition = "decimal(7,2)")
  private BigDecimal idrAdjBEombAmt;

  @Column(name = "`idrAdjPEombAmt`", columnDefinition = "decimal(7,2)")
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
