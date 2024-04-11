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
  private static final String REGEX_SYSTEM =
      "[A-Za-z]+\\.[A-Za-z]+\\[N\\]\\.type\\.coding\\[N\\]\\.system";
  private static final String REGEX_CODE =
      "[A-Za-z]+\\.[A-Za-z]+\\[N\\]\\.type\\.coding\\[N\\]\\.code";
  private static final String REGEX_DISPLAY =
      "[A-Za-z]+\\.[A-Za-z]+\\[N\\]\\.type\\.coding\\[N\\]\\.display";

  /**
   * Enrich the eob based on fhir mapping info.
   *
   * @param eob the eob with relevant element(s) enriched
   */
  public ExplanationOfBenefit enrich(ExplanationOfBenefit eob) {
    return eob;
  }

  /**
   * Create codeable concept using fhir mapping.
   *
   * @return CodeableConcept object
   */
  public CodeableConcept createCodeableConcept() {
    Pattern pattern = Pattern.compile("([a-z])");
    Matcher matcher = pattern.matcher("expression here");
    boolean matchFound = matcher.find();
    CodeableConcept cc = null;
    List<FhirMapping> mappings = getFhirMapping();
    FhirMapping mapping = mappings.get(0);
    // handle one mapping case for POC
    Map<String, String> discriminator = parseExpressionList(mapping.getDiscriminator());
    Map<String, String> additional = parseExpressionList(mapping.getAdditional());
    // extract system, code, display from additionals
    String sys = "";
    String code = "";
    String display = "";
    cc = new CodeableConcept().setCoding(Arrays.asList(new Coding(sys, code, display)));
    return cc;
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
      r.put(kv[0], kv[1]);
    }
    return r;
  }
}
