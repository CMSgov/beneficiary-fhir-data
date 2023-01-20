package gov.cms.bfd.server.war.commons;

import static org.junit.jupiter.api.Assertions.*;

import gov.cms.bfd.server.war.r4.providers.TransformerUtilsV2;
import gov.cms.bfd.server.war.stu3.providers.TransformerUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Tests for the {@link LoggingUtils}. */
public class LoggingUtilsTest {

  /**
   * Test to see that {@link LoggingUtils#findBeneIds(org.hl7.fhir.dstu3.model.Bundle)} returns no
   * beneficiaryIds to log to BfdMdc in v1.
   */
  @Test
  public void testMdcLogsForEmptyBundleV1() {
    org.hl7.fhir.dstu3.model.Bundle bundle = new org.hl7.fhir.dstu3.model.Bundle();
    TransformerUtils.addResourcesToBundle(
        bundle, Collections.singletonList(new org.hl7.fhir.dstu3.model.Patient()));
    assertTrue(LoggingUtils.findBeneIds(bundle).isEmpty());

    bundle = new org.hl7.fhir.dstu3.model.Bundle();
    TransformerUtils.addResourcesToBundle(
        bundle, Collections.singletonList(new org.hl7.fhir.dstu3.model.Coverage()));
    assertTrue(LoggingUtils.findBeneIds(bundle).isEmpty());

    bundle = new org.hl7.fhir.dstu3.model.Bundle();
    TransformerUtils.addResourcesToBundle(
        bundle, Collections.singletonList(new org.hl7.fhir.dstu3.model.ExplanationOfBenefit()));
    assertTrue(LoggingUtils.findBeneIds(bundle).isEmpty());
  }

  /**
   * Test to see that {@link LoggingUtils#findBeneIds(org.hl7.fhir.dstu3.model.Bundle)} returns a
   * single beneficiaryId to log to BfdMdc in v1.
   */
  @Test
  public void testMdcLogsForSingleBundleV1() {
    org.hl7.fhir.dstu3.model.Bundle bundle = new org.hl7.fhir.dstu3.model.Bundle();
    TransformerUtils.addResourcesToBundle(
        bundle, Collections.singletonList(new org.hl7.fhir.dstu3.model.Patient().setId("567834")));

    Set<Long> beneIds = LoggingUtils.findBeneIds(bundle);
    assertFalse(beneIds.isEmpty());
    assertEquals(1, beneIds.size());
    assertEquals("567834", beneIds.iterator().next().toString());

    bundle = new org.hl7.fhir.dstu3.model.Bundle();
    TransformerUtils.addResourcesToBundle(
        bundle,
        Collections.singletonList(
            new org.hl7.fhir.dstu3.model.Coverage()
                .setBeneficiary(new org.hl7.fhir.dstu3.model.Reference("Patient/567834"))));
    assertFalse(beneIds.isEmpty());
    assertEquals(1, beneIds.size());
    assertEquals("567834", beneIds.iterator().next().toString());

    bundle = new org.hl7.fhir.dstu3.model.Bundle();
    TransformerUtils.addResourcesToBundle(
        bundle,
        Collections.singletonList(
            new org.hl7.fhir.dstu3.model.ExplanationOfBenefit()
                .setPatient(new org.hl7.fhir.dstu3.model.Reference("Patient/567834"))));
    assertFalse(beneIds.isEmpty());
    assertEquals(1, beneIds.size());
    assertEquals("567834", beneIds.iterator().next().toString());
  }

  /**
   * Test to see that {@link LoggingUtils#findBeneIds(org.hl7.fhir.dstu3.model.Bundle)} returns
   * multiple beneficiaryIds to log to BfdMdc in v1.
   */
  @Test
  public void testMdcLogsForMultiBundleV1() {
    org.hl7.fhir.dstu3.model.Bundle bundle = new org.hl7.fhir.dstu3.model.Bundle();
    List<String> benes = Arrays.asList("567834", "567835", "567836");
    Set<Long> expectedBenes = new HashSet<>();
    benes.forEach(bene -> expectedBenes.add(Long.parseLong(bene)));

    for (String bene : benes) {
      TransformerUtils.addResourcesToBundle(
          bundle, Collections.singletonList(new org.hl7.fhir.dstu3.model.Patient().setId(bene)));
    }

    Set<Long> beneIds = LoggingUtils.findBeneIds(bundle);
    assertFalse(beneIds.isEmpty());
    assertEquals(3, beneIds.size());
    assertEquals(expectedBenes, beneIds);

    bundle = new org.hl7.fhir.dstu3.model.Bundle();
    for (String bene : benes) {
      TransformerUtils.addResourcesToBundle(
          bundle, Collections.singletonList(new org.hl7.fhir.dstu3.model.Patient().setId(bene)));
    }
    assertFalse(beneIds.isEmpty());
    assertEquals(3, beneIds.size());
    assertEquals(expectedBenes, beneIds);

    bundle = new org.hl7.fhir.dstu3.model.Bundle();
    for (String bene : benes) {
      TransformerUtils.addResourcesToBundle(
          bundle, Collections.singletonList(new org.hl7.fhir.dstu3.model.Patient().setId(bene)));
    }
    assertFalse(beneIds.isEmpty());
    assertEquals(3, beneIds.size());
    assertEquals(expectedBenes, beneIds);
  }

