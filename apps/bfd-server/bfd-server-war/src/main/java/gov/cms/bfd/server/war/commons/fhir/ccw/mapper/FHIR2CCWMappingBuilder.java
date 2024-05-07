package gov.cms.bfd.server.war.commons.fhir.ccw.mapper;

import gov.cms.bfd.server.war.commons.MetaModel;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
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
          "(identifier|extension|supportingInfo)\\.(code|type|category)\\.coding\\((.*)\\)");

  // expression in discriminator is for the element's value and it's URL
  // e.g. for ccw var: clm_id, it's value's URL is:
  // system='https://bluebutton.cms.gov/resources/variables/clm_id'
  private static final Pattern REGEX_DISCRIMINATOR =
      Pattern.compile("(identifier|extension|supportingInfo)\\((.*)\\)");

  /**
   * Enrich the eob based on fhir mapping info.
   *
   * @param eob the eob with relevant element(s) enriched
   */
  public ExplanationOfBenefit enrich(ExplanationOfBenefit eob, String val) {
    List<FhirMapping> mappings = getFhirMapping();
    FhirMapping mapping = mappings.get(0);
    Map<String, String> discriminators = new HashMap<String, String>();
    String elementExpr = mapping.getElement();
    String pathExpr = mapping.getFhirPath();
    String resourceName = mapping.getResource();
    List<String> discriminatorList = mapping.getDiscriminator();
    List<String> additionalList = mapping.getAdditional();
    String[] elemSplit = elementExpr.split("\\.");

    String elemName = elemSplit[0];
    // take ccw var: clm_id for example:
    // from the meta data infering the below code:
    // element = identifier.value, need to create an identifer object and set its value as below:
    // the element = "identifier" -> eob.getIdentifier() <== a list of identifier ->
    // eob.getIdentifier().add(id = new Identifier())
    // then id.setValue(String val),
    // the discriminator =
    // identifier(system='https://bluebutton.cms.gov/resources/variables/pde_id'), it provides info
    // to set "system" property of the value:
    // eob.getIdentifier().add(new
    // Identifier().setSystem("https://bluebutton.cms.gov/resources/variables/pde_id"))
    // id.setType(CodeableConcept cc)
    // where the
    // eob.getIdentifier().add(new Identifier().setType(CodeableConcept cc))
    // where the CodeableConcept is derived fron additional =
    // identifier.type.coding(system='http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType', code='uc', display='Unique Claim ID')
    Object elementObj = createElement(elemName);
    Method getterMethod = getGetterMethod(eob, elemName);
    Object r = callMethod(getterMethod, eob);
    Coding coding = extractCoding(additionalList);
    // check first if it's C4BB
    CodeableConcept cc =
        MetaModel.getC4BBCodeSystem(
            MetaModel.C4BBProfile.CODESYSTEM, coding.getSystem(), coding.getCode());
    if (cc == null) {
      cc = new CodeableConcept().setCoding(Arrays.asList(coding));
    }
    String systemURLStr = extractSystemURL(discriminatorList);
    // exhausted list of elements - e.g. here for EOB resources for POC demo
    if (elemName.equals("identifier")) {
      try {
        // EOB.identifier is a collection, call add to add element
        r.getClass().getMethod("add", Object.class).invoke(r, elementObj);
        // call setters per type of the element
        elementObj.getClass().getMethod("setSystem", String.class).invoke(elementObj, systemURLStr);
        elementObj.getClass().getMethod("setType", cc.getClass()).invoke(elementObj, cc);
        elementObj.getClass().getMethod("setValue", String.class).invoke(elementObj, val);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    } else if (elemName.equals("supportingInfo")) {
      try {
        CodeableConcept valCode =
            new CodeableConcept()
                .setCoding(
                    Arrays.asList(new Coding(systemURLStr, val, "Find out from Value Set...")));
        // EOB.supportingInfo is a collection, call add to add element
        r.getClass().getMethod("add", Object.class).invoke(r, elementObj);
        // call setters per type of the element
        elementObj.getClass().getMethod("setCategory", cc.getClass()).invoke(elementObj, cc);
        // fake a seq here
        elementObj.getClass().getMethod("setSequence", int.class).invoke(elementObj, 1);
        elementObj.getClass().getMethod("setCode", valCode.getClass()).invoke(elementObj, valCode);
        elementObj
            .getClass()
            .getMethod("setTiming", org.hl7.fhir.r4.model.Type.class)
            .invoke(elementObj, new org.hl7.fhir.r4.model.DateType());
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
    return eob;
  }

  private String extractSystemURL(List<String> exprList) {
    String expr = exprList.get(0);
    Matcher m = REGEX_DISCRIMINATOR.matcher(expr);
    if (m.matches()) {
      String elemName = m.group(1); // element name
      String arg = m.group(2); // args
      if (arg.startsWith("system")) {
        return arg.replaceFirst("system\\s*=\\s*", "");
      }
    } else {
      // try supportingInfo.code.coding regex
      Coding c = extractCoding(exprList);
      return c.getSystem();
    }
    throw new RuntimeException(
        "Unknown/Unexpected expression in fhir mapping -> discriminator: " + exprList.toString());
  }

  /**
   * Given a list of FHIR code expression, derive a CodeableConcept from it. e.g. for ccw var -
   * clm_id: "additional" : [
   * "identifier.type.coding(system='http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType',
   * code='uc', display='Unique Claim ID'))" ],
   *
   * @param exprList list of code spec e.g.
   * @return codeable concept object.
   */
  private Coding extractCoding(List<String> exprList) {
    Coding coding = new Coding();
    // assume only one expression in the list
    Matcher m = REGEX_CC_PROPS.matcher(exprList.get(0));
    if (m.matches()) {
      String elemName = m.group(1); // element of resource, e.g. extension, supportingInfo, category
      String elemPropName = m.group(2); // property name (setter / getter) of the element
      String argName =
          m.group(3); // arg of the coding infers to FHIR Coding type which accepts system, code,
      // display
      String[] args = argName.split(",");
      for (String arg : args) {
        String a = arg.trim();
        if (a.startsWith("system")) {
          coding.setSystem(a.replaceFirst("^system\\s*=\\s*", "").replaceAll("^'|'$", ""));
        } else if (a.startsWith("code")) {
          coding.setCode(a.replaceFirst("^code\\s*=\\s*", "").replaceAll("^'|'$", ""));
        } else if (a.startsWith("display")) {
          coding.setDisplay(a.replaceFirst("^display\\s*=\\s*", "").replaceAll("^'|'$", ""));
        }
      }
    } else {
      throw new RuntimeException(
          "Unknown/Unexpected expression in fhir mapping -> additional: " + exprList.toString());
    }
    return coding;
  }

  /**
   * Call the getter on the resource.
   *
   * @param getterMethod
   * @param eob
   * @return getter returned object
   */
  private Object callMethod(Method getterMethod, ExplanationOfBenefit eob) {
    try {
      return getterMethod.invoke(eob);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Giben resource instance and its element name, figure out the getter for the element.
   *
   * @param eob fhir resource
   * @param elemName the element name
   * @return the method
   */
  private Method getGetterMethod(ExplanationOfBenefit eob, String elemName) {
    try {
      // figure out the setter / getter / add methods by fhir model conventions
      return eob.getClass()
          .getMethod("get" + elemName.substring(0, 1).toUpperCase() + elemName.substring(1));
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Given element name of a resource e.g. EOB, look up its FHIR class and create a new instance.
   *
   * @param elemName - name of the element, e.g. "identifier" of ExplanationOfBenefit
   * @return the instance.
   */
  private Object createElement(String elemName) {
    try {
      // this is for r4, should also applicable to stu3
      return Class.forName(elemName2FhirModelClazzFQN.get(elemName)).getConstructor().newInstance();
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
  }
}
