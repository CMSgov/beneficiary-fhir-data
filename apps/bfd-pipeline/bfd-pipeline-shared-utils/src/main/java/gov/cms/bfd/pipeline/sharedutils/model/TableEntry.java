package gov.cms.bfd.pipeline.sharedutils.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/** POJO to contain information about the tables to use in the backfill SQL queries. */
@Getter
@Setter
@AllArgsConstructor
public class TableEntry {
  /** The query to get the dates from the parent table. */
  String datesQuery;

  /** The tag table associated with this claim. */
  String tagTable;

  /** The column that contains the claim id. */
  String claimField;

  /** The column holding the begin date of the claim. */
  String fromDateField;

  /** The column holding the end date of the claim. */
  String toDateField;

  /** The line item field. */
  String lineItemField;

  /** Claim table. */
  String claimTable;

  /** Parent table. */
  String parentTable;

  /** True if this is a line item table. */
  Boolean lineItem;
}
