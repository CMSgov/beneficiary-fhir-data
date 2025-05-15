package gov.cms.bfd.server.ng.coverage;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Coverage;
import org.springframework.stereotype.Component;

/**
 * Handler methods for the Coverage resource. This is called after the FHIR inputs from the resource
 * provider are converted into input types that are easier to work with.
 */
@Component
@RequiredArgsConstructor
public class CoverageHandler {

  private final BeneficiaryRepository beneficiaryRepository;

  /**
   * Reads a Coverage resource based on a composite ID ({part}-{bene_sk}).
   *
   * @param compositeId The ID string in the format {part}-{bene_sk}.
   * @return An {@link Optional} containing the {@link Coverage} resource if found, otherwise empty.
   * @throws InvalidRequestException if the compositeId format is invalid.
   */
  public Optional<Coverage> readCoverage(final String compositeId) {
    String[] idParts = parseCompositeId(compositeId);
    String partIdentifier = idParts[0];
    long beneSk = Long.parseLong(idParts[1]);

    // Fetch the beneficiary.
    Optional<Beneficiary> beneficiaryOpt =
        beneficiaryRepository.findById(beneSk, new DateTimeRange());

    if (beneficiaryOpt.isEmpty()) {
      return Optional.empty();
    }

    Beneficiary beneficiary = beneficiaryOpt.get();

    return beneficiary.toFhirCoverage(partIdentifier, compositeId);
  }

  /**
   * Parses the composite ID string "{part}-{bene_sk}" into its components. (This method remains the
   * same as in the previous detailed example)
   *
   * @param compositeId the compositeId.
   * @return parseCompositeIdn
   */
  private String[] parseCompositeId(String compositeId) {
    int lastHyphenIndex = compositeId.lastIndexOf('-');
    if (lastHyphenIndex == -1
        || lastHyphenIndex == 0
        || lastHyphenIndex == compositeId.length() - 1) {
      throw new InvalidRequestException(
          "Invalid Coverage ID format. Expected {part}-{bene_sk}, got: " + compositeId);
    }

    String part = compositeId.substring(0, lastHyphenIndex);
    String beneSkStr = compositeId.substring(lastHyphenIndex + 1);

    if (part.isEmpty() || beneSkStr.isEmpty()) {
      throw new InvalidRequestException(
          "Invalid Coverage ID format. Part or bene_sk is empty in: " + compositeId);
    }

    try {
      Long.parseLong(beneSkStr);
    } catch (NumberFormatException e) {
      throw new InvalidRequestException(
          "Invalid Coverage ID format. Bene SK part is not a number: " + beneSkStr, e);
    }
    return new String[] {part, beneSkStr};
  }
}
