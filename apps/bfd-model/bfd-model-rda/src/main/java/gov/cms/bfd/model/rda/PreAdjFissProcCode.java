package gov.cms.bfd.model.rda;

import java.sql.Date;
import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/** JPA class for the PreAdjFissProcCodes table */
@Entity
@Getter
@Setter
@FieldNameConstants
@IdClass(PreAdjFissProcCodePK.class)
@Table(name = "`FissProcCodes`", schema = "pre_adj")
public class PreAdjFissProcCode {

  @Id
  @Column(name = "`dcn`", length = 23, nullable = false)
  private String dcn;

  @Id
  @Column(name = "`priority`", nullable = false)
  private short priority;

  @Column(name = "`procCode`", length = 10, nullable = false)
  private String procCode;

  @Column(name = "`procFlag`", length = 4, nullable = false)
  private String procFlag;

  @Column(name = "`procDate`")
  private Date procDate;

  @Column(name = "`lastUpdated`")
  private Timestamp lastUpdated;
}
