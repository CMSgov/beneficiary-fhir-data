package gov.cms.bfd.model.rda;

import java.io.Serializable;
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

/** JPA class for the McsLocations table */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@IdClass(PreAdjMcsLocation.PK.class)
@Table(name = "mcs_locations", schema = "part_adj")
public class PreAdjMcsLocation {
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

  @Column(name = "idr_loc_clerk", length = 4)
  private String idrLocClerk;

  @Column(name = "idr_loc_code", length = 3)
  private String idrLocCode;

  @Column(name = "idr_loc_date")
  private LocalDate idrLocDate;

  @Column(name = "idr_loc_actv_code", length = 1)
  private String idrLocActvCode;

  /** PK class for the McsLocations table */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PK implements Serializable {
    private String idrClmHdIcn;

    private short priority;
  }
}
