package gov.cms.bfd.model.rif.samhsa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Entity for an MCS tag. */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@IdClass(CcwTagKey.class)
@Table(name = "outpatient_tags", schema = "ccw")
public class OutpatientTag {
  /** The tag code. */
  @Id
  @Column(name = "code", nullable = false)
  private String code;

  /** The tag details. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "details", nullable = true)
  private List details;

  /** The associated claim. */
  @Id
  @Column(name = "clm_id", nullable = false)
  Long claim;
}
