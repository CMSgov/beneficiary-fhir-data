package gov.cms.bfd.pipeline.sharedutils.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Contains the yaml entries to the claim samhsa code access. */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ClaimSamhsaCodeEntries {
  /** The claim class. */
  String claimClass;

  /** This class's parent class. Will be null if this is the parent. */
  String table;

  /** End date of the code. */
  List<SamhsaFields> fields;
}
