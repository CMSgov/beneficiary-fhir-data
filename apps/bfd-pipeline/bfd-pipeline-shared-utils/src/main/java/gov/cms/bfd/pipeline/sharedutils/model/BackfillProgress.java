package gov.cms.bfd.pipeline.sharedutils.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/** Entity for backfill progress. */
@Entity
@Getter
@Setter
@AllArgsConstructor
@Table(name = "samhsa_backfill_progress", schema = "ccw")
public class BackfillProgress {
  /** The table that this progress belongs to. */
  @Column(name = "claim_table")
  String claimTable;

  /** The last processed claim of this table. */
  @Column(name = "last_processed_claim")
  String lastClaimId;

  /** The total number of claims processed so far. */
  @Column(name = "total_processed")
  Long totalProcessed;

  /** The total number of tags saved so far. */
  @Column(name = "total_tags")
  Long totalTags;
}
