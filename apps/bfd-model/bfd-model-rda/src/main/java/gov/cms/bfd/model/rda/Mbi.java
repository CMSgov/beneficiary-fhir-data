package gov.cms.bfd.model.rda;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
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
public class Mbi {
  @Id
  @Column(name = "`mbiId`", nullable = false, updatable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mbi_cache_mbi_id_seq")
  @SequenceGenerator(
      name = "mbi_cache_mbi_id_seq",
      sequenceName = "mbi_cache_mbi_id_seq",
      allocationSize = 50)
  private Long mbiId;

  @Column(name = "`mbi`", length = 13, nullable = false, unique = true)
  @EqualsAndHashCode.Include
  private String mbi;

  @Column(name = "`mbiHash`", length = 64, nullable = false, unique = true)
  private String mbiHash;
}
