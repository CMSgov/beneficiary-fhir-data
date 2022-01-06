package gov.cms.bfd.model.rda;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/** JPA class for the MbiCache table */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Table(name = "`MbiCache`", schema = "`pre_adj`")
public class PreAdjMbi {
  @Id
  @Column(name = "`mbi`", length = 13, nullable = false)
  @EqualsAndHashCode.Include
  private String mbi;

  @Column(name = "`mbiHash`", length = 64, nullable = false)
  private String mbiHash;
}
