package SamhsaUtils.model;

import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "fiss_tags", schema = "rda")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@IdClass(FissTagKey.class)
public class FissTag {
  @Id
  @Convert(converter = TagCodeConverter.class)
  @Column(name = "code", nullable = false)
  private TagCode code;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "details", nullable = true)
  private TagDetails[] details;

  @Id
  @ManyToOne
  @JoinColumn(name = "claim_id")
  @Column(name = "clm_id", nullable = false)
  RdaFissClaim claim;
}
