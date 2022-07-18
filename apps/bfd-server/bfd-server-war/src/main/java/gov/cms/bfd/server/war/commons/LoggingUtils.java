package gov.cms.bfd.server.war.commons;

import com.google.common.base.Strings;
import gov.cms.bfd.server.sharedutils.BfdMDC;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import gov.cms.bfd.server.war.r4.providers.TransformerUtilsV2;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ResourceType;
import org.jetbrains.annotations.NotNull;
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
    try {
      logBeneIdToMdc(Long.parseLong(beneId));
    } catch (NumberFormatException e) {
      LOGGER.warn("Could not parse long from bene_id: " + beneId);
    }
  }

  /**
   * TODO
   * @param bundle
   * @return
   */
  public static void logBenesToMdc(Bundle bundle) {
    Set<Long> beneIds = findBeneIds(bundle);
    logBeneIdToMdc(beneIds.stream().toArray(Long[]::new));
  }

  /**
   *
   * @param bundle
   * @return
   */
  static Set<Long> findBeneIds(Bundle bundle) {
    Set<Long> beneIds = new HashSet<Long>();
    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
      if (entry.getResource().getResourceType() == ResourceType.ExplanationOfBenefit) {
        ExplanationOfBenefit eob = ((ExplanationOfBenefit) entry.getResource());
        if (eob != null
                && eob.getPatient() != null
                && !Strings.isNullOrEmpty(eob.getPatient().getReference())) {
          String reference = eob.getPatient().getReference().replace("Patient/", "");
          if (!Strings.isNullOrEmpty(reference)) {
            addBeneIdsToSet(beneIds, reference);
          }
        }
      } else if (entry.getResource().getResourceType() == ResourceType.Patient) {
        Patient patient = ((Patient) entry.getResource());
        if (patient != null && !Strings.isNullOrEmpty(patient.getId())) {
          addBeneIdsToSet(beneIds, patient.getId());
        }

      } else if (entry.getResource().getResourceType() == ResourceType.Coverage) {
        Coverage coverage = ((Coverage) entry.getResource());
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
   * @param beneIds the collection of beneIds logged to MDC.
   * @param beneId beneficiaryId to add to beneId collection.
   */
  private static void addBeneIdsToSet(Set<Long> beneIds, String beneId) {
    try {
      beneIds.add(Long.parseLong(beneId));
    } catch (NumberFormatException e) {
      LOGGER.warn("Could not parse long from bene_id: " + beneId);
    }
  }
}
