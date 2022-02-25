package gov.cms.bfd.server.war;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public interface IDrugCodeProvider {
  String retrieveFDADrugCodeDisplay(String claimDrugCode);

  Map<String, String> readFDADrugCodeFile();

  Set<String> drugCodeLookupMissingFailures = new HashSet<>();
}
