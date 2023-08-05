package gov.cms.bfd.server.war.commons;

import com.google.common.base.Strings;
import gov.cms.bfd.server.sharedutils.BfdMDC;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A set of methods for various logging purposes i.e. MDC */
public class LoggingUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingUtils.class);

  /**
   * Log a list of beneficiary IDs to the BfdMDC under the 'bene_id' key.
   *
   * @param beneIds the {@link Long} of beneficiary IDs top log
   */
  public static void logBeneIdToMdc(Long... beneIds) {
    if (beneIds.length > 0) {
      String beneIdEntry =
          Arrays.stream(beneIds).map(String::valueOf).collect(Collectors.joining(", "));
      BfdMDC.put("bene_id", beneIdEntry);
    }
  }

  /**
   * Log a beneficiary ID to the BfdMDC under the 'bene_id' key if the ID supplied can be parsed as
   * a long.
   *
   * @param beneId the {@link String} of beneficiary IDs top log
   */
  public static void logBeneIdToMdc(String beneId) {
    if (!Strings.isNullOrEmpty(beneId)) {
      try {
        logBeneIdToMdc(Long.parseLong(beneId));
      } catch (NumberFormatException e) {
        LOGGER.warn("Could not parse long from bene_id: " + beneId);
      }
    }
  }

  /**
   * Log beneIDs from a bundle to BfdMDC in v1.
   *
   * @param bundle that is searched for valid bene_ids to log
   */
  public static void logBenesToMdc(org.hl7.fhir.dstu3.model.Bundle bundle) {
    Set<Long> beneIds = findBeneIds(bundle);
    logBeneIdToMdc(beneIds.stream().toArray(Long[]::new));
  }

  /**
   * Log beneIDs from a bundle to BfdMDC in v2.
   *
   * @param bundle that is searched through for valid bene_ids to log
   */
  public static void logBenesToMdc(org.hl7.fhir.r4.model.Bundle bundle) {
    Set<Long> beneIds = findBeneIds(bundle);
    logBeneIdToMdc(beneIds.stream().toArray(Long[]::new));
  }

  /**
   * Log resource count returned to client given size of single element or bundle to BfdMDC.
   *
   * @param count of resources returned to client to log
   */
  public static void logResourceCountToMdc(int count) {
    BfdMDC.put("resources_returned_count", String.format("%d", count));
  }

  /**
   * Helper function for aggregating bene_ids within a bundle in v1. If no bene_ids found, an empty
   * set is returned.
   *
   * @param bundle that is searched through for valid bene_ids
   * @return the set
   */
  static Set<Long> findBeneIds(org.hl7.fhir.dstu3.model.Bundle bundle) {
    Set<Long> beneIds = new HashSet<Long>();
    for (org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent entry : bundle.getEntry()) {
      if (entry.getResource().getResourceType()
          == org.hl7.fhir.dstu3.model.ResourceType.ExplanationOfBenefit) {
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit eob =
            ((org.hl7.fhir.dstu3.model.ExplanationOfBenefit) entry.getResource());
        if (eob != null
            && eob.getPatient() != null
            && !Strings.isNullOrEmpty(eob.getPatient().getReference())) {
          String reference = eob.getPatient().getReference().replace("Patient/", "");
          if (!Strings.isNullOrEmpty(reference)) {
            addBeneIdsToSet(beneIds, reference);
          }
        }
      } else if (entry.getResource().getResourceType()
          == org.hl7.fhir.dstu3.model.ResourceType.Patient) {
        org.hl7.fhir.dstu3.model.Patient patient =
            ((org.hl7.fhir.dstu3.model.Patient) entry.getResource());
        if (patient != null && !Strings.isNullOrEmpty(patient.getId())) {
          addBeneIdsToSet(beneIds, patient.getId());
        }

      } else if (entry.getResource().getResourceType()
          == org.hl7.fhir.dstu3.model.ResourceType.Coverage) {
        org.hl7.fhir.dstu3.model.Coverage coverage =
            ((org.hl7.fhir.dstu3.model.Coverage) entry.getResource());
        if (coverage != null
            && coverage.getBeneficiary() != null
            && !Strings.isNullOrEmpty(coverage.getBeneficiary().getReference())) {
          String reference = coverage.getBeneficiary().getReference().replace("Patient/", "");
          if (!Strings.isNullOrEmpty(reference)) {
            addBeneIdsToSet(beneIds, reference);
          }
        }
      }
    }
    return beneIds;
  }

  /**
   * Helper function for aggregating bene_ids within a bundle in v2. If no bene_ids found, an empty
   * set is returned.
   *
   * @param bundle that is searched through for valid bene_ids
   * @return the set of bene ids, or an empty set if none were found
   */
  static Set<Long> findBeneIds(org.hl7.fhir.r4.model.Bundle bundle) {
    Set<Long> beneIds = new HashSet<Long>();
    for (org.hl7.fhir.r4.model.Bundle.BundleEntryComponent entry : bundle.getEntry()) {
      if (entry.getResource().getResourceType()
          == org.hl7.fhir.r4.model.ResourceType.ExplanationOfBenefit) {
        org.hl7.fhir.r4.model.ExplanationOfBenefit eob =
            ((org.hl7.fhir.r4.model.ExplanationOfBenefit) entry.getResource());
        if (eob != null
            && eob.getPatient() != null
            && !Strings.isNullOrEmpty(eob.getPatient().getReference())) {
          String reference = eob.getPatient().getReference().replace("Patient/", "");
          if (!Strings.isNullOrEmpty(reference)) {
            addBeneIdsToSet(beneIds, reference);
          }
        }
      } else if (entry.getResource().getResourceType()
          == org.hl7.fhir.r4.model.ResourceType.Patient) {
        org.hl7.fhir.r4.model.Patient patient =
            ((org.hl7.fhir.r4.model.Patient) entry.getResource());
        if (patient != null && !Strings.isNullOrEmpty(patient.getId())) {
          addBeneIdsToSet(beneIds, patient.getId());
        }

      } else if (entry.getResource().getResourceType()
          == org.hl7.fhir.r4.model.ResourceType.Coverage) {
        org.hl7.fhir.r4.model.Coverage coverage =
            ((org.hl7.fhir.r4.model.Coverage) entry.getResource());
        if (coverage != null
            && coverage.getBeneficiary() != null
            && !Strings.isNullOrEmpty(coverage.getBeneficiary().getReference())) {
          String reference = coverage.getBeneficiary().getReference().replace("Patient/", "");
          if (!Strings.isNullOrEmpty(reference)) {
            addBeneIdsToSet(beneIds, reference);
          }
        }
      }
    }
    return beneIds;
  }

  /**
   * Parses Sting beneficiary ID to Long and adds to List to log to MDC.
   *
   * @param beneIds the collection of beneIds logged to MDC
   * @param beneId beneficiaryId to add to beneId collection
   */
  private static void addBeneIdsToSet(Set<Long> beneIds, String beneId) {
    try {
      beneIds.add(Long.parseLong(beneId));
    } catch (NumberFormatException e) {
      LOGGER.warn("Could not parse long from bene_id: " + beneId);
    }
  }
}
