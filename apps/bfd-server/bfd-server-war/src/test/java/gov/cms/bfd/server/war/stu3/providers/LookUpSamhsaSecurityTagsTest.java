package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import gov.cms.bfd.server.war.commons.LookUpSamhsaSecurityTags;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.HashSet;
import java.util.Set;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class LookUpSamhsaSecurityTagsTest {

  @Mock private EntityManager entityManager;

  @Mock private Query query;

  @InjectMocks private LookUpSamhsaSecurityTags lookUpSamhsaSecurityTags;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this); // Initialize mocks
  }

  @Test
  void testGetClaimSecurityLevel_Inpatient() {
    CodeableConcept type = new CodeableConcept();
    type.addCoding().setCode("INP");

    Set<String> mockTags = new HashSet<>();
    mockTags.add("R");
    when(entityManager.createQuery(anyString())).thenReturn(query);
    when(query.getResultList()).thenReturn(new java.util.ArrayList<>(mockTags));

    String securityLevel = lookUpSamhsaSecurityTags.getClaimSecurityLevel(type, "12345");
    assertEquals(
        "Restricted",
        securityLevel,
        "Security level should be 'Restricted' for Inpatient claim with 'R' tag");
  }

  @Test
  void testGetClaimSecurityLevel_Outpatient() {
    CodeableConcept type = new CodeableConcept();
    type.addCoding().setCode("OUT");

    Set<String> mockTags = new HashSet<>();
    mockTags.add("NormalTag");
    when(entityManager.createQuery(anyString())).thenReturn(query);
    when(query.getResultList()).thenReturn(new java.util.ArrayList<>(mockTags));

    String securityLevel = lookUpSamhsaSecurityTags.getClaimSecurityLevel(type, "67890");
    assertEquals(
        "Normal",
        securityLevel,
        "Security level should be 'Normal' for Outpatient claim with 'NormalTag'");
  }
}
