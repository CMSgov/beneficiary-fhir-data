package gov.cms.bfd.server.war.r4.providers.preadj.common;

import java.util.Map;
import org.hl7.fhir.r4.model.Enumerations;

public class AbstractTransformerV2 {

  private static final Map<String, Enumerations.AdministrativeGender> GENDER_MAP =
      Map.of(
          "m", Enumerations.AdministrativeGender.MALE,
          "f", Enumerations.AdministrativeGender.FEMALE,
          "u", Enumerations.AdministrativeGender.UNKNOWN);

  protected static Map<String, Enumerations.AdministrativeGender> genderMap() {
    return GENDER_MAP;
  }
}
