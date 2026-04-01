package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.*;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.ParametersUtil;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.base.CharMatcher;
import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
import gov.cms.bfd.server.ng.log.LogStreamAuditLogger;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.LoggerConstants;
import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.model.*;

// Tells JUnit to re-use the same test instance per class
// This is fine because we do not (and should not) have tests that rely on shared static state
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PatientMatchIT extends IntegrationTestBase {
  private ListAppender<ILoggingEvent> logAppender;

  private Bundle searchBundle(Patient patient) {
    var ctx = FhirContext.forR4();
    var params = ParametersUtil.newInstance(ctx);
    ParametersUtil.addParameterToParameters(ctx, params, "IDIPatient", patient);

    var res =
        getFhirClient(ctx)
            .operation()
            .onType(Patient.class)
            .named("idi-match")
            .withParameters(params)
            .withAdditionalHeader("X-CLIENT-IP", "127.0.0.1")
            .withAdditionalHeader("X-CLIENT-NAME", "test-client")
            .withAdditionalHeader("X-CLIENT-ID", "client-123")
            .returnResourceType(Bundle.class)
            .execute();
    // Organization should always be the first entry in the bundle
    assertEquals(
        ResourceType.Organization, res.getEntry().getFirst().getResource().getResourceType());

    return res;
  }

  private Patient buildRequest(
      Optional<String> firstName,
      Optional<String> lastName,
      Optional<Date> birthDate,
      List<Address> addresses,
      Optional<String> mbi,
      Optional<String> ssnLastFour) {
    var patient = new Patient();
    var name = new HumanName();
    firstName.ifPresent(name::addGiven);
    lastName.ifPresent(name::setFamily);
    patient.setName(List.of(name));
    birthDate.ifPresent(patient::setBirthDate);
    addresses.forEach(patient::addAddress);
    mbi.ifPresent(
        m -> patient.addIdentifier(new Identifier().setSystem(SystemUrls.CMS_MBI).setValue(m)));
    ssnLastFour.ifPresent(
        s -> patient.addIdentifier(new Identifier().setSystem(SystemUrls.US_SSN).setValue(s)));

    return patient;
  }

  @Test
  void emptyRequestReturnsEmptyBundle() {
    var res = searchBundle(new Patient());
    assertEquals(1, res.getEntry().size());
  }

  @Test
  void patientMatchSnapshot() {
    var testBene = TestBene.fromBene(getBeneficiaryFromBeneSk("-300428640"));
    var patient =
        buildRequest(
            Optional.of(testBene.firstName),
            Optional.of(testBene.lastName),
            Optional.of(testBene.birthDate),
            List.of(testBene.address),
            Optional.of(testBene.mbi),
            Optional.of(testBene.ssnLastFour));
    var bundle = searchBundle(patient);
    expectFhir().toMatchSnapshot(bundle);
  }

  private Stream<Arguments> verifyPatientMatch() {
    var testBene = TestBene.fromBene(getBeneficiaryFromBeneSk("-300428640"));
    var testBeneWithSpaces = TestBene.fromBene(getBeneficiaryFromBeneSk("-18976899"));
    // precondition - should have whitespace in the name
    assertTrue(testBeneWithSpaces.bene.getBeneficiaryName().getFirstName().contains(" "));

    var duplicateBene1 = TestBene.fromBene(getBeneficiaryFromBeneSk("-591866793"));
    var duplicateBene2 = TestBene.fromBene(getBeneficiaryFromBeneSk("-591866794"));
    // precondition - these two should have the same info but different xref sks
    assertNotEquals(duplicateBene1.bene.getXrefSk(), duplicateBene2.bene.getXrefSk());
    assertEquals(
        duplicateBene1.bene.getBeneficiaryName(), duplicateBene2.bene.getBeneficiaryName());
    assertEquals(duplicateBene1.bene.getBirthDate(), duplicateBene2.bene.getBirthDate());
    assertEquals(
        duplicateBene1.bene.getSsnLastFourDigits(), duplicateBene2.bene.getSsnLastFourDigits());

    var historicalBene = TestBene.fromBene(getBeneficiaryFromBeneSk("-415220062", 0));
    var currentBene = TestBene.fromBene(getBeneficiaryFromBeneSk("-415220062", 1));

    // precondition - same bene, different addresses
    assertEquals(historicalBene.bene.getBeneSk(), currentBene.bene.getBeneSk());
    assertNotEquals(historicalBene.bene.getAddress(), currentBene.bene.getAddress());

    var beneWithDiacritics = TestBene.fromBene(getBeneficiaryFromBeneSk("-614916732"));
    // precondition - first and last name should contain diacritics
    assertFalse(CharMatcher.ascii().matchesAllOf(beneWithDiacritics.firstName));
    assertFalse(CharMatcher.ascii().matchesAllOf(beneWithDiacritics.lastName));

    var xrefBeneOld = TestBene.fromBene(getBeneficiaryFromBeneSk("-752271050"));
    var xrefBeneCurrent = TestBene.fromBene(getBeneficiaryFromBeneSk("-408071088"));
    // precondition - xrefBeneOld should be xrefed to xrefBeneCurrent
    assertEquals(xrefBeneOld.bene.getXrefSk(), xrefBeneCurrent.bene.getXrefSk());
    assertNotEquals(xrefBeneOld.bene.getBeneSk(), xrefBeneCurrent.bene.getXrefSk());
    assertEquals(xrefBeneOld.bene.getXrefSk(), xrefBeneCurrent.bene.getBeneSk());
    assertNotEquals(
        xrefBeneOld.bene.getIdentifier().getMbi(), xrefBeneCurrent.bene.getIdentifier().getMbi());
    return Stream.of(
        Arguments.of(
            "Scenario 1 - first name, last name, DOB, address",
            testBene.bene,
            Optional.of(testBene.firstName),
            Optional.of(testBene.lastName),
            Optional.of(testBene.birthDate),
            List.of(testBene.address),
            Optional.empty(),
            Optional.empty(),
            Optional.of(1)),
        Arguments.of(
            "Scenario 1 - name with spaces",
            testBeneWithSpaces.bene,
            Optional.of(testBeneWithSpaces.firstName.replace(" ", "")),
            Optional.of(testBeneWithSpaces.lastName),
            Optional.of(testBeneWithSpaces.birthDate),
            List.of(testBeneWithSpaces.address),
            Optional.empty(),
            Optional.empty(),
            Optional.of(1)),
        Arguments.of(
            "Scenario 1 - invalid/should fail",
            testBene.bene,
            Optional.of(testBene.firstName),
            Optional.of(testBene.lastName),
            Optional.of(testBene.birthDate),
            List.of(
                new Address()
                    .addLine("3728 Broadway Avenue J")
                    .setCity(testBene.address.getCity())
                    .setState(testBene.address.getState())
                    .setPostalCode(testBene.address.getPostalCode())),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()),
        Arguments.of(
            "Scenario 1 - duplicate bene should return no results",
            duplicateBene1.bene,
            Optional.of(duplicateBene1.firstName),
            Optional.of(duplicateBene1.lastName),
            Optional.of(duplicateBene1.birthDate),
            List.of(duplicateBene1.address),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()),
        Arguments.of(
            "Scenario 1 - historical address should not match",
            historicalBene.bene,
            Optional.of(historicalBene.firstName),
            Optional.of(historicalBene.lastName),
            Optional.of(historicalBene.birthDate),
            List.of(historicalBene.address),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()),
        Arguments.of(
            "Scenario 1 - current address should match",
            currentBene.bene,
            Optional.of(currentBene.firstName),
            Optional.of(currentBene.lastName),
            Optional.of(currentBene.birthDate),
            List.of(currentBene.address),
            Optional.empty(),
            Optional.empty(),
            Optional.of(1)),
        Arguments.of(
            "Scenario 1 - current and historical address should match",
            currentBene.bene,
            Optional.of(currentBene.firstName),
            Optional.of(currentBene.lastName),
            Optional.of(currentBene.birthDate),
            List.of(currentBene.address, historicalBene.address),
            Optional.empty(),
            Optional.empty(),
            Optional.of(1)),
        Arguments.of(
            "Scenario 1 - current and historical address should match (reverse order)",
            currentBene.bene,
            Optional.of(currentBene.firstName),
            Optional.of(currentBene.lastName),
            Optional.of(currentBene.birthDate),
            List.of(historicalBene.address, currentBene.address),
            Optional.empty(),
            Optional.empty(),
            Optional.of(1)),
        Arguments.of(
            "Scenario 1 - xref",
            xrefBeneOld.bene,
            Optional.of(xrefBeneOld.firstName),
            Optional.of(xrefBeneOld.lastName),
            Optional.of(xrefBeneOld.birthDate),
            List.of(xrefBeneOld.address),
            Optional.empty(),
            Optional.empty(),
            Optional.of(1)),
        Arguments.of(
            "Scenario 4 - first name, last name, DOB, SSN last 4",
            testBene.bene,
            Optional.of(testBene.firstName),
            Optional.of(testBene.lastName),
            Optional.of(testBene.birthDate),
            List.of(),
            Optional.empty(),
            Optional.of(testBene.ssnLastFour),
            Optional.of(4)),
        Arguments.of(
            "Scenario 4 - duplicate bene should return no results",
            duplicateBene1.bene,
            Optional.of(duplicateBene1.firstName),
            Optional.of(duplicateBene1.lastName),
            Optional.of(duplicateBene1.birthDate),
            List.of(),
            Optional.empty(),
            Optional.of(duplicateBene1.ssnLastFour),
            Optional.empty()),
        Arguments.of(
            "Scenario 4 - invalid/should fail",
            testBene.bene,
            Optional.of(testBene.firstName),
            Optional.of(testBene.lastName),
            Optional.of(testBene.birthDate),
            List.of(),
            Optional.empty(),
            Optional.of("fakeSsn"),
            Optional.empty()),
        Arguments.of(
            "Scenario 4 - name with diacritics",
            beneWithDiacritics.bene,
            Optional.of(beneWithDiacritics.firstName),
            Optional.of(beneWithDiacritics.lastName),
            Optional.of(beneWithDiacritics.birthDate),
            List.of(),
            Optional.empty(),
            Optional.of(beneWithDiacritics.ssnLastFour),
            Optional.empty()),
        Arguments.of(
            "Scenario 4 - xref",
            xrefBeneOld.bene,
            Optional.of(xrefBeneOld.firstName),
            Optional.of(xrefBeneOld.lastName),
            Optional.of(xrefBeneOld.birthDate),
            List.of(),
            Optional.empty(),
            Optional.of(xrefBeneOld.ssnLastFour),
            Optional.of(4)),
        Arguments.of(
            " Scenario 1 failure, scenario 4 success",
            testBene.bene,
            Optional.of(testBene.firstName),
            Optional.of(testBene.lastName),
            Optional.of(testBene.birthDate),
            List.of(new Address().addLine("fake")),
            Optional.empty(),
            Optional.of(testBene.ssnLastFour),
            Optional.of(4)),
        // Note: it's impossible for this to produce two beneficiaries with different xrefs
        // because we already protect against MBIs incorrectly attributed to multiple benes
        Arguments.of(
            "Scenario 8 - first name, DOB, MBI",
            testBene.bene,
            Optional.of(testBene.firstName),
            Optional.empty(),
            Optional.of(testBene.birthDate),
            List.of(),
            Optional.of(testBene.mbi),
            Optional.empty(),
            Optional.of(8)),
        Arguments.of(
            " Scenario 8 - invalid/should fail",
            testBene.bene,
            Optional.of(testBene.firstName),
            Optional.empty(),
            Optional.of(testBene.birthDate),
            List.of(),
            Optional.of("wrongMbi"),
            Optional.empty(),
            Optional.empty()),
        Arguments.of(
            "Scenario 1/4 fail, scenario 8 success",
            duplicateBene1.bene,
            Optional.of(duplicateBene1.firstName),
            Optional.of(duplicateBene1.lastName),
            Optional.of(duplicateBene1.birthDate),
            List.of(duplicateBene1.address),
            Optional.of(duplicateBene1.mbi),
            Optional.of(duplicateBene1.ssnLastFour),
            Optional.of(8)),
        Arguments.of(
            "Scenario 8 - xref",
            xrefBeneOld.bene,
            Optional.of(xrefBeneOld.firstName),
            Optional.empty(),
            Optional.of(xrefBeneOld.birthDate),
            List.of(),
            Optional.of(xrefBeneOld.mbi),
            Optional.empty(),
            Optional.of(8)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource
  void verifyPatientMatch(
      String testName,
      Beneficiary beneficiary,
      Optional<String> firstName,
      Optional<String> lastName,
      Optional<Date> birthDate,
      List<Address> addresses,
      Optional<String> mbi,
      Optional<String> ssnLastFour,
      Optional<Integer> expectedMatchNumber) {
    var patient = buildRequest(firstName, lastName, birthDate, addresses, mbi, ssnLastFour);
    var bundle = searchBundle(patient);
    assertEquals(expectedMatchNumber.isPresent() ? 2 : 1, bundle.getEntry().size());
    if (expectedMatchNumber.isEmpty()) {
      return;
    }
    var entry =
        bundle.getEntry().stream()
            .filter(b -> !SystemUrls.CMS_GOV.equals(b.getFullUrl()))
            .findFirst()
            .get();
    var patientResult = (Patient) entry.getResource();

    var replaceCount =
        patientResult.getLink().stream()
            .filter(l -> l.getType() == Patient.LinkType.REPLACEDBY)
            .count();
    // xref benes won't have bene information supplied
    if (replaceCount == 1) {
      return;
    }
    var name = patientResult.getName().stream().findFirst().get();
    var expectedName = beneficiary.getBeneficiaryName();
    assertEquals(expectedName.getFirstName(), name.getGiven().getFirst().toString());
    assertEquals(expectedName.getLastName(), name.getFamily());

    assertEquals(DateUtil.toDate(beneficiary.getBirthDate()), patientResult.getBirthDate());
    var inputMbi = beneficiary.getIdentifier().getMbi();
    assert (patientResult.getIdentifier().stream()
            .filter(i -> i.getSystem().equals(SystemUrls.CMS_MBI) && i.getValue().equals(inputMbi))
            .count()
        == 1);
  }

  private record TestBene(
      String firstName,
      String lastName,
      Address address,
      Date birthDate,
      String mbi,
      String ssnLastFour,
      Beneficiary bene) {
    private static TestBene fromBene(Beneficiary testBene) {
      var firstName = testBene.getBeneficiaryName().getFirstName();
      var lastName = testBene.getBeneficiaryName().getLastName();
      var birthDate = DateUtil.toDate(testBene.getBirthDate());
      var addressIn = testBene.getAddress();
      var combinedLines =
          Stream.of(addressIn.getAddressLine1(), addressIn.getAddressLine2())
              .flatMap(Optional::stream)
              .map(TestBene::normalizeAddress)
              .collect(Collectors.joining(" "));
      var address =
          new Address()
              .addLine(combinedLines)
              .setCity(addressIn.getCity().get())
              .setState(addressIn.getStateCode().get())
              .setPostalCode(addressIn.getZipCode().get());
      var ssnLastFour = testBene.getSsnLastFourDigits();
      var mbi = testBene.getIdentifier().getMbi();
      return new TestBene(firstName, lastName, address, birthDate, mbi, ssnLastFour, testBene);
    }

    private static String normalizeAddress(String address) {
      return address.replace("Avenue", "Ave");
    }
  }

  @ParameterizedTest(name = "{0} - audit log")
  @MethodSource("verifyPatientMatch")
  void verifyAuditLog(
      String testName,
      Beneficiary beneficiary,
      Optional<String> firstName,
      Optional<String> lastName,
      Optional<Date> birthDate,
      List<Address> addresses,
      Optional<String> mbi,
      Optional<String> ssnLastFour,
      Optional<Integer> expectedMatchNumber) {
    var patient = buildRequest(firstName, lastName, birthDate, addresses, mbi, ssnLastFour);
    searchBundle(patient);
    var auditRecords = getAuditRecordFromDynamo(beneficiary.getBeneSk());
    var logs = logAppender.list;

    if (expectedMatchNumber.isPresent()) {
      assertFalse(auditRecords.isEmpty(), "Expected audit record for " + testName);
      var matchedScenario =
          auditRecords.stream()
              .anyMatch(r -> Objects.equals(r.scenarioIndex(), expectedMatchNumber.get()));
      assertTrue(
          matchedScenario, "Expected successful patient match combination not found in audit log");

      assertFalse(logs.isEmpty(), "Expected log stream audit record for " + testName);
      var foundStandardAuditLog =
          logs.stream()
              .anyMatch(
                  event ->
                      event
                          .getFormattedMessage()
                          .contains(LoggerConstants.PATIENT_MATCH_REQUESTED));
      assertTrue(foundStandardAuditLog, "Expected standard audit log for " + testName);
    } else {
      assertTrue(auditRecords.isEmpty(), "Expected no audit records for " + testName);
      assertTrue(logs.isEmpty(), "Expected no log stream audit records for " + testName);
    }
  }

  @BeforeAll
  void setupDynamDbTable() {
    dynamoDbClient.createTable(
        CreateTableRequest.builder()
            .tableName(configuration.getPatientMatchAuditTableName())
            .keySchema(
                KeySchemaElement.builder()
                    .attributeName("matchedBeneSk")
                    .keyType(KeyType.HASH)
                    .build(),
                KeySchemaElement.builder()
                    .attributeName("timestamp")
                    .keyType(KeyType.RANGE)
                    .build())
            .attributeDefinitions(
                AttributeDefinition.builder()
                    .attributeName("matchedBeneSk")
                    .attributeType(ScalarAttributeType.N)
                    .build(),
                AttributeDefinition.builder()
                    .attributeName("timestamp")
                    .attributeType(ScalarAttributeType.S)
                    .build())
            .billingMode(BillingMode.PAY_PER_REQUEST)
            .build());
  }

  @BeforeEach
  void clearDynamDbTable() {
    var tableName = configuration.getPatientMatchAuditTableName();
    var response = dynamoDbClient.scan(d -> d.tableName(tableName));
    for (var item : response.items()) {
      dynamoDbClient.deleteItem(
          d ->
              d.tableName(tableName)
                  .key(
                      Map.of(
                          "matchedBeneSk",
                          item.get("matchedBeneSk"),
                          "timestamp",
                          item.get("timestamp"))));
    }
  }

  @BeforeEach
  void setupLogAppender() {
    var logger = (Logger) LoggerFactory.getLogger(LogStreamAuditLogger.class);
    logAppender = new ListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);
  }
}
