package gov.cms.bfd.server.war.commons.fhir.ccw.mapper;

import gov.cms.bfd.server.war.commons.MetaModel;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.*;

/** Builder to fabricate FHIR resources and elements and values based on fhir mapping meta info. */
@SuppressWarnings("all")
public class FHIR2CCWMappingBuilder extends FHIR2CCWMapper {
  private static final Map<String, String> elemName2FhirModelClazzFQN =
      Map.ofEntries(
          Map.entry("identifier", "org.hl7.fhir.r4.model.Identifier"),
          Map.entry(
              "supportingInfo",
              "org.hl7.fhir.r4.model.ExplanationOfBenefit$SupportingInformationComponent"));
  private static final Map<String, String> elemName2C4BBCodeSystem =
      Map.ofEntries(
          Map.entry("identifier", "C4BBIdentifierType"),
          Map.entry("supportingInfo", "C4BBSupportingInfoType"));

  // sample fhir expressions to recognize:
  // fhirMapping -> additional for ccw var: pde_id
  // 1. eob.identifier[N].type.coding[N].system =
  // 'http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType'
  // 2. eob.identifier[N].type.coding[N].code = 'uc'
  // 3. eob.identifier[N].type.coding[N].display = 'Unique Claim ID'
  // fhirMapping -> discriminator for ccw var: pde_id
  // 1. identifier[N].system = 'https://bluebutton.cms.gov/resources/variables/pde_id'

  // All fhir path like expressions in DD can be categorized into
  // set of patterns and can be parsed and processed like ExplanationOfBenefit.identifier
  // being processed (enriched) here.
  /**
   * use regex here to parse and process fhir path like expressions seen in FHIR2CCW mappings,
   * <elem>[N].type.coding <elem>[N].category.coding <elem>[N}.code.coding ... now only handle a sub
   * set for demo purpose
   */
  private static final Pattern REGEX_CC_PROPS =
      Pattern.compile(
          "(eob)\\.(identifier|extension|supportingInfo)(\\[N\\])\\.(type|category)\\.coding(\\[N\\])\\.(system|code|display)");

  private static final Pattern REGEX_DISCRIMINATOR =
      Pattern.compile("(identifier|extension|supportingInfo)(\\[N\\])\\.system");

  /**
   * Enrich the eob based on fhir mapping info.
   *
   * @param eob the eob with relevant element(s) enriched
   */
  public ExplanationOfBenefit enrich(ExplanationOfBenefit eob, String val) {
    List<FhirMapping> mappings = getFhirMapping();
    FhirMapping mapping = mappings.get(0);
    // handle one mapping case for POC
    //    Map<String, String> discriminatorMap = parseExpressionList(mapping.getDiscriminator());
    //    Map<String, String> additionalMap = parseExpressionList(mapping.getAdditional());
    // extract system, code, display from additionals
    Map<String, String> discriminators = new HashMap<String, String>();
    String element = null;
    String elemCardinal = null;
    String childName = null;
    String childCodingCardinal = null;
    String elementExpr = mapping.getElement();
    String pathExpr = mapping.getFhirPath();
    String resourceName = mapping.getResource();
    List<String> discriminatorList = mapping.getDiscriminator();
    List<String> additionalList = mapping.getAdditional();
    //    for (Map.Entry<String, String> e : discriminatorMap.entrySet()) {
    //      Matcher m = REGEX_DISCRIMINATOR.matcher(e.getKey());
    //      if (m.matches()) {
    //        element = m.group(1); // assert element stay same
    //        elemCardinal = m.group(2); // when cardinal is [N] add element to array
    //        discriminators.put("system", e.getValue());
    //      }
    //    }
    Map<String, String> ccProps = new HashMap<String, String>();
    //    for (Map.Entry<String, String> e : additionalMap.entrySet()) {
    //      Matcher m = REGEX_CC_PROPS.matcher(e.getKey());
    //      if (m.matches()) {
    //        String resource = m.group(1); // assert resource name stay same, now it's eob
    //        element = m.group(2); // assert element stay same
    //        elemCardinal = m.group(3); // when cardinal is [N] add element to array
    //        childName = m.group(4); // child of the element
    //        childCodingCardinal =
    //            m.group(5); // when typeCoding cardinal is [N] type -> coding is an array of cc
    //        String ccPropName = m.group(6);
    //        ccProps.put(ccPropName, e.getValue());
    //      }
    //    }
    if (elemCardinal != null && elemCardinal.equals("[N]")) {
      // cardinal is N indicate that the codeable concept needs to be
      // added vs set, expecting a method like eob.addIdentifier(...)
      Object fhirComponent = null;
      String elemUpperCase1st = element.substring(0, 1).toUpperCase() + element.substring(1);

      // flat code just for demo of idea
      try {
        // this is for r4, should also applicable to stu3
        fhirComponent =
            Class.forName(elemName2FhirModelClazzFQN.get(element)).getConstructor().newInstance();
      } catch (InstantiationException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }

      Method method = null;

      try {
        // figure out the setter / getter / add methods by fhir model conventions
        method = eob.getClass().getMethod("add" + elemUpperCase1st, fhirComponent.getClass());
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      }

      try {
        method.invoke(eob, fhirComponent);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e);
      }

      CodeableConcept cc =
          MetaModel.getC4BBCodeSystem(
              MetaModel.C4BBProfile.CODESYSTEM, ccProps.get("system"), ccProps.get("code"));
      if (cc == null) {
        // try C4BB CodeSystem first, then probe the expressions in meta
        // probe current fhirComponent's declared methods:
        // setType requires a parameter of CodeableConcept
        cc =
            new CodeableConcept()
                .setCoding(
                    Arrays.asList(
                        new Coding(
                            ccProps.get("system"), ccProps.get("code"), ccProps.get("display"))));
      }

      if (element.equals("identifier")) {
        try {
          // call setters per type of the element
          fhirComponent
              .getClass()
              .getMethod("setSystem", String.class)
              .invoke(fhirComponent, discriminators.get("system"));
          fhirComponent.getClass().getMethod("setType", cc.getClass()).invoke(fhirComponent, cc);
          fhirComponent.getClass().getMethod("setValue", String.class).invoke(fhirComponent, val);
        } catch (NoSuchMethodException e) {
          throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
          throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      } else if (element.equals("supportingInfo")) {
        try {
          CodeableConcept valCode =
              new CodeableConcept()
                  .setCoding(
                      Arrays.asList(
                          new Coding(
                              discriminators.get("system"), val, "Find out from Value Set...")));
          // call setters per type of the element
          fhirComponent
              .getClass()
              .getMethod("setCategory", cc.getClass())
              .invoke(fhirComponent, cc);
          // fake a seq here
          fhirComponent.getClass().getMethod("setSequence", int.class).invoke(fhirComponent, 1);
          fhirComponent
              .getClass()
              .getMethod("setCode", valCode.getClass())
              .invoke(fhirComponent, valCode);
          fhirComponent
              .getClass()
              .getMethod("setTiming", org.hl7.fhir.r4.model.Type.class)
              .invoke(fhirComponent, new org.hl7.fhir.r4.model.DateType());
        } catch (NoSuchMethodException e) {
          throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
          throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      } else {
        throw new NotImplementedException("to be implemented");
      }
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
      // fhir mapping expressions need cleansing and normalization:
      // e.g. there are paranteisis quoted expressions with OR operator, e.g. icd-9 OR icd-10
      // strip off the ( and ) for now
      if (k.startsWith("(")) k = k.substring(1);
      if (k.endsWith(")")) k = k.substring(0, k.length() - 1);
      r.put(k, v);
    }
    return r;
  }
}
