package gov.cms.test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.lang.Long;
import java.lang.String;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

/**
 * JPA class for the {@code FissProcCodes} table.
 */
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(
    onlyExplicitlyIncluded = true
)
@FieldNameConstants
@Table(
    name = "`FissProcCodes`",
    schema = "`pre_adj`"
)
@IdClass(FissProcCode.PK.class)
public class FissProcCode implements Serializable {
  @Id
  @Column(
      name = "`dcn`",
      nullable = false,
      length = 23
  )
  @EqualsAndHashCode.Include
  private String dcn;

  @Id
  @Column(
      name = "`priority`",
      nullable = false
  )
  @EqualsAndHashCode.Include
  private short priority;

  @Column(
      name = "`procCode`",
      nullable = false,
      length = 10
  )
  private String procCode;

  @Column(
      name = "`procFlag`",
      nullable = true,
      length = 4
  )
  private String procFlag;

  @Column(
      name = "`procDate`",
      nullable = true
  )
  private LocalDate procDate;

  /**
   * Illustrates string column auto-converted into long in entity class.
   */
  @Column(
      name = "`longString`",
      nullable = true,
      length = 15
  )
  private String longString;

  @Column(
      name = "`lastUpdated`",
      nullable = true
  )
  private Instant lastUpdated;

  public String getDcn() {
    return dcn;
  }

  public void setDcn(String dcn) {
    this.dcn = dcn;
  }

  public short getPriority() {
    return priority;
  }

  public void setPriority(short priority) {
    this.priority = priority;
  }

  public String getProcCode() {
    return procCode;
  }

  public void setProcCode(String procCode) {
    this.procCode = procCode;
  }

  public String getProcFlag() {
    return procFlag;
  }

  public void setProcFlag(String procFlag) {
    this.procFlag = procFlag;
  }

  public LocalDate getProcDate() {
    return procDate;
  }

  public void setProcDate(LocalDate procDate) {
    this.procDate = procDate;
  }

  public long getLongString() {
    return Long.parseLong(longString);
  }

  public void setLongString(long longString) {
    this.longString = String.valueOf(longString);
  }

  public Instant getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(Instant lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  /**
   * PK class for the FissProcCodes table
   */
  @Getter
  @EqualsAndHashCode
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PK implements Serializable {
    private static final long serialVersionUID = 1;

    private String dcn;

    private short priority;
  }
}
