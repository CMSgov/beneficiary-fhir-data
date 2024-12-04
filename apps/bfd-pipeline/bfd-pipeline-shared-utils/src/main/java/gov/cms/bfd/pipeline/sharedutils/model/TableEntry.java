package gov.cms.bfd.pipeline.sharedutils.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/** POJO to contain information about the tables to use in the backfill SQL queries. */
@Getter
@Setter
@AllArgsConstructor
public class TableEntry {
  /** The claim table. */
  String claimTable;

  /** The class of the claim. */
  Class claimClass;

  /** The tag table associated with this claim. */
  String tagTable;

  /** The column that contains the claim id. */
  String claimColumnName;
}
