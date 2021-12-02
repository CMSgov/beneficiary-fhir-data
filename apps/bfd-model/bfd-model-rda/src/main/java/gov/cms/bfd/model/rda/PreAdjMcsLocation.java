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

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Table(name = "`McsLocations`", schema = "`pre_adj`")
@IdClass(PreAdjMcsLocation.PK.class)
public class PreAdjMcsLocation {
  @Id
  @EqualsAndHashCode.Include
  @Column(name = "`idrClmHdIcn`", nullable = false, length = 15)
  private String idrClmHdIcn;

  @Id
  @EqualsAndHashCode.Include
  @Column(name = "`priority`", nullable = false)
  private short priority;

  @Column(name = "`lastUpdated`")
  private Instant lastUpdated;

  @Column(name = "`idrLocClerk`", length = 4)
  private String idrLocClerk;

  @Column(name = "`idrLocCode`", length = 3)
  private String idrLocCode;

  @Column(name = "`idrLocDate`")
  private LocalDate idrLocDate;

  @Column(name = "`idrLocActvCode`", length = 1)
  private String idrLocActvCode;

  /** PK class for the McsLocations table */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static final class PK implements Serializable {
    private String idrClmHdIcn;

    private short priority;
  }
}