  /**
   * Test to see that {@link LoggingUtils#findBeneIds(org.hl7.fhir.r4.model.Bundle)} returns no
   * beneficiaryIds to log to BfdMdc in v2.
   */
  @Test
  public void testMdcLogsForEmptyBundleV2() {
    org.hl7.fhir.r4.model.Bundle bundle = new org.hl7.fhir.r4.model.Bundle();
    TransformerUtilsV2.addResourcesToBundle(
        bundle, Collections.singletonList(new org.hl7.fhir.r4.model.Patient()));
    assertTrue(LoggingUtils.findBeneIds(bundle).isEmpty());

    bundle = new org.hl7.fhir.r4.model.Bundle();
    TransformerUtilsV2.addResourcesToBundle(
        bundle, Collections.singletonList(new org.hl7.fhir.r4.model.Coverage()));
    assertTrue(LoggingUtils.findBeneIds(bundle).isEmpty());

    bundle = new org.hl7.fhir.r4.model.Bundle();
    TransformerUtilsV2.addResourcesToBundle(
        bundle, Collections.singletonList(new org.hl7.fhir.r4.model.ExplanationOfBenefit()));
    assertTrue(LoggingUtils.findBeneIds(bundle).isEmpty());
  }

  /**
   * Test to see that {@link LoggingUtils#findBeneIds(org.hl7.fhir.r4.model.Bundle)} returns a
   * single beneficiaryId to log to BfdMdc in v2.
   */
  @Test
  public void testMdcLogsForSingleBundleV2() {
    org.hl7.fhir.r4.model.Bundle bundle = new org.hl7.fhir.r4.model.Bundle();
    TransformerUtilsV2.addResourcesToBundle(
        bundle, Collections.singletonList(new org.hl7.fhir.r4.model.Patient().setId("567834")));

    Set<Long> beneIds = LoggingUtils.findBeneIds(bundle);
    assertFalse(beneIds.isEmpty());
    assertEquals(1, beneIds.size());
    assertEquals("567834", beneIds.iterator().next().toString());

    bundle = new org.hl7.fhir.r4.model.Bundle();
    TransformerUtilsV2.addResourcesToBundle(
        bundle,
        Collections.singletonList(
            new org.hl7.fhir.r4.model.Coverage()
                .setBeneficiary(new org.hl7.fhir.r4.model.Reference("Patient/567834"))));
    assertFalse(beneIds.isEmpty());
    assertEquals(1, beneIds.size());
    assertEquals("567834", beneIds.iterator().next().toString());

    bundle = new org.hl7.fhir.r4.model.Bundle();
    TransformerUtilsV2.addResourcesToBundle(
        bundle,
        Collections.singletonList(
            new org.hl7.fhir.r4.model.ExplanationOfBenefit()
                .setPatient(new org.hl7.fhir.r4.model.Reference("Patient/567834"))));
    assertFalse(beneIds.isEmpty());
    assertEquals(1, beneIds.size());
    assertEquals("567834", beneIds.iterator().next().toString());
  }

  /**
   * Test to see that {@link LoggingUtils#findBeneIds(org.hl7.fhir.r4.model.Bundle)} returns
   * multiple beneficiaryIds to log to BfdMdc in v2.
   */
  @Test
  public void testMdcLogsForMultiBundleV2() {
    org.hl7.fhir.r4.model.Bundle bundle = new org.hl7.fhir.r4.model.Bundle();
    List<String> benes = Arrays.asList("567834", "567835", "567836");
    Set<Long> expectedBenes = new HashSet<>();
    benes.forEach(bene -> expectedBenes.add(Long.parseLong(bene)));

    for (String bene : benes) {
      TransformerUtilsV2.addResourcesToBundle(
          bundle, Collections.singletonList(new org.hl7.fhir.r4.model.Patient().setId(bene)));
    }

    Set<Long> beneIds = LoggingUtils.findBeneIds(bundle);
    assertFalse(beneIds.isEmpty());
    assertEquals(3, beneIds.size());
    assertEquals(expectedBenes, beneIds);

    bundle = new org.hl7.fhir.r4.model.Bundle();
    for (String bene : benes) {
      TransformerUtilsV2.addResourcesToBundle(
          bundle, Collections.singletonList(new org.hl7.fhir.r4.model.Patient().setId(bene)));
    }
    assertFalse(beneIds.isEmpty());
    assertEquals(3, beneIds.size());
    assertEquals(expectedBenes, beneIds);

    bundle = new org.hl7.fhir.r4.model.Bundle();
    for (String bene : benes) {
      TransformerUtilsV2.addResourcesToBundle(
          bundle, Collections.singletonList(new org.hl7.fhir.r4.model.Patient().setId(bene)));
    }
    assertFalse(beneIds.isEmpty());
    assertEquals(3, beneIds.size());
    assertEquals(expectedBenes, beneIds);
  }
}
