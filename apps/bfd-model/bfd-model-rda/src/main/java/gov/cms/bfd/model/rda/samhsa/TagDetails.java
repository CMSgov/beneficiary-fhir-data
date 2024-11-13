package gov.cms.bfd.model.rda.samhsa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** An object to contain the tag details. This will be converted to JSON before persisting. */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TagDetails {
  /** The claims table of the SAMHSA code. */
  String table;

  /** The column in the claims table. */
  String column;

  /** The line entry in the table. */
  Integer clm_line_num;

  /** The type. */
  String type;
}
