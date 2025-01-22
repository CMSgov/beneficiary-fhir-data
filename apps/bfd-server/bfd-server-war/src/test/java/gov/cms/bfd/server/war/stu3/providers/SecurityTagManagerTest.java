package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import gov.cms.bfd.model.rif.samhsa.CarrierTag;
import gov.cms.bfd.model.rif.samhsa.HospiceTag;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** securityTagManager test. */
class SecurityTagManagerTest {

  /** The EntityManager. */
  @Mock private EntityManager entityManager;

  /** Query. */
  @Mock private Query query;

  /** securityTagManager. */
  @InjectMocks private SecurityTagManager securityTagManager;

  /** Set up the mocks and the validator being tested. */
  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this); // Initialize mocks
  }

  /** testGetClaimSecurityLevel_Inpatient. */
  @Test
  void testGetClaimSecurityLevelInpatient() {
    CodeableConcept type = new CodeableConcept();
    type.addCoding().setCode("INP");

    Set<String> mockTags = new HashSet<>();
    mockTags.add("R");
    when(entityManager.createQuery(anyString())).thenReturn(query);
    when(query.getResultList()).thenReturn(new java.util.ArrayList<>(mockTags));

    List<Coding> securityLevel =
        securityTagManager.getClaimSecurityLevel("12345", CarrierTag.class);
    assertEquals(
        "Restricted",
        securityLevel.getFirst().getDisplay(),
        "Security level should be 'Restricted' for Inpatient claim with 'R' tag");
  }

  /** testGetClaimSecurityLevel_Outpatient. */
  @Test
  void testGetClaimSecurityLevelOutpatient() {
    CodeableConcept type = new CodeableConcept();
    type.addCoding().setCode("OUT");

    Set<String> mockTags = new HashSet<>();
    mockTags.add("NormalTag");
    when(entityManager.createQuery(anyString())).thenReturn(query);
    when(query.getResultList()).thenReturn(new java.util.ArrayList<>(mockTags));

    List<Coding> securityLevel =
        securityTagManager.getClaimSecurityLevel("67890", HospiceTag.class);
    assertEquals(
        "Normal",
        securityLevel.getFirst().getDisplay(),
        "Security level should be 'Normal' for Outpatient claim with 'NormalTag'");
  }
}
