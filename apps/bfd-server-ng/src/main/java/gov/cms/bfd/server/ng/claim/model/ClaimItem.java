package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;

/** Claim item table. */
@Getter
@Entity
@Table(name = "claim_item", schema = "idr")
public class ClaimItem {
  @EmbeddedId private ClaimItemId claimItemId;
  @Embedded private ClaimLine claimLine;
  @Embedded private ClaimProcedure claimProcedure;
  @Embedded private ClaimValue claimValue;

  @Column(name = "bfd_updated_ts")
  private ZonedDateTime bfdUpdatedTimestamp;

  @JoinColumn(name = "clm_uniq_id")
  @ManyToOne
  private Claim claim;

  @JoinColumns({
    @JoinColumn(
        name = "clm_uniq_id",
        insertable = false,
        updatable = false,
        referencedColumnName = "clm_uniq_id"),
    @JoinColumn(
        name = "clm_line_num",
        insertable = false,
        updatable = false,
        referencedColumnName = "clm_line_num")
  })
  @OneToOne
  private ClaimLineInstitutional claimLineInstitutional;

  Optional<ClaimLineInstitutional> getClaimLineInstitutional() {
    return Optional.ofNullable(claimLineInstitutional);
  }

  /**
   * Returns a stream of relevant timestamps for this ClaimItem, including the item's own updated
   * timestamp and any timestamps from the associated ClaimLineInstitutional.
   *
   * @return stream of ZonedDateTimes
   */
  Stream<ZonedDateTime> streamTimestamps() {
    var itemTs = Optional.ofNullable(bfdUpdatedTimestamp).stream();

    var lineInstitutionalStream =
        getClaimLineInstitutional()
            .map(
                cli -> {
                  var cliTs = Optional.ofNullable(cli.getBfdUpdatedTimestamp()).stream();
                  var ansiTs =
                      cli.getAnsiSignature()
                          .map(ansi -> Optional.ofNullable(ansi.getBfdUpdatedTimestamp()).stream())
                          .orElseGet(Stream::empty);
                  return Stream.concat(cliTs, ansiTs);
                })
            .orElseGet(Stream::empty);

    return Stream.concat(itemTs, lineInstitutionalStream);
  }
}
