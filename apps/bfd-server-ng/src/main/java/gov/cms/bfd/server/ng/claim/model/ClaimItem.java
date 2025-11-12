package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/** Claim item table. */
@Getter
@Entity
@EqualsAndHashCode
@Table(name = "claim_item", schema = "idr")
public class ClaimItem implements Comparable<ClaimItem> {
  @EmbeddedId private ClaimItemId claimItemId;
  @Embedded private ClaimLine claimLine;
  @Embedded private ClaimProcedure claimProcedure;
  @Embedded private ClaimValue claimValue;

  @Column(name = "bfd_updated_ts")
  private ZonedDateTime bfdUpdatedTimestamp;

  @JoinColumn(name = "clm_uniq_id")
  @ManyToOne
  private Claim claim;

  @JoinColumn(
      name = "clm_uniq_id",
      insertable = false,
      updatable = false,
      referencedColumnName = "clm_uniq_id")
  @JoinColumn(
      name = "clm_line_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "clm_line_num")
  @OneToOne
  private ClaimLineInstitutional claimLineInstitutional;

  @JoinColumn(
      name = "clm_uniq_id",
      insertable = false,
      updatable = false,
      referencedColumnName = "clm_uniq_id")
  @JoinColumn(
      name = "clm_line_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "clm_line_num")
  @OneToOne
  private ClaimLineProfessional claimLineProfessional;

  Optional<ClaimLineInstitutional> getClaimLineInstitutional() {
    return Optional.ofNullable(claimLineInstitutional);
  }

  Optional<ClaimLineProfessional> getClaimLineProfessional() {
    return Optional.ofNullable(claimLineProfessional);
  }

  /**
   * Returns a stream of relevant timestamps for this ClaimItem, including the item's own updated
   * timestamp and any timestamps from the associated ClaimLineInstitutional.
   *
   * @return stream of ZonedDateTimes
   */
  Stream<ZonedDateTime> streamTimestamps() {
    var itemTs = Stream.of(bfdUpdatedTimestamp);
    var lineInstitutionalStream =
        getClaimLineInstitutional()
            .map(
                cli ->
                    Stream.concat(
                        Stream.of(cli.getBfdUpdatedTimestamp()),
                        cli
                            .getAnsiSignature()
                            .map(ClaimAnsiSignature::getBfdUpdatedTimestamp)
                            .stream()))
            .stream()
            .flatMap(s -> s);
    return Stream.concat(itemTs, lineInstitutionalStream);
  }

  @Override
  public int compareTo(@NotNull ClaimItem o) {
    return claimItemId.compareTo(o.claimItemId);
  }
}
