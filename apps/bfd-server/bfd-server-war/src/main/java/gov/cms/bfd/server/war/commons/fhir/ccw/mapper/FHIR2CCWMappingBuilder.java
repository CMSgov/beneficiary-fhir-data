package gov.cms.bfd.server.war.commons.fhir.ccw.mapper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hl7.fhir.r4.model.*;

/** Builder to fabricate FHIR resources and elements and values based on fhir mapping meta info. */
@SuppressWarnings("all")
public class FHIR2CCWMappingBuilder extends FHIR2CCWMapper {
  // sample fhir expressions to recognize:
  // fhirMapping -> additional for ccw var: pde_id
  // 1. eob.identifier[N].type.coding[N].system =
  // 'http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType'
  // 2. eob.identifier[N].type.coding[N].code = 'uc'
  // 3. eob.identifier[N].type.coding[N].display = 'Unique Claim ID'
  // fhirMapping -> discriminator for ccw var: pde_id
  // 1. identifier[N].system = 'https://bluebutton.cms.gov/resources/variables/pde_id'
  private static final Pattern REGEX_CC_PROPS =
      Pattern.compile(
          "(eob)\\.(identifier|extension|supportingInfo)([\\[N\\]])\\.type\\.coding([\\[N\\]])\\.(system|code|display)");
  private static final Pattern REGEX_DISCRIMINATOR =
      Pattern.compile("(identifier|extension|supportingInfo)([\\[N\\]])\\.system");

  /**
   * Enrich the eob based on fhir mapping info.
   *
   * @param eob the eob with relevant element(s) enriched
   */
  public ExplanationOfBenefit enrich(ExplanationOfBenefit eob, String val) {
    List<FhirMapping> mappings = getFhirMapping();
    FhirMapping mapping = mappings.get(0);
    // handle one mapping case for POC
    Map<String, String> discriminatorMap = parseExpressionList(mapping.getDiscriminator());
    Map<String, String> additionalMap = parseExpressionList(mapping.getAdditional());
    // extract system, code, display from additionals
    Map<String, String> discriminators = new HashMap<String, String>();
    String element = null;
    String elemCardinal = null;
    String typeCodingCardinal = null;
    for (Map.Entry<String, String> e : discriminatorMap.entrySet()) {
      Matcher m = REGEX_DISCRIMINATOR.matcher(e.getKey());
      if (m.matches()) {
        element = m.group(1); // assert element stay same
        elemCardinal = m.group(2); // when cardinal is [N] add element to array
        String valSysURL = m.group(5);
        discriminators.put("system", e.getValue());
      }
    }
    Map<String, String> ccProps = new HashMap<String, String>();
    for (Map.Entry<String, String> e : additionalMap.entrySet()) {
      Matcher m = REGEX_CC_PROPS.matcher(e.getKey());
      if (m.matches()) {
        String resource = m.group(1); // assert resource name stay same, now it's eob
        element = m.group(2); // assert element stay same
        elemCardinal = m.group(3); // when cardinal is [N] add element to array
        typeCodingCardinal =
            m.group(4); // when typeCoding cardinal is [N] type -> coding is an array of cc
        String ccPropName = m.group(5);
        ccProps.put(ccPropName, e.getValue());
      }
    }
    if (elemCardinal != null && elemCardinal.equals("[N]")) {
      Identifier identifier = new Identifier();
      CodeableConcept cc =
          new CodeableConcept()
              .setCoding(
                  Arrays.asList(
                      new Coding(
                          ccProps.get("system"), ccProps.get("code"), ccProps.get("display"))));
      identifier.setSystem(discriminators.get("system"));
      identifier.setType(cc);
      identifier.setValue(val);
      // to do: use reflection
      eob.addIdentifier(identifier);
    }

    return eob;
  }

  /**
   * helper to parse the fhirMapping -> discriminator, and fhirMapping -> additional
   *
   * @param l list of expressions in the form of path = value
   * @return map of <path, value>
   */
  private Map<String, String> parseExpressionList(List<String> l) {
    Map<String, String> r = new HashMap<String, String>();
    for (String e : l) {
      String[] kv = e.split("=");
      String k = kv[0].trim().replaceAll("^\"|\"$", "").replaceAll("^'|'$", "");
      String v = kv[1].trim().replaceAll("^\"|\"$", "").replaceAll("^'|'$", "");
      r.put(k, v);
    }
    return r;
  }
}
