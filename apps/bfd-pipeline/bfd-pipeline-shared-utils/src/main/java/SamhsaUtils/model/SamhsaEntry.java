package SamhsaUtils.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SamhsaEntry {
  String system;
  String code;
  String startDate;
  String endDate;
  String comment;
}
