package gov.cms.bfd.pipeline.sharedutils.npi_fda;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Config for NpiFdaLoadJob. */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NpiFdaLoadJobConfig {
  /** True if the job is enabled. */
  boolean enabled = false;

  /** The number of records to save before committing a transaction. */
  int batchSize = 100000;

  /** How often to run the job, in days. */
  int runInterval;
}
