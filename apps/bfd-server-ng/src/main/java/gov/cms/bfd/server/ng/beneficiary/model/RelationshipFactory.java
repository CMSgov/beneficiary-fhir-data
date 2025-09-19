package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import org.hl7.fhir.r4.model.CodeableConcept;

/**
 * Factory class for creating common FHIR {@link CodeableConcept} instances, particularly for
 * relationships.
 */
public final class RelationshipFactory {

  private RelationshipFactory() {}

  /**
   * Creates a FHIR {@link CodeableConcept} representing the "self" subscriber relationship. This
   * indicates that the beneficiary is their own subscriber.
   *
   * <p>System: {@link SystemUrls#SYS_SUBSCRIBER_RELATIONSHIP}
   * (http://terminology.hl7.org/CodeSystem/subscriber-relationship) Code: "self" Display: "Self"
   *
   * @return A {@link CodeableConcept} for the "self" relationship.
   */
  public static CodeableConcept createSelfSubscriberRelationship() {
    CodeableConcept relationshipCodeableConcept = new CodeableConcept();
    relationshipCodeableConcept
        .addCoding()
        .setSystem(SystemUrls.SYS_SUBSCRIBER_RELATIONSHIP)
        .setCode("self")
        .setDisplay("Self");
    return relationshipCodeableConcept;
  }
}
