package gov.cms.bfd.model.rda;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
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
public class Mbi {
  @Id
  @Column(name = "`mbiId`", nullable = false, updatable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long mbiId;

  /** Actual MBI value from RDA API. */
  @Column(name = "`mbi`", length = 11, nullable = false, unique = true)
  @EqualsAndHashCode.Include
  private String mbi;

  /** Currently active hash value used to represent the MBI in client applications. */
  @Column(name = "`hash`", length = 64, nullable = false, unique = true)
  private String hash;

  /**
   * Old hash value that can be used to maintain backwards compatibility when rotating between a
   * current hash to a new one. For example if the algorithm, number of iterations, or salt have
   * been changed. This column is nullable.
   */
  @Column(name = "`oldHash`", length = 64)
  private String oldHash;

  @Column(name = "`lastUpdated`", nullable = false)
  private Instant lastUpdated;

  /**
   * Convenience constructor to create a record with a non-null ID and no oldHash value.
   *
   * @param mbiId primary key value
   * @param mbi mbi value
   * @param hash hash value
   */
  public Mbi(long mbiId, String mbi, String hash) {
    this(mbiId, mbi, hash, null, Instant.now());
  }

  /**
   * Convenience constructor to create a record with null ID and no oldHash value.
   *
   * @param mbi mbi value
   * @param hash hash value
   */
  public Mbi(String mbi, String hash) {
    this(null, mbi, hash, null, Instant.now());
  }
}
