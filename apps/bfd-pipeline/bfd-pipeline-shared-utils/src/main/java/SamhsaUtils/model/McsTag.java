package SamhsaUtils.model;

import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "mcs_tags", schema = "rda")
public class McsTag {
  @Column(name = "tag_id")
  String tagId;

  @Convert(converter = TagCodeConverter.class)
  @Column(name = "code", nullable = false)
  private TagCode code;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "details", nullable = true)
  private TagDetails[] details;

  @Column(name = "clm_id", nullable = false)
  RdaMcsClaim claim;
}
