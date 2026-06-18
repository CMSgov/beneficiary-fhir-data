package gov.cms.bfd.server.ng.audit;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import gov.cms.bfd.server.ng.util.FhirUtil;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/** Handler to handle AuditEvent Requests. */
@Component
@RequiredArgsConstructor
public class AuditEventHandler {

  private static final String LAST_INDEX = "lastIndex";

  private final AuditEventRepository auditEventRepository;

  /**
   * Look up AuditEvent by ID.
   *
   * @param id AuditEventId
   * @return AuditEvent
   */
  public AuditEvent getAuditEventById(AuditEventId id) {
    return auditEventRepository.findById(id);
  }

  /**
   * Look up multiple AuditEvents by their IDs and return them as a Bundle.
   *
   * @param ids list of AuditEventIds
   * @return Bundle of audit events
   */
  public Bundle getAuditEventsById(List<AuditEventId> ids) {
    return FhirUtil.bundleOrDefault(auditEventRepository.findByIds(ids), ZonedDateTime::now);
  }

  /**
   * Look up AuditEvents for a given beneficiary and return them as a Bundle.
   *
   * @param criteria search criteria
   * @param requestDetails HAPI FHIR request details
   * @return Bundle of audit events
   */
  public Bundle getAuditEventsByBeneficiary(
      AuditPatientSearchCriteria criteria, RequestDetails requestDetails) {
    var bundle =
        FhirUtil.bundleOrDefault(auditEventRepository.findByBeneId(criteria), ZonedDateTime::now);
    applyLinks(bundle, requestDetails, criteria.resolveLimit());
    return bundle;
  }

  private void applyLinks(Bundle bundle, RequestDetails requestDetails, Integer limit) {
    // check if a link is needed
    if (bundle.getEntry().size() > limit) {
      // Trim first so getLast() is the final item actually returned to the client.
      // DynamoDB's exclusiveStartKey starts *after* this key, so the next request
      // will correctly begin on the very next record.
      bundle.setEntry(bundle.getEntry().subList(0, limit));
      bundle
          .addLink()
          .setRelation(Constants.LINK_NEXT)
          .setUrl(
              buildLinkURL(requestDetails, bundle.getEntry().getLast().getResource().getIdPart()));
    }
    bundle.setTotal(bundle.getEntry().size());
  }

  private static String buildLinkURL(RequestDetails requestDetails, String lastIndex) {
    var uriBuilder = UriComponentsBuilder.fromUriString(requestDetails.getCompleteUrl());

    // Remove offset and startIndex parameters
    uriBuilder.replaceQueryParam(LAST_INDEX);

    // Add the new offset if it's not null
    Optional.ofNullable(lastIndex).ifPresent(id -> uriBuilder.queryParam(LAST_INDEX, id));

    return uriBuilder.build().toUriString();
  }
}
