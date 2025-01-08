package gov.cms.bfd.pipeline.sharedutils.samhsa.backfill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** POJO for Backfill Config. */
@Getter
@Setter
@AllArgsConstructor
@Builder
public class BackfillConfigOptions {
  /** true if backfill job is enabled. */
  boolean enabled;

  /**
   * The number of claims to process at a time. This directly relates to the limit parameter of the
   * query.
   */
  int batchSize;

  /** The log interval. */
  Long logInterval;
}
