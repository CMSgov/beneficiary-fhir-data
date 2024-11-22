package gov.cms.bfd.pipeline.sharedutils.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** an entry to contain the column and method to retrieve a particular SAMHSA code. */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class SamhsaFields {
  /** The samhsa code's table. */
  String table;

  /** The samhsa code's line num. */
  Short lineNum;

  /** The samhsa code's column for in table. */
  String column;

  /** The samhsa code. */
  String code;
}
