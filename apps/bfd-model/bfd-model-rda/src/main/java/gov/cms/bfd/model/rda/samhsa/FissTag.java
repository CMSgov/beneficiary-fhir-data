package gov.cms.bfd.model.rda.samhsa;

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

/** Entity for a FISS tag. */
@Entity
@Table(name = "fiss_tags", schema = "rda")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@IdClass(TagKey.class)
public class FissTag {
  /** Tag Code. */
  @Id
  @Column(name = "code", nullable = false)
  private String code;

  /** the details field. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "details", nullable = true)
  private List<TagDetails> details;

  /** The associated claim. */
  @Id
  @Column(name = "clm_id", nullable = false)
  String claim;
}
