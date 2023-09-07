package gov.cms.test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.lang.Long;
import java.lang.String;
import java.time.Instant;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

/**
 * JPA class for the {@code MbiCache} table.
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
    name = "MbiCache",
    schema = "rda"
)
public class NoTransforms {
  @Id
  @Column(
      name = "mbi_id",
      nullable = false
  )
  @GeneratedValue(
      strategy = GenerationType.IDENTITY
  )
  private Long mbiId;

  @Column(
      name = "mbi",
      nullable = false,
      length = 11
  )
  @EqualsAndHashCode.Include
  private String mbi;

  @Column(
      name = "hash",
      nullable = false,
      length = 64
  )
  private String hash;

  @Column(
      name = "old_hash",
      nullable = true,
      length = 64
  )
  private String oldHash;

  @Column(
      name = "last_updated",
      nullable = false
  )
  private Instant lastUpdated;

  @Transient
  private String extra;

  public Long getMbiId() {
    return mbiId;
  }

  public void setMbiId(Long mbiId) {
    this.mbiId = mbiId;
  }

  public String getMbi() {
    return mbi;
  }

  public void setMbi(String mbi) {
    this.mbi = mbi;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public Optional<String> getOldHash() {
    return Optional.ofNullable(oldHash);
  }

  public void setOldHash(Optional<String> oldHash) {
    this.oldHash = oldHash.orElse(null);
  }

  public Instant getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(Instant lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  public Optional<String> getExtra() {
    return Optional.ofNullable(extra);
  }

  public void setExtra(Optional<String> extra) {
    this.extra = extra.orElse(null);
  }
}
