package gov.cms.bfd.pipeline.sharedutils.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A SAMHSA code entry. */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SamhsaEntry {
  /** The system. */
  String system;

  /** The SAMHSA code. */
  String code;

  /** Start date of the code. */
  String startDate;

  /** End date of the code. */
  String endDate;

  /** Comment. */
  String comment;
}
