package gov.cms.bfd.server.ng.audit;

import static gov.cms.bfd.server.ng.util.LoggerConstants.BENE_SKS_FOUND;
import static gov.cms.bfd.server.ng.util.LoggerConstants.CLIENT_ID_KEY;
import static gov.cms.bfd.server.ng.util.LoggerConstants.CLIENT_IP_KEY;
import static gov.cms.bfd.server.ng.util.LoggerConstants.CLIENT_NAME_KEY;
import static gov.cms.bfd.server.ng.util.LoggerConstants.COMBINATIONS_EVALUATED;
import static gov.cms.bfd.server.ng.util.LoggerConstants.CURRENT_MATCH_ALGORITHM_VERSION;
import static gov.cms.bfd.server.ng.util.LoggerConstants.FINAL_DETERMINATION;
import static gov.cms.bfd.server.ng.util.LoggerConstants.MATCHED_BENE_SK;
import static gov.cms.bfd.server.ng.util.LoggerConstants.MATCH_ALGORITHM_VERSION;
import static gov.cms.bfd.server.ng.util.LoggerConstants.TIMESTAMP;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;

/** DynamoDB-mapped audit event item. Values are mapped via static table schema builder. */
@Setter
@Getter
public class AuditEventBase {
  private Long matchedBeneSk;
  private String timestamp;
  private Set<Long> beneSksFound;
  private String clientId;
  private String clientName;
  private String clientIp;
  private String combinationsEvaluated;
  private String finalDetermination;
  private String matchAlgorithmVersion;

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private AuditEventId eventId;

  /**
   * Create Audit Table Schema For DynamoDB. We use this rather than @DynamoDbBean. This annotation
   * has known issues with Spring Boot. Supporting documentation under section 4.1.2 <a
   * href="https://docs.awspring.io/spring-cloud-aws/docs/4.0.0/reference/html/index.html">here</a>
   */
  public static final StaticTableSchema<AuditEventBase> TABLE_SCHEMA =
      StaticTableSchema.builder(AuditEventBase.class)
          .newItemSupplier(AuditEventBase::new)
          .addAttribute(
              Long.class,
              a ->
                  a.name(MATCHED_BENE_SK)
                      .getter(AuditEventBase::getMatchedBeneSk)
                      .setter(AuditEventBase::setMatchedBeneSk)
                      .tags(StaticAttributeTags.primaryPartitionKey()))
          .addAttribute(
              String.class,
              a ->
                  a.name(TIMESTAMP)
                      .getter(AuditEventBase::getTimestamp)
                      .setter(AuditEventBase::setTimestamp)
                      .tags(StaticAttributeTags.primarySortKey()))
          .addAttribute(
              EnhancedType.setOf(Long.class),
              a ->
                  a.name(BENE_SKS_FOUND)
                      .getter(AuditEventBase::getBeneSksFound)
                      .setter(AuditEventBase::setBeneSksFound))
          .addAttribute(
              String.class,
              a ->
                  a.name(CLIENT_ID_KEY)
                      .getter(AuditEventBase::getClientId)
                      .setter(AuditEventBase::setClientId))
          .addAttribute(
              String.class,
              a ->
                  a.name(CLIENT_NAME_KEY)
                      .getter(AuditEventBase::getClientName)
                      .setter(AuditEventBase::setClientName))
          .addAttribute(
              String.class,
              a ->
                  a.name(CLIENT_IP_KEY)
                      .getter(AuditEventBase::getClientIp)
                      .setter(AuditEventBase::setClientIp))
          .addAttribute(
              String.class,
              a ->
                  a.name(COMBINATIONS_EVALUATED)
                      .getter(AuditEventBase::getCombinationsEvaluated)
                      .setter(AuditEventBase::setCombinationsEvaluated))
          .addAttribute(
              String.class,
              a ->
                  a.name(FINAL_DETERMINATION)
                      .getter(AuditEventBase::getFinalDetermination)
                      .setter(AuditEventBase::setFinalDetermination))
          .addAttribute(
              String.class,
              a ->
                  a.name(MATCH_ALGORITHM_VERSION)
                      .getter(AuditEventBase::getMatchAlgorithmVersion)
                      .setter(AuditEventBase::setMatchAlgorithmVersion))
          .build();

  /**
   * Formats timestamp string to a Date for FHIR mapping.
   *
   * @return timestamp as Date
   */
  public final Date getTimestampDate() {
    return Date.from(Instant.parse(getTimestamp()));
  }

  /**
   * Generates AuditEvent Resource ID.
   *
   * @return resource id
   */
  public final AuditEventId getAuditId() {
    if (eventId == null) {
      eventId = AuditEventId.fromDynamoTimestamp(getMatchedBeneSk(), getTimestamp());
    }
    return eventId;
  }

  /**
   * Builds a FHIR AuditEvent from the Dynamo Object.
   *
   * @return maps to a FHIR AuditEvent resource for API response
   */
  public final AuditEvent toFhir() {
    var ae = new AuditEvent();
    ae.setMeta(new Meta().addProfile(SystemUrls.AUDIT_EVENT_STRUCTURE_DEF));
    ae.setId(getAuditId().getIdAsString());
    ae.setRecorded(getTimestampDate());
    ae.setType(
        new Coding().setSystem(SystemUrls.AUDIT_EVENT_LIFE_CYCLE_SYSTEM).setCode("transmit"));
    ae.addSubtype()
        .setSystem(SystemUrls.AUDIT_EVENT_SUBTYPE_SYSTEM)
        .setCode("patient-match-request");
    ae.setAction(AuditEvent.AuditEventAction.E);
    ae.setOutcome(AuditEvent.AuditEventOutcome._0);
    ae.setSource(
        new AuditEvent.AuditEventSourceComponent()
            .setObserver(new Reference().setDisplay("CMS Beneficiary FHIR Data Server")));
    ae.addPurposeOfEvent()
        .setText("patient requested")
        .addCoding()
        .setDisplay("patient requested")
        .setCode("PATRQT")
        .setSystem(SystemUrls.SAMHSA_ACT_CODE_SYSTEM_URL);
    var entityComponent = ae.addEntity();
    entityComponent.setRole(
        new Coding()
            .setSystem(SystemUrls.OBJECT_ROLE)
            .setCode("1")
            .setDisplay(ResourceType.Patient.name()));
    entityComponent.setType(
        new Coding()
            .setSystem(SystemUrls.RESOURCE_TYPE)
            .setCode(ResourceType.Patient.name())
            .setDisplay(ResourceType.Patient.name()));
    entityComponent.setWhat(new Reference(ResourceType.Patient.name() + "/" + getMatchedBeneSk()));
    entityComponent
        .setName("Match Info")
        .setDescription(
            "The detail elements refer to the matching algorithm + version combination that resulted in a unique match.");
    entityComponent
        .addDetail()
        .setType("matchAlgorithm")
        .setValue(new StringType(getFinalDetermination()));
    entityComponent
        .addDetail()
        .setType("matchAlgorithmVersion")
        .setValue(
            new StringType(
                Optional.ofNullable(getMatchAlgorithmVersion())
                    .orElse(CURRENT_MATCH_ALGORITHM_VERSION)));

    AuditEvent.AuditEventAgentComponent agent = ae.addAgent();
    agent.setName(getClientName());
    agent.setAltId(getClientName());
    agent.setRequestor(true);
    agent.setNetwork(
        new AuditEvent.AuditEventAgentNetworkComponent()
            .setAddress(getClientIp())
            .setType(AuditEvent.AuditEventAgentNetworkType._2));
    return ae;
  }
}
