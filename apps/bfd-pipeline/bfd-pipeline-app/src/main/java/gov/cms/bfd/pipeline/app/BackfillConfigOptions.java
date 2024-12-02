package gov.cms.bfd.pipeline.app;

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
}
